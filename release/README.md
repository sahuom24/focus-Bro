# FocusBro v1.1.0 Release Artifacts

These build artifacts are ready for Google Play Store publishing and on-device testing.

---

### Files in this folder

| File | Purpose | How to use |
|---|---|---|
| **`FocusBro-v1.1.0-release.aab`** | **Google Play Store Publishing** | Upload this file to **Google Play Console** &rarr; Production / Open Testing / Closed Testing track. Play App Signing handles key signing automatically. |
| **`FocusBro-v1.1.0-debug.apk`** | **Direct Phone Testing** | Sideload directly to your Android device via USB or file transfer. Already signed with debug keys. |
| **`FocusBro-v1.1.0-release-unsigned.apk`** | **Direct Release APK** | R8-optimized release binary. Can be signed with your private release keystore for F-Droid or GitHub Releases. |
| **`SHA256SUMS.txt`** | **Integrity Check** | SHA-256 checksums for verifying file integrity. |

---

### What's New in v1.1.0

1. **Monochrome Tactile "Slide to Exit"**:
   - Clean, colorless black & white slider (`#141416` track, `#E2E2E2` thumb, `#000000` icon).
   - Removed all reddish/coral tint for an ultra-clean AMOLED aesthetic.

2. **Clean Text-Only App Dock**:
   - Focus session dock lists allowed apps cleanly by **name only** (no distracting colored icons).

3. **BlockIt-Mimicked Automatic Routines**:
   - Segmented digital clock (`START : END`) with live duration calculation.
   - Continuous 24-hour dotted timeline with routine time block fill.
   - Routine 1, 2, 3 tabs with active mint underline and tactile toggle.
   - S M T W T F S day-of-week active chips.
   - Tactile ruler cards for interactive start and end time adjustments.
   - Bag assignment per routine and custom routine renaming.

4. **Lockdown Mode (Display Over Other Apps)**:
   - Uses `SYSTEM_ALERT_WINDOW` permission so FocusBro locks down as your home screen during a focus session.
   - Suppresses launcher and task-switching escapes, ensuring only chosen apps can be opened.

5. **UPI & Banking Protection (100% Payment Safe)**:
   - Removed invasive accessibility flags (`flagIncludeNotImportantViews`) so Indian and global banking apps never flag FocusBro as an overlay hazard.
   - Hardcoded and dynamic intent whitelisting for Google Pay, PhonePe, Paytm, BHIM, CRED, Navi, WhatsApp Pay, YONO SBI, HDFC, ICICI, Axis, Kotak, etc. Payments will never be blocked or interrupted.

6. **Extended Durations & Expanded Bags**:
   - Custom focus duration up to **12 hours (720 minutes)**.
   - Up to **6 apps per bag** (across Default, Study, and Travel bags).

---

### Release Metadata

- **Application ID**: `dev.breakmode`
- **Version Name**: `1.1.0`
- **Version Code**: `2`
- **Min SDK**: `26` (Android 8.0)
- **Target SDK**: `36` (Android 16 — fully compliant with Google Play target API rules)
- **Network Permission**: None (`android.permission.INTERNET` is NOT requested; 100% offline & private)
- **Release AAB Size**: ~5.6 MB
