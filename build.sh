#!/bin/bash
# Build GoldenDash tanpa Gradle: aapt2 -> javac -> d8 -> zipalign -> apksigner.
# Pakai: ./build.sh            build saja → build/goldendash.apk
#        ./build.sh install    build + pasang ke headunit lewat ADB, beri izin, jadikan launcher default
#        DEVICE=192.168.1.17:5555 ./build.sh install   bila ada lebih dari satu perangkat ADB
set -euo pipefail
cd "$(dirname "$0")"

PKG=com.goldendash.launcher
VERSION_NAME=${VERSION_NAME:-0.2.1}
VERSION_CODE=${VERSION_CODE:-3}
MODE=${1:-}

SDK=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}
[ -d "$SDK" ] || { echo "Android SDK tidak ditemukan di $SDK (atur ANDROID_HOME)." >&2; exit 1; }

# build-tools dan platform terbaru yang terpasang (bisa ditimpa lewat BT=... dan JAR=...)
if [ -z "${BT:-}" ]; then
    BT=$(ls -d "$SDK"/build-tools/*/ 2>/dev/null | tail -1); BT=${BT%/}
fi
if [ -z "${JAR:-}" ]; then
    best=0
    for f in "$SDK"/platforms/android-*/android.jar; do
        [ -f "$f" ] || continue
        n=$(basename "$(dirname "$f")"); n=${n#android-}
        case $n in (*[!0-9]*) continue ;; esac
        if [ "$n" -gt "$best" ]; then best=$n; JAR=$f; fi
    done
fi
[ -x "$BT/aapt2" ] || { echo "build-tools tidak ditemukan di $SDK/build-tools." >&2; exit 1; }
[ -f "${JAR:-}" ] || { echo "platform Android tidak ditemukan di $SDK/platforms." >&2; exit 1; }

rm -rf build
mkdir -p build/classes build/dex build/gen

"$BT/aapt2" compile --dir res -o build/res.zip
"$BT/aapt2" link -o build/base.apk -I "$JAR" --manifest AndroidManifest.xml \
    --min-sdk-version 24 --target-sdk-version 29 --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" \
    --java build/gen build/res.zip

javac --release 8 -encoding UTF-8 -Xlint:-options -classpath "$JAR" \
    -d build/classes $(find src build/gen -name '*.java')
"$BT/d8" --release --min-api 24 --lib "$JAR" --output build/dex $(find build/classes -name '*.class')

cp build/base.apk build/unaligned.apk
(cd build/dex && zip -q ../unaligned.apk classes.dex)
"$BT/zipalign" -f -p 4 build/unaligned.apk build/aligned.apk

# Kunci tanda tangan dibuat sekali. Cadangkan goldendash.keystore dan .keystore-pass: tanpa keduanya APK baru
# tidak bisa memperbarui aplikasi yang sudah terpasang.
if [ -n "${GOLDENDASH_KS_PASS:-}" ]; then
    export KS_PASS=$GOLDENDASH_KS_PASS
elif [ -f .keystore-pass ]; then
    export KS_PASS=$(cat .keystore-pass)
else
    [ -f goldendash.keystore ] && { echo "goldendash.keystore ada tapi .keystore-pass hilang." >&2; exit 1; }
    export KS_PASS=$(openssl rand -base64 48 | tr -d '/+=\n' | cut -c1-32)
    (umask 077; printf '%s' "$KS_PASS" > .keystore-pass)
fi
if [ ! -f goldendash.keystore ]; then
    keytool -genkeypair -keystore goldendash.keystore -storepass:env KS_PASS -keypass:env KS_PASS \
        -alias goldendash -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=GoldenDash" >/dev/null 2>&1
    echo "Kunci baru dibuat: goldendash.keystore + .keystore-pass. Cadangkan keduanya." >&2
fi
"$BT/apksigner" sign --ks goldendash.keystore --ks-pass env:KS_PASS --key-pass env:KS_PASS \
    --out build/goldendash.apk build/aligned.apk
ls -lh build/goldendash.apk

if [ "$MODE" = "install" ]; then
    ADB=(adb); [ -n "${DEVICE:-}" ] && ADB=(adb -s "$DEVICE")
    "${ADB[@]}" install -r build/goldendash.apk
    run() { "${ADB[@]}" shell "$@" || true; }
    run pm grant $PKG android.permission.ACCESS_FINE_LOCATION
    run pm grant $PKG android.permission.ACCESS_COARSE_LOCATION
    run appops set $PKG WRITE_SETTINGS allow
    run cmd notification allow_listener $PKG/$PKG.NotifListener
    # AOT sekarang: dexopt latar menunggu perangkat diam + dicas, headunit hampir tidak pernah begitu
    run cmd package compile -m speed -f $PKG
    run cmd package set-home-activity $PKG/.HomeActivity
    # Firmware NWD memilih launcher dari properti ini (kembalikan: setprop persist.nwd.launcher.default com.android.launcher)
    run setprop persist.nwd.launcher.default $PKG
    run am start -a android.intent.action.MAIN -c android.intent.category.HOME
fi
