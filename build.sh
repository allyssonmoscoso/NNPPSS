#!/usr/bin/env sh
# build.sh - Compila y empaqueta el proyecto NNPPSS sin Maven
# Genera:
#   - JAR normal con manifest en dist/NNPPSS.jar (manifest con Class-Path apuntando a lib/)
#   - Fat JAR (dependencias embebidas) en dist/NNPPSS-fat.jar
#
# Requisitos:
# - JDK 17+ (javac, jar, java)
# - curl o wget (para descargar dependencias si faltan)
#
# Uso:
#   ./build.sh        -> compila y genera los jars en dist/
#   ./build.sh clean  -> elimina artefactos de build
#
# Nota: El script intentará descargar jsoup v1.15.3 (según pom.xml) a lib/.
# Si no se puede descargar, coloca manualmente el jar requerido en lib/.

set -eu
# intentar habilitar pipefail cuando la shell lo soporte
if (set -o pipefail) 2>/dev/null; then set -o pipefail; fi

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$ROOT_DIR/src/main/java"
RES_DIR="$ROOT_DIR/src/main/resources"
OUT_DIR="$ROOT_DIR/out"
CLASSES_DIR="$OUT_DIR/classes"
LIB_DIR="$ROOT_DIR/lib"
DIST_DIR="$ROOT_DIR/dist"
TMP_DIR="$ROOT_DIR/.build_tmp"
MAIN_CLASS="com.squarepeace.nnppss.NNPPSS"

# Dependencias (tomadas del pom.xml)
JSOUP_VERSION="1.15.3"
JSOUP_JAR="jsoup-${JSOUP_VERSION}.jar"
JSOUP_URL="https://repo1.maven.org/maven2/org/jsoup/jsoup/${JSOUP_VERSION}/${JSOUP_JAR}"

GSON_VERSION="2.10.1"
GSON_JAR="gson-${GSON_VERSION}.jar"
GSON_URL="https://repo1.maven.org/maven2/com/google/code/gson/gson/${GSON_VERSION}/${GSON_JAR}"

JAVAC_FLAGS="--release 17"
PLAIN_JAR_NAME="NNPPSS.jar"
FAT_JAR_NAME="NNPPSS-fat.jar"

die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
info() { printf '%s\n' "$*"; }

clean() {
  info "Limpiando artefactos..."
  rm -rf "$OUT_DIR" "$DIST_DIR" "$TMP_DIR"
  info "Limpieza finalizada."
}

ensure_tools() {
  command -v java >/dev/null 2>&1 || die "java no encontrado en PATH"
  command -v javac >/dev/null 2>&1 || die "javac no encontrado en PATH"
  command -v jar >/dev/null 2>&1 || die "jar no encontrado en PATH"
}

ensure_dirs() {
  mkdir -p "$CLASSES_DIR" "$LIB_DIR" "$DIST_DIR" "$TMP_DIR"
}

download_dep_if_missing() {
  jarfile="$1"; url="$2"
  if [ -f "$LIB_DIR/$jarfile" ]; then
    info "Dependencia ya presente: lib/$jarfile"
    return 0
  fi
  info "Descargando dependencia $jarfile..."
  if command -v curl >/dev/null 2>&1; then
    if curl -fL -o "$LIB_DIR/$jarfile" "$url"; then
      info "Descargado $jarfile con curl."
      return 0
    else
      info "curl falló al descargar $jarfile."
    fi
  fi
  if command -v wget >/dev/null 2>&1; then
    if wget -O "$LIB_DIR/$jarfile" "$url"; then
      info "Descargado $jarfile con wget."
      return 0
    else
      info "wget falló al descargar $jarfile."
    fi
  fi
  die "No se pudo descargar $jarfile automáticamente. Colócalo manualmente en $LIB_DIR"
}

collect_libs_cp() {
  CP=""
  for f in "$LIB_DIR"/*.jar; do
    [ -e "$f" ] || continue
    if [ -z "$CP" ]; then CP="$f"; else CP="$CP:$f"; fi
  done
  printf '%s' "$CP"
}

compile_sources() {
  info "Compilando fuentes Java..."
  set +e
  SRC_COUNT=$(find "$SRC_DIR" -name '*.java' | wc -l 2>/dev/null)
  set -e
  SRC_COUNT=$(printf '%s' "$SRC_COUNT" | tr -d ' ')
  if [ -z "$SRC_COUNT" ] || [ "$SRC_COUNT" -lt 1 ]; then
    die "No se encontraron fuentes .java en $SRC_DIR"
  fi

  CP="$(collect_libs_cp)"
  # Usar find para expandir archivos en caso de muchos ficheros
  if [ -z "$CP" ]; then
    javac $JAVAC_FLAGS -d "$CLASSES_DIR" $(find "$SRC_DIR" -name '*.java')
  else
    javac $JAVAC_FLAGS -cp "$CP" -d "$CLASSES_DIR" $(find "$SRC_DIR" -name '*.java')
  fi
  info "Compilación finalizada."
}

copy_resources() {
  if [ -d "$RES_DIR" ]; then
    info "Copiando recursos a clases..."
    cp -R "$RES_DIR/." "$CLASSES_DIR/" 2>/dev/null || true
  fi
}

create_plain_jar() {
  info "Creando JAR normal (manifest con Class-Path): $DIST_DIR/$PLAIN_JAR_NAME"
  MANIFEST="$OUT_DIR/manifest.mf"
  CP_MANIFEST=""
  for f in "$LIB_DIR"/*.jar; do
    [ -e "$f" ] || continue
    bn="$(basename "$f")"
    CP_MANIFEST="$CP_MANIFEST$bn "
  done
  {
    echo "Manifest-Version: 1.0"
    echo "Main-Class: $MAIN_CLASS"
    [ -n "$CP_MANIFEST" ] && echo "Class-Path: $CP_MANIFEST"
    echo ""
  } > "$MANIFEST"
  (cd "$CLASSES_DIR" && jar cfm "$DIST_DIR/$PLAIN_JAR_NAME" "$MANIFEST" .)
  info "JAR normal creado: $DIST_DIR/$PLAIN_JAR_NAME"
}

create_fat_jar() {
  info "Creando fat-jar (dependencias embebidas): $DIST_DIR/$FAT_JAR_NAME"
  rm -rf "$TMP_DIR"/*
  mkdir -p "$TMP_DIR"
  # copiar clases al tmp
  if [ -d "$CLASSES_DIR" ]; then
    (cd "$CLASSES_DIR" && tar cf - .) | (cd "$TMP_DIR" && tar xf -)
  fi
  # extraer cada dependencia en tmp
  for dep in "$LIB_DIR"/*.jar; do
    [ -e "$dep" ] || continue
    info "Desempaquetando $dep"
    (cd "$TMP_DIR" && jar xf "$dep")
  done
  # eliminar firmas y manifiestos que puedan causar conflictos
  if [ -d "$TMP_DIR/META-INF" ]; then
    rm -f "$TMP_DIR/META-INF"/*.SF 2>/dev/null || true
    rm -f "$TMP_DIR/META-INF"/*.RSA 2>/dev/null || true
    rm -f "$TMP_DIR/META-INF"/*.DSA 2>/dev/null || true
    rm -f "$TMP_DIR/META-INF/MANIFEST.MF" 2>/dev/null || true
  fi
  # crear manifest propio
  MANIFEST="$TMP_DIR/manifest.mf"
  {
    echo "Manifest-Version: 1.0"
    echo "Main-Class: $MAIN_CLASS"
    echo ""
  } > "$MANIFEST"
  (cd "$TMP_DIR" && jar cfm "$DIST_DIR/$FAT_JAR_NAME" "$MANIFEST" .)
  info "Fat-jar creado: $DIST_DIR/$FAT_JAR_NAME"
}

create_run_sh() {
  RUN_SH="$ROOT_DIR/run.sh"
  cat > "$RUN_SH" <<'SH'
#!/usr/bin/env sh
DIR="$(cd "$(dirname "$0")" && pwd)"
java -jar "$DIR/dist/NNPPSS-fat.jar" "$@"
SH
  chmod +x "$RUN_SH"
  info "Script de ejecución creado: $RUN_SH"
}

# Entrada principal
if [ "${1:-}" = "clean" ]; then
  clean
  exit 0
fi

ensure_tools
ensure_dirs

# Asegurar dependencias externas necesarias
download_dep_if_missing "$JSOUP_JAR" "$JSOUP_URL"
download_dep_if_missing "$GSON_JAR" "$GSON_URL"

# Compilar y empaquetar
compile_sources
copy_resources
create_plain_jar
create_fat_jar
create_run_sh

info "Build finalizado."
info "JAR normal: $DIST_DIR/$PLAIN_JAR_NAME"
info "Fat JAR:    $DIST_DIR/$FAT_JAR_NAME"
info "Ejecutar: ./run.sh  (o java -jar $DIST_DIR/$FAT_JAR_NAME)"

exit 0
