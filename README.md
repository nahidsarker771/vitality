# Vitality

Three daily power bars in real time — **Willpower**, **Wealth Power** and
**Energy Power**. Willpower and Wealth start at 100% at 05:00 and drain to 0%
at 22:00, then reset overnight. Energy follows your real activity: steps and
movement drain it, rest and sleep restore it. Fully offline, native Android
(Java), no WebView.

**Download:** [latest APK](https://github.com/nahid77777771-maker/vitality/releases/latest/download/Vitality.apk)
· **Web:** [nahid77777771-maker.github.io/vitality](https://nahid77777771-maker.github.io/vitality/)

## Features

- Live countdown with seconds precision and an overall power ring
- Track tab: step counter, distance (step + GPS), active minutes, last 7 days
- Activity-aware Energy Power: step drain, active/light/rest modes, sleep recovery
- Automatic sleep detection at night from stillness and screen state
- Foreground tracking service with an ongoing status notification, resumes on boot
- 8 themes (incl. a light theme); widgets follow the active theme
- SQLite history: 7-day average, 14-day chart, per-day details, CSV export with steps
- Four home-screen widgets: 2×2, 4×1, 4×2 and 4×4
- Threshold alerts at 75 / 50 / 25 / 10% with per-bar switches
- Alarm-based background sampling and widget refresh, rescheduled on boot
- Android 10+ (minSdk 29, targetSdk 34), no INTERNET permission

## Install

Download `Vitality.apk` from the [latest release](https://github.com/nahid77777771-maker/vitality/releases/latest)
and open it on an Android 10+ phone. Allow install from unknown sources when
asked. Updates must be installed over the existing app with the same signing
key.

## Build

Requirements: JDK 17, Android SDK with build-tools 34.0.0 and platform
android-34.

```bash
# create your own signing key (or restore yours)
keytool -genkeypair -keystore vitality.jks -alias vitality \
  -keyalg RSA -keysize 2048 -validity 10000

echo 'your-keystore-password' > .ks-pass   # or export VITALITY_KS_PASS

./build-apk.sh          # builds VERSION → ../apk/Vitality-v<ver>.apk
./build-apk.sh 1.1      # explicit version
```

`VERSION` holds the next version; the script bumps the patch after each build.
`versionCode = major × 10000 + minor × 100 + patch`.

The pipeline is aapt2 → javac → d8 → `classes.dex` at APK root → zipalign →
apksigner. No Gradle.

## Structure

```
AndroidManifest.xml
res/                  layouts, drawables, widgets, themes, strings
src/com/nahid/vitality/
  core/               time model, energy model, themes, preferences
  data/               SQLite history + activity records
  track/              sensors, sleep detection, foreground service
  notify/             alarms, notifications, widget refreshes
  ui/                 custom views
  widget/             four widget providers
build-apk.sh          versioned APK builder
env.sh                JDK/SDK paths
docs/                 GitHub Pages landing page
```

## Signing

Release APKs are signed with a personal key, which is **not** in this
repository. To publish your own builds, use your own keystore and password
(`vitality.jks` + `.ks-pass`, both gitignored).
