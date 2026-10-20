#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIST_DIR="$PROJECT_DIR/dist"
mkdir -p "$DIST_DIR"

VERSION_CODE="$(git -C "$PROJECT_DIR" rev-list --count HEAD 2>/dev/null || echo 2)"
VERSION_NAME="1.1.${VERSION_CODE}"

echo "==> Compilando Homelab TV Launcher v${VERSION_NAME} (code ${VERSION_CODE}) con límite estricto de RAM..."

docker run --rm \
  --memory="2560m" \
  --memory-swap="3584m" \
  --cpus="3.0" \
  -e GRADLE_USER_HOME=/gradle-cache \
  -v homelab-tv-gradle-cache:/gradle-cache \
  -v "$PROJECT_DIR":/workspace \
  -w /workspace \
  ghcr.io/cirruslabs/android-sdk:34 \
  bash -c '
    set -euo pipefail
    if [[ ! -x /gradle-cache/gradle-8.7/bin/gradle ]]; then
      echo "==> Descargando Gradle 8.7 en volumen de caché..."
      curl -sSL https://services.gradle.org/distributions/gradle-8.7-bin.zip -o /tmp/gradle.zip
      unzip -q /tmp/gradle.zip -d /gradle-cache
      rm -f /tmp/gradle.zip
    fi
    /gradle-cache/gradle-8.7/bin/gradle assembleRelease \
      --no-daemon \
      --max-workers=2 \
      -PappVersionCode='"$VERSION_CODE"' \
      -PappVersionName='"$VERSION_NAME"' \
      -Dkotlin.compiler.execution.strategy=in-process \
      -Dorg.gradle.jvmargs="-Xmx1400m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8"
    chown -R '"$(id -u):$(id -g)"' /workspace/app/build /workspace/.gradle 2>/dev/null || true
  '

APK_PATH="$PROJECT_DIR/app/build/outputs/apk/release/app-release.apk"
if [[ -f "$APK_PATH" ]]; then
  cp "$APK_PATH" "$DIST_DIR/homelab-tv-launcher.apk"
  SHA256="$(sha256sum "$DIST_DIR/homelab-tv-launcher.apk" | awk '{print $1}')"
  cat > "$DIST_DIR/version.json" <<EOF
{
  "version_code": ${VERSION_CODE},
  "version_name": "${VERSION_NAME}",
  "sha256": "${SHA256}",
  "built_at": $(date +%s)
}
EOF
  DEPLOY_DIR="$(grep -E '^deploy\.dir=' "$PROJECT_DIR/local.properties" 2>/dev/null | cut -d'=' -f2- || true)"
  if [[ -n "$DEPLOY_DIR" ]]; then
    mkdir -p "$DEPLOY_DIR"
    cp "$DIST_DIR/homelab-tv-launcher.apk" "$DEPLOY_DIR/homelab-tv-launcher.apk"
    cp "$DIST_DIR/version.json" "$DEPLOY_DIR/version.json"
    echo "==> APK publicada en el endpoint OTA local (${DEPLOY_DIR})"
  fi
  echo "==> APK generada con éxito en: $DIST_DIR/homelab-tv-launcher.apk ($(du -h "$DIST_DIR/homelab-tv-launcher.apk" | cut -f1), v${VERSION_NAME})"
else
  echo "ERROR: No se encontró la APK compilada en $APK_PATH" >&2
  exit 1
fi
