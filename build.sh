#!/bin/sh
# Build without Gradle. Needs JDK 17+ and Android SDK (platforms;android-34, build-tools;36.0.0).
# d8 from build-tools 34 crashes on this code, so 36 is used.
# Signing: set SM_KS_PASS (keystore password); SM_KS = keystore path (default key.jks, created if missing).
set -e
cd "$(dirname "$0")"
: "${SM_KS_PASS:?Set SM_KS_PASS (keystore password)}"
KS=${SM_KS:-key.jks}
SDK=${ANDROID_HOME:-$HOME/Android/Sdk}
BT=$SDK/build-tools/36.0.0
JAR=$SDK/platforms/android-34/android.jar
rm -rf build && mkdir -p build/res build/gen build/classes build/dex

$BT/aapt2 compile --dir res -o build/res/res.zip
$BT/aapt2 link -I $JAR --manifest AndroidManifest.xml -o build/base.apk --java build/gen \
  build/res/res.zip --min-sdk-version 26 --target-sdk-version 34
javac --release 11 -nowarn -encoding UTF-8 -classpath $JAR -d build/classes $(find src build/gen -name '*.java')
(cd build/classes && jar cf ../classes.jar .)
$BT/d8 --min-api 26 --lib $JAR --output build/dex build/classes.jar

cp build/base.apk build/unsigned.apk
(cd build/dex && zip -q ../unsigned.apk classes.dex)
$BT/zipalign -f 4 build/unsigned.apk build/aligned.apk

[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -alias k -keyalg RSA -keysize 2048 \
  -validity 10000 -storepass "$SM_KS_PASS" -keypass "$SM_KS_PASS" -dname "CN=S-Mykhailo" >/dev/null 2>&1
$BT/apksigner sign --ks "$KS" --ks-pass env:SM_KS_PASS --out MonoMusicWidget.apk build/aligned.apk
$BT/apksigner verify MonoMusicWidget.apk && ls -la MonoMusicWidget.apk
