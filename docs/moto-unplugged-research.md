# Moto Unplugged — what it is, and what a safe equivalent looks like

Research notes compiled on 2026-10-28, used to shape **Break Mode**.

## 1. What Moto Unplugged actually does

Moto Unplugged is a **user-started, timed "break" session** that ships with Motorola's Moto app
suite on *some* phones in *some* countries. It is **not** a parental-control or device-lock
product.

Per Motorola's own support documentation:

| Aspect | Behaviour |
| --- | --- |
| Launch | The **Moto Unplugged app**, or the **Moto Unplugged Quick Settings tile**. |
| Setup | Tap **Customize** to choose the **apps that stay available**, a **session wallpaper**, and **what may interrupt** the session. |
| Optional friction | You can require your **normal phone-unlock password** so ending a session early is deliberate. |
| Duration | You pick a **timeframe**, then tap **Start**; the first Start grants requested permissions. |
| During the session | You see **the apps you chose, and nothing else**; notifications stay out of the notification shade, but **allowed calls/messages can still come through**. |
| Ending early | Tap the **widget**, the **Quick Settings tile**, or the **session notification**. With the password option on, ending early can require the device credential. |
| Availability | Only some models/regions; it can be missing entirely from a Motorola phone. |

Sources:
- Motorola support, *Manage how you spend time on your phone* — <https://help.motorola.com/hc/3590/16/na/en-us/CGT1811062324.html>
- Motorola support (moto g75 5G variant) — <https://help.motorola.com/hc/3575/15/global/en-us/CGT1811062324.html>
- Motorola News, *Introducing Moto Unplugged* — <https://motorolanews.com/introducing-moto-unplugged-designed-to-help-users-stay-in-the-moment-while-staying-connected/>

### What Motorola does *not* document

Motorola never states the implementation. So, factually:

- It is **not documented** as Android's **Do Not Disturb**; Motorola documents DND separately,
  as the tool for *muting sounds, stopping vibration and blocking calls/notifications*.
- It is **not documented** as a launcher replacement or an accessibility-based app blocker.
- It **does not promise total disconnection** — that is the entire point: "stay in the moment
  *while staying connected*".

Do not repeat the common claim that Moto Unplugged "locks your phone" or "blocks calls". It does
neither by default; it restricts the surface and lets you choose the interruptions.

## 2. How it compares with the built-in Android features

| | Moto Unplugged | Digital Wellbeing **Focus mode** | **Do Not Disturb** |
| --- | --- | --- | --- |
| Purpose | On-demand, personalised break | Pause distracting apps | Filter interruptions |
| Scope | Chosen apps + chosen interruptions + wallpaper | Selected apps cannot be opened, notifications paused | Calls, messages, app notifications, alarms |
| Feel | Full-screen "session" experience | App list, no dedicated session screen | System-wide policy |
| Scheduling | Manual start, fixed timeframe | Manual or scheduled, plus "Take a break" | Manual or scheduled |

Sources:
- Android Help, *Digital Wellbeing* — <https://support.google.com/android/answer/9346420>
- Android Help, *Modes and Do Not Disturb* — <https://support.google.com/android/answer/9069335>

## 3. What a *safe* third-party equivalent may do (Android 14–16, Google Play)

The blocker for a Play-distributed clone is that **Android gives a normal app no dependable,
policy-safe way to prevent the device owner from opening other apps**. Everything that could
enforce a lock is either restricted or explicitly against policy:

| Technique | Verdict |
| --- | --- |
| `QUERY_ALL_PACKAGES` to enumerate every app | Restricted on Play, requires a declaration; a focus app does not clearly qualify. Use a narrow `<queries>` intent for launcher apps instead. |
| `AccessibilityService` to detect and block apps | Play prohibits using the Accessibility API for non-accessibility purposes; needs disclosure, consent and a declaration. Not appropriate here. |
| `SYSTEM_ALERT_WINDOW` full-screen overlay | Not a real lock; revocable; must not obscure permission/system UI. |
| Lock task mode / kiosk | Managed-device (DPC) feature, not for consumer apps. |
| Full-screen-intent notification to force a screen | Play limits it to calling/alarm use cases. |
| Foreground service purely to stay alive | Requires a matching declared FGS type and a Play declaration; a focus timer does not fit one. |
| `ACCESS_NOTIFICATION_POLICY` for DND | Legitimate **with user-granted special access**, as long as the change is user-requested and reversibly scoped. |
| Device-credential / biometric confirmation | Legitimate, no special permission: `KeyguardManager.createConfirmDeviceCredentialIntent` (or `BiometricPrompt`). |

Important Android 15+/target-35 behaviour change: apps can no longer flip the *global* DND state;
`setInterruptionFilter()` becomes an app-attributed implicit automatic Zen rule, combined with other
rules by most-restrictive-wins. So an app must never assume it "owns" DND.

Sources:
- <https://developer.android.com/guide/topics/permissions/overview>
- <https://developer.android.com/training/permissions/requesting>
- <https://developer.android.com/about/versions/15/behavior-changes-15>
- <https://developer.android.com/guide/topics/manifest/queries-element>
- <https://support.google.com/googleplay/android-developer/answer/10158779>
- <https://support.google.com/googleplay/android-developer/answer/10964491>
- <https://support.google.com/googleplay/android-developer/answer/13392821>
- <https://developer.android.com/google/play/requirements/target-sdk>

## 4. Decisions taken for Break Mode

| Moto Unplugged idea | Break Mode implementation | Why |
| --- | --- | --- |
| Timed break session | Countdown derived from a persisted end timestamp | Survives process death, screen-off and clock ticks without a foreground service. |
| Choose which apps stay | Launcher-app picker, frozen into the session at start | Uses a narrow `<queries>` intent, **no** `QUERY_ALL_PACKAGES`. |
| Session wallpaper | Three built-in calm gradients | Zero assets, zero downloads. |
| Notifications stay out of the shade | *Optional* DND, applied only when DND is currently **off**, restored at the end | Never clobbers a DND state the user set; degrades gracefully when access is denied. |
| Password to end early | *Optional* device-credential confirmation | System credential sheet; the app never sees the PIN or biometrics. |
| QS tile / widget launch | Not implemented in v1 | Adds a service surface for little value; listed as a next step. |
| "Nothing else visible" | The session screen *is* the app's own screen | No overlays, no accessibility spying, no app blocking. The break is voluntary and always exitable. |
| — | **No `INTERNET` permission at all** | Strongest possible privacy claim: the app *cannot* transmit app lists, usage or anything else. |
