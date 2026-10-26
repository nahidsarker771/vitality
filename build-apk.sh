#!/usr/bin/env bash
# Vitality — versioned APK builder (native, no Gradle).
#
# Usage:
#   ./build-apk.sh          -> build VERSION, write ../apk/Vitality-v<ver>.apk, bump VERSION
#   ./build-apk.sh 1.0      -> build explicitly as 1.0
set -euo pipefail
cd "$(dirname "$0")"
# shellcheck disable=SC1091
source ./env.sh

APP_NAME="Vitality"
OUT_DIR="$(cd .. && pwd)/apk"
KEYSTORE="${VITALITY_KEYSTORE:-$PWD/vitality.jks}"
KS_ALIAS="${VITALITY_KS_ALIAS:-vitality}"
KS_PASS="${VITALITY_KS_PASS:-}"
if [ -z "$KS_PASS" ] && [ -f "$PWD/.ks-pass" ]; then
  KS_PASS="$(tr -d '[:space:]' < "$PWD/.ks-pass")"
fi
BT="$ANDROID_HOME/build-tools/34.0.0"
ANDROID_JAR="$(ls "$ANDROID_HOME"/platforms/android-*/android.jar 2>/dev/null | sort -V | tail -1)"

[ -x "$BT/aapt2" ] || { echo "ERROR: aapt2 not found in $BT"; exit 1; }
[ -f "$ANDROID_JAR" ] || { echo "ERROR: android.jar not found"; exit 1; }
[ -f "$KEYSTORE" ] || { echo "ERROR: $KEYSTORE missing"; exit 1; }
[ -f AndroidManifest.xml ] || { echo "ERROR: run from the vitality working dir"; exit 1; }

VER_FILE="$PWD/VERSION"
[ -f "$VER_FILE" ] || echo "1.0" > "$VER_FILE"
CUR="$(tr -d '[:space:]' < "$VER_FILE")"
VER="${1:-$CUR}"
IFS='.' read -ra PARTS <<< "$VER"
MAJ="${PARTS[0]:-1}"; MIN="${PARTS[1]:-0}"; PAT="${PARTS[2]:-0}"
VC=$(( MAJ * 10000 + MIN * 100 + PAT ))
LAST=$(( ${#PARTS[@]} - 1 )); PARTS[$LAST]=$(( PARTS[$LAST] + 1 ))
NEXT="$(IFS=.; echo "${PARTS[*]}")"

echo "==> Building $APP_NAME v$VER (versionCode $VC)"
mkdir -p "$OUT_DIR"

WORK="$PWD/.work"
rm -rf "$WORK"
mkdir -p "$WORK/flat" "$WORK/gen" "$WORK/classes" "$WORK/dex"

echo "==> aapt2 compile"
"$BT/aapt2" compile --dir res -o "$WORK/res.zip"
unzip -q "$WORK/res.zip" -d "$WORK/flat"

echo "==> aapt2 link"
"$BT/aapt2" link -o "$WORK/unsigned.apk" \
  -I "$ANDROID_JAR" \
  --manifest AndroidManifest.xml \
  -R "$WORK"/flat/*.flat \
  --java "$WORK/gen" \
  --auto-add-overlay \
  --min-sdk-version 29 \
  --target-sdk-version 34 \
  --version-code "$VC" \
  --version-name "$VER"

echo "==> javac"
find src "$WORK/gen" -name '*.java' -print0 | \
  xargs -0 javac -source 8 -target 8 -Xlint:-options \
  -classpath "$ANDROID_JAR" -d "$WORK/classes"

echo "==> d8"
find "$WORK/classes" -name '*.class' -print0 | \
  xargs -0 "$BT/d8" --lib "$ANDROID_JAR" --min-api 29 --output "$WORK/dex"

echo "==> package / align / sign"
(cd "$WORK/dex" && zip -q "$WORK/unsigned.apk" classes.dex)
"$BT/zipalign" -f 4 "$WORK/unsigned.apk" "$WORK/aligned.apk"
"$BT/apksigner" sign --ks "$KEYSTORE" --ks-pass "pass:$KS_PASS" \
  --key-pass "pass:$KS_PASS" --ks-key-alias "$KS_ALIAS" \
  --out "$OUT_DIR/$APP_NAME-v$VER.apk" "$WORK/aligned.apk"
"$BT/apksigner" verify "$OUT_DIR/$APP_NAME-v$VER.apk" >/dev/null

echo "$NEXT" > "$VER_FILE"
rm -rf "$WORK"
echo ""
echo "==> Done!"
echo "    APK : $OUT_DIR/$APP_NAME-v$VER.apk"
echo "    Next build will be v$NEXT"
