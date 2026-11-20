#!/usr/bin/env sh
# Builds a self-contained app image with jpackage (Linux/macOS).
# Output: dist/NNPPSS/ (Linux) or dist/NNPPSS.app (macOS)
set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

echo "Building application JAR and runtime dependencies..."
mvn -B -DskipTests package

APP_DIR="$ROOT_DIR/target/app"
JAR="$(find "$ROOT_DIR/target" -maxdepth 1 -name 'NNPPSS-*.jar' \
    ! -name '*sources*' ! -name '*javadoc*' | head -n 1)"
if [ -z "$JAR" ]; then
    echo "NNPPSS JAR was not produced by Maven" >&2
    exit 1
fi

cp "$JAR" "$APP_DIR/NNPPSS.jar"

# pkg2zip is looked up on PATH on Linux/macOS; bundle it too when present.
if [ -f "$ROOT_DIR/lib/pkg2zip" ]; then
    cp "$ROOT_DIR/lib/pkg2zip" "$APP_DIR/lib/pkg2zip"
    chmod +x "$APP_DIR/lib/pkg2zip"
    echo "Bundled pkg2zip"
fi

rm -rf "$ROOT_DIR/dist/NNPPSS" "$ROOT_DIR/dist/NNPPSS.app"

PACKAGE_TYPE="${JPACKAGE_TYPE:-app-image}"
echo "Running jpackage (type: $PACKAGE_TYPE)..."
jpackage --type "$PACKAGE_TYPE" \
    --name NNPPSS \
    --input "$APP_DIR" \
    --main-jar NNPPSS.jar \
    --main-class com.squarepeace.nnppss.Launcher \
    --dest "$ROOT_DIR/dist" \
    --java-options "--enable-native-access=ALL-UNNAMED"

echo "App image created under $ROOT_DIR/dist"
