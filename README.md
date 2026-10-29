# Break Mode

A small, offline Android app — a **timed break with only the apps
you allow** — but on any Android phone, and without any of the invasive machinery that a "phone
locker" app needs.

Android 8.0+ (API 26), 5.6 MB, no account, no internet permission.

- **Install:** [`release/BreakMode-1.0.0-release.apk`](release/BreakMode-1.0.0-release.apk) — signed, not debuggable
- **Debug build:** [`dist/BreakMode-1.0.0-debug.apk`](dist/BreakMode-1.0.0-debug.apk)
- **Research behind the design:** [`docs/moto-unplugged-research.md`](docs/moto-unplugged-research.md)

---

## What it does

| | |
| --- | --- |
| Pick a length | 15, 30, 45, 60 or 90 minutes. |
| Pick the apps you keep | Every app with a home-screen shortcut. Leave it empty for a completely blank break screen. |
| Optional quiet | Do Not Disturb for the duration — but only if it is currently off, and it is switched back afterwards. |
| Optional friction | "Require unlock to end": ending early goes through the phone's own lock screen. |
| Pick a background | Three built-in gradients (Dusk, Forest, Night). |
| The break screen | Full-screen countdown ring, the apps you kept, `+10 min`, and `End break`. |
| On-device summary | Today's minutes and a seven-day bar chart, kept locally. Nothing is ever uploaded. |

A break is **always exitable** — that is deliberate. The app is a nudge and a nicer place to be, not
a lock.

## Why it does not block other apps

Moto Unplugged is documented by Motorola as a *session* with chosen apps and chosen interruptions; it
is not a parental control. Copying that honestly means the restriction is **voluntary**: this app
shows its own full-screen session and hides everything else from *its* surface. It never tries to
prevent you from pressing Home.

Trying to be stricter would be neither safe nor shippable: an `AccessibilityService` or a full-screen
overlay used to keep you out of other apps is restricted by Google Play, `QUERY_ALL_PACKAGES` needs a
special declaration, and lock-task/kiosk mode is a managed-device feature. The choices below are the
ones a normal app is allowed to make.

## Permissions — the whole list

Verified with `aapt dump permissions` on the built APK:

```
uses-permission: name='android.permission.POST_NOTIFICATIONS'
uses-permission: name='android.permission.ACCESS_NOTIFICATION_POLICY'
uses-permission: name='dev.breakmode.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'   (added by AndroidX)
```

| Permission | When | If refused |
| --- | --- | --- |
| `POST_NOTIFICATIONS` | Android 13+, asked when you tap Start | The break still runs; there is just no notification in the shade. |
| `ACCESS_NOTIFICATION_POLICY` | Only when you enable "Silence interruptions" | The switch stays off and the app falls back to leaving notifications alone. |

**There is no `INTERNET` permission.** The app cannot open a socket, so your installed-app list,
break history and settings physically cannot leave the phone. Also deliberately absent:
`QUERY_ALL_PACKAGES` (a narrow `MAIN`/`LAUNCHER` `<queries>` intent is used instead),
`SYSTEM_ALERT_WINDOW`, any `AccessibilityService`, and `SCHEDULE_EXACT_ALARM` (the end-of-break alarm
is inexact and permission-free).

Other hardening:

- `allowBackup="false"` plus `data_extraction_rules.xml` excluding everything, so neither cloud
  backup nor device transfer carries the break history.
- No analytics, no crash reporting, no third-party SDKs — the dependency list is AndroidX and
  Compose only.
- The "require unlock" check uses the system credential sheet (`KeyguardManager`), so the app never
  sees your PIN, pattern or biometric data.
- Everything is written to one private `SharedPreferences` file inside the app sandbox.

## How it behaves

- **The countdown is derived from a stored end time**, not from a ticker, so it stays correct across
  screen-off, rotation and process death. No foreground service is needed for that.
- **A break that expires while the app is closed** is closed out by either the notification's
  countdown, the inexact alarm, or the next time the app is opened — whichever comes first. In every
  case Do Not Disturb is restored and a "break complete" notice is posted.
- **Do Not Disturb is never stolen.** If DND is already on when a break starts, the app leaves it
  alone. On targets ≥ Android 15 the system turns such calls into an app-attributed implicit Zen rule
  that other rules are combined with, so the app never assumes it owns the global state.

## Build and install

Requirements: JDK 17, Android SDK platform 35 + build-tools 35.

```bash
# local.properties is machine-specific and git-ignored; point it at your SDK
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### The prebuilt APK

[`release/BreakMode-1.0.0-release.apk`](release/BreakMode-1.0.0-release.apk) is a signed release build
(`android:debuggable="false"`, no Compose tooling, 5.6 MB). Verify and install it:

```bash
cd release && sha256sum -c SHA256SUMS.txt
adb install -r BreakMode-1.0.0-release.apk
```

Or copy it to the phone and open it (sideloading needs "install unknown apps" for the file manager you
use). It is signed with a **local throwaway key**, not a Play key, so if an earlier debug build is
already installed, uninstall that first — `adb uninstall dev.breakmode` — otherwise `adb install -r`
fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE` because the signatures differ.

To sign your own release build, drop a git-ignored `keystore.properties` at the repo root:

```properties
storeFile=keystore/breakmode-release.jks
storePassword=***
keyAlias=breakmode
keyPassword=***
```

Then `./gradlew assembleRelease`. Without that file the build still assembles, just unsigned.

Versions are pinned to a verified-compatible set: AGP 8.6.1, Gradle 8.7, Kotlin 2.0.0,
Compose BOM 2024.06.00, `compileSdk`/`targetSdk` 35, `minSdk` 26.

## Verification status

- `./gradlew assembleDebug` — **passes**; APK produced (8.1 MB) and permissions audited with `aapt`.
- `./gradlew assembleRelease` — **passes**; `apksigner verify` confirms one signer, and `aapt2 dump
  badging` on the packaged 5.6 MB APK shows `minSdkVersion:'26'`, `targetSdkVersion:'35'`, no
  `application-debuggable`, and only `POST_NOTIFICATIONS` + `ACCESS_NOTIFICATION_POLICY` (plus the
  `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` that AGP injects for AndroidX — signature-level, grants
  nothing). Still no `INTERNET`.
- `./gradlew lintDebug` — **0 errors**; the 7 remaining warnings are all "a newer library version
  exists", which is intentional pinning.
- `./gradlew testDebugUnitTest` — **18 tests, 0 failures** (Robolectric, real Android runtime on the
  JVM): cold start of `MainActivity`, starting a break from the UI, all four screens laid out and
  asserted against the window bounds, the session callbacks, notification posting, and the full
  break lifecycle including restore-from-disk and expiry.
- **No emulator or physical device.** This machine is Windows on ARM (Snapdragon X Elite,
  `OSArchitecture: ARM 64-bit`), and Google ships no Windows-ARM64 emulator: the `windows_x64`
  emulator binary segfaults (exit 139) under Windows' x86 emulation, and hardware acceleration is
  unavailable, so the AVD created here cannot boot. Screens were therefore never rendered to real
  pixels — Robolectric's native-graphics mode also has no Windows ARM support. Visual polish and the
  device-credential and DND hand-offs still need a real phone (just plug one in and `adb install`).

### Running the tests

Robolectric needs a native Conscrypt library that has no Windows-ARM64 build, so on an ARM Windows
host the tests must run on an x64 JVM:

```bash
JAVA_HOME=/path/to/jdk17-x64 ./gradlew testDebugUnitTest
```

On x86_64 hosts the normal `./gradlew testDebugUnitTest` works.

### Bugs this run actually found and fixed

1. The "Keep available" card printed the app name twice when exactly one app was allowed (title and
   subtitle); the subtitle is now "Kept available during your break".
2. `extend()` accepted negative minutes and quietly *extended* the break instead of ignoring the
   call.

## Limits and next steps

- **Play submission:** new apps and updates must target API 36 since 31 Aug 2026; this build targets
  35 because platform 36 is not installed here. Install `platforms;android-36`, bump
  `compileSdk`/`targetSdk`, and re-run the target-version behaviour checks.
- **Not a blocker.** If the phone must be genuinely locked, the honest options are Digital Wellbeing
  Focus mode, Android's screen pinning, or a managed device — not a third-party overlay.
- Ideas that fit the existing design: a Quick Settings tile (as Moto has), a home-screen widget, and
  scheduled/sleep breaks. All are optional additions to the same session model.

## Layout

```
app/src/main/java/dev/breakmode/
  apps/AppCatalog.kt          # launchable apps via the narrow <queries> intent
  session/BreakRepository.kt  # settings + active break + history, one private prefs file
  session/BreakModels.kt      # BreakSettings, ActiveBreak, BreakRecord, Wallpaper
  session/Interruptions.kt    # reversible DND handling
  session/BreakNotifier.kt    # ongoing countdown + completion notice
  session/BreakAlarms.kt      # inexact end-of-break alarm and its receiver
  ui/MainActivity.kt          # routing, permission and credential flows
  ui/SetupScreen.kt           # duration, apps, toggles, wallpaper, stats
  ui/SessionScreen.kt         # the break itself
  ui/PickerScreen.kt          # "keep available" app picker
app/src/test/java/dev/breakmode/
  session/BreakRepositoryTest.kt   # lifecycle, persistence, notification posting (Robolectric)
  ui/ScreensRenderTest.kt          # per-screen layout and callback checks
  ui/MainActivityStartTest.kt      # cold start and start-break flow through the real Activity
docs/moto-unplugged-research.md
```
