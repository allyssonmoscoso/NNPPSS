#!/usr/bin/env sh
# run-dev.sh - Compila rápido y ejecuta la app sin empaquetar.
# - Descarga jsoup si falta (según pom.xml: 1.15.3)
# - Compila fuentes a out/classes
# - Copia recursos desde src/main/resources
# - Ejecuta con classpath apuntando a out/classes y lib/*
#
# Uso:
#   ./run-dev.sh           -> compila todo y ejecuta
#   ./run-dev.sh compile   -> compila todo y sale
#   ./run-dev.sh run       -> ejecuta (asume compilado)
#   ./run-dev.sh clean     -> limpia out/ y .build_tmp/
#   ./run-dev.sh file <ruta_a_java> -> compila sólo ese archivo (rápido)
#
set -eu

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$ROOT_DIR/src/main/java"
RES_DIR="$ROOT_DIR/src/main/resources"
OUT_DIR="$ROOT_DIR/out"
CLASSES_DIR="$OUT_DIR/classes"
LIB_DIR="$ROOT_DIR/lib"
TMP_DIR="$ROOT_DIR/.build_tmp"

MAIN_CLASS="com.squarepeace.nnppss.NNPPSS"

# dependencias (según pom.xml)
JSOUP_VERSION="1.15.3"
JSOUP_JAR="jsoup-${JSOUP_VERSION}.jar"
JSOUP_URL="https://repo1.maven.org/maven2/org/jsoup/jsoup/${JSOUP_VERSION}/${JSOUP_JAR}"

GSON_VERSION="2.10.1"
GSON_JAR="gson-${GSON_VERSION}.jar"
GSON_URL="https://repo1.maven.org/maven2/com/google/code/gson/gson/${GSON_VERSION}/${GSON_JAR}"

SLF4J_VERSION="2.0.9"
SLF4J_JAR="slf4j-api-${SLF4J_VERSION}.jar"
SLF4J_URL="https://repo1.maven.org/maven2/org/slf4j/slf4j-api/${SLF4J_VERSION}/${SLF4J_JAR}"

LOGBACK_VERSION="1.4.11"
LOGBACK_CLASSIC_JAR="logback-classic-${LOGBACK_VERSION}.jar"
LOGBACK_CLASSIC_URL="https://repo1.maven.org/maven2/ch/qos/logback/logback-classic/${LOGBACK_VERSION}/${LOGBACK_CLASSIC_JAR}"

LOGBACK_CORE_JAR="logback-core-${LOGBACK_VERSION}.jar"
LOGBACK_CORE_URL="https://repo1.maven.org/maven2/ch/qos/logback/logback-core/${LOGBACK_VERSION}/${LOGBACK_CORE_JAR}"

JAVAC_FLAGS="--release 17"

die() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}
info() { printf '%s\n' "$*"; }

ensure_dirs() {
  mkdir -p "$CLASSES_DIR" "$LIB_DIR" "$TMP_DIR"
}

_download_dep() {
  jarfile="$1"
  url="$2"
  if [ -f "$LIB_DIR/$jarfile" ]; then
    info "Dependencia encontrada: lib/$jarfile"
    return 0
  fi
  info "Dependencia lib/$jarfile no encontrada. Intentando descargar..."
  if command -v curl >/dev/null 2>&1; then
    if curl -fL -o "$LIB_DIR/$jarfile" "$url"; then
      info "Descargado $jarfile (curl)."
      return 0
    else
      info "curl falló al descargar $jarfile"
    fi
  fi
  if command -v wget >/dev/null 2>&1; then
    if wget -O "$LIB_DIR/$jarfile" "$url"; then
      info "Descargado $jarfile (wget)."
      return 0
    else
      info "wget falló al descargar $jarfile"
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

compile_all() {
  info "Compilando todas las fuentes..."
  find "$SRC_DIR" -name '*.java' | grep -q . || die "No se encontraron fuentes Java en $SRC_DIR"
  CP="$(collect_libs_cp)"
  if [ -z "$CP" ]; then
    javac $JAVAC_FLAGS -d "$CLASSES_DIR" $(find "$SRC_DIR" -name '*.java')
  else
    javac $JAVAC_FLAGS -cp "$CP" -d "$CLASSES_DIR" $(find "$SRC_DIR" -name '*.java')
  fi
  info "Compilación completa."
}

compile_file() {
  file="$1"
  [ -f "$file" ] || die "Archivo no encontrado: $file"
  info "Compilando $file..."
  CP="$(collect_libs_cp)"
  if [ -z "$CP" ]; then
    javac $JAVAC_FLAGS -d "$CLASSES_DIR" "$file"
  else
    javac $JAVAC_FLAGS -cp "$CP:$CLASSES_DIR" -d "$CLASSES_DIR" "$file"
  fi
  info "Compilado $file"
}

copy_resources() {
  if [ -d "$RES_DIR" ]; then
    info "Copiando recursos a $CLASSES_DIR..."
    # preservar estructura
    cp -R "$RES_DIR/." "$CLASSES_DIR/" 2>/dev/null || true
  fi
}

run_app() {
  # Determinar separador de classpath por plataforma
  SEP=":"
  case "$(uname -s 2>/dev/null || echo unknown)" in
    CYGWIN*|MINGW*|MSYS*) SEP=";";;
  esac

  # construir classpath: out/classes + jars en lib
  CLASSES_CP="$CLASSES_DIR"
  LIB_CP=""
  for j in "$LIB_DIR"/*.jar; do [ -e "$j" ] || continue; LIB_CP="$LIB_CP$j$SEP"; done
  # quitar sep final si existe
  LIB_CP="$(printf '%s' "$LIB_CP" | sed 's/[:;]$//')"

  if [ -n "$LIB_CP" ]; then
    CP="$CLASSES_CP$SEP$LIB_CP"
  else
    CP="$CLASSES_CP"
  fi

  info "Ejecutando: java -cp \"$CP\" $MAIN_CLASS"
  java -cp "$CP" "$MAIN_CLASS"
}

clean() {
  info "Limpiando out/ y .build_tmp/ ..."
  rm -rf "$OUT_DIR" "$TMP_DIR"
  info "Limpieza completa."
}

# Entradas
case "${1:-}" in
  clean)
    clean
    exit 0
    ;;
  compile)
    ensure_dirs
    _download_dep "$JSOUP_JAR" "$JSOUP_URL"
    _download_dep "$GSON_JAR" "$GSON_URL"
    _download_dep "$SLF4J_JAR" "$SLF4J_URL"
    _download_dep "$LOGBACK_CLASSIC_JAR" "$LOGBACK_CLASSIC_URL"
    _download_dep "$LOGBACK_CORE_JAR" "$LOGBACK_CORE_URL"
    compile_all
    copy_resources
    info "Listo. Ejecuta './run-dev.sh run' o './run-dev.sh' para ejecutar."
    exit 0
    ;;
  run)
    ensure_dirs
    _download_dep "$JSOUP_JAR" "$JSOUP_URL"
    _download_dep "$GSON_JAR" "$GSON_URL"
    _download_dep "$SLF4J_JAR" "$SLF4J_URL"
    _download_dep "$LOGBACK_CLASSIC_JAR" "$LOGBACK_CLASSIC_URL"
    _download_dep "$LOGBACK_CORE_JAR" "$LOGBACK_CORE_URL"
    # si no hay clases compiladas, compilar
    if [ ! -d "$CLASSES_DIR" ] || [ -z "$(ls -A "$CLASSES_DIR" 2>/dev/null || true)" ]; then
      compile_all
      copy_resources
    fi
    run_app
    exit 0
    ;;
  file)
    if [ -z "${2:-}" ]; then
      die "Uso: $0 file <ruta_a_java>"
    fi
    ensure_dirs
    _download_dep "$JSOUP_JAR" "$JSOUP_URL"
    _download_dep "$GSON_JAR" "$GSON_URL"
    _download_dep "$SLF4J_JAR" "$SLF4J_URL"
    _download_dep "$LOGBACK_CLASSIC_JAR" "$LOGBACK_CLASSIC_URL"
    _download_dep "$LOGBACK_CORE_JAR" "$LOGBACK_CORE_URL"
    compile_file "$2"
    copy_resources
    info "Archivo compilado. Ejecuta './run-dev.sh' para correr la app completa."
    exit 0
    ;;
  "" )
    # default: compile + run
    ensure_dirs
    _download_dep "$JSOUP_JAR" "$JSOUP_URL"
    _download_dep "$GSON_JAR" "$GSON_URL"
    _download_dep "$SLF4J_JAR" "$SLF4J_URL"
    _download_dep "$LOGBACK_CLASSIC_JAR" "$LOGBACK_CLASSIC_URL"
    _download_dep "$LOGBACK_CORE_JAR" "$LOGBACK_CORE_URL"
    compile_all
    copy_resources
    run_app
    ;;
  * )
    die "Opción desconocida: $1. Usa: (clean|compile|run|file <java>)"
    ;;
esac
