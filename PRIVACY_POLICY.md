# Privacy Policy for FocusBro (Break Mode)

**Last Updated:** November 24, 2026  
**Effective Date:** November 24, 2026  

FocusBro ("Break Mode", "we", "our", or "the app"), developed under package `dev.breakmode`, is committed to protecting your privacy. This Privacy Policy outlines our practices regarding data collection, usage, and device permissions.

---

### 1. Summary: 100% Offline & Zero Data Collection

- **No Data Collection:** We do not collect, transmit, store on remote servers, sell, or share any personal information, browsing history, app usage habits, or device identifiers.
- **No Internet Access:** FocusBro **does not declare or use the `android.permission.INTERNET` permission**. The application is physically incapable of transmitting any data off your device or connecting to external servers.
- **No Third-Party SDKs or Trackers:** There are no advertising SDKs, analytics libraries (such as Google Analytics or Firebase), crash-reporting services, or third-party trackers integrated into the app.

---

### 2. Device Permissions and How They Are Used

FocusBro operates strictly locally on your device. To provide core focus, scheduling, and distraction-blocking functionality, the app requests the following Android permissions:

#### A. Accessibility Service (`android.permission.BIND_ACCESSIBILITY_SERVICE`)
- **Purpose:** FocusBro uses an Accessibility Service (`FocusAccessibilityService`) solely to detect the package name of the currently active foreground app during an active focus session. When an unselected (blocked) app is opened, FocusBro displays a gentle focus reminder overlay to help you stay on track.
- **Privacy Assurance:** The Accessibility Service:
  - **Does NOT** inspect, read, or record screen content, text, passwords, or personal messages.
  - **Does NOT** log or track your keystrokes or inputs.
  - **Does NOT** collect or store personal data.
  - **Does NOT** send any information over any network (as the app has zero network capability).
  - Can be enabled or disabled by you at any time in Android System Settings.

#### B. Accessibility & System Overlay (`android.permission.SYSTEM_ALERT_WINDOW`)
- **Purpose:** Used to display the focus reminder screen or break overlay over blocked apps during an active focus session.

#### C. Notification Management (`android.permission.ACCESS_NOTIFICATION_POLICY` & `android.permission.POST_NOTIFICATIONS`)
- **Post Notifications:** Used to display ongoing break countdowns, quick action controls (e.g., extend break, finish break), and completion notifications.
- **Access Notification Policy (Do Not Disturb):** An optional permission that allows FocusBro to automatically silence notifications (turn on Do Not Disturb) during your focus session and restore your previous notification settings when the break ends. FocusBro does not read or intercept your notifications.

#### D. Exact Alarms & Scheduling (`android.permission.SCHEDULE_EXACT_ALARM`, `android.permission.USE_EXACT_ALARM`)
- **Purpose:** Used exclusively to trigger your custom focus schedules at the exact time you configured, even when the device is in low-power Doze mode.

#### E. Startup on Boot (`android.permission.RECEIVE_BOOT_COMPLETED`)
- **Purpose:** Used to reschedule your active focus timers or recurring focus schedules when your device restarts.

#### F. Installed Application Queries (`<queries>`)
- **Purpose:** FocusBro queries launcher apps locally on your device to display a selection list, allowing you to choose which apps remain accessible during a focus session (such as Phone, Messages, or essential tools). This list is processed entirely in device memory and is never exported.

---

### 3. Data Storage and Retention

- All user preferences (such as selected focus apps, schedule timers, and local session statistics) are stored locally in the app's private sandbox (`SharedPreferences`).
- Cloud backup is disabled (`allowBackup="false"`), meaning your focus history does not transfer to cloud storage.
- You can delete all data at any time by clearing the app's cache and storage in your device settings or by uninstalling the app.

---

### 4. Children’s Privacy

FocusBro does not collect any personal information from anyone, including children under the age of 13 (or under the applicable age limit in your jurisdiction). The app is safe for all audiences.

---

### 5. Third-Party Links

FocusBro does not contain third-party advertisements or external links, except for optional links to the open-source code repository or issue tracker on GitHub.

---

### 6. Changes to This Privacy Policy

We may update this Privacy Policy from time to time. Any changes will be reflected by updating the "Last Updated" date at the top of this document. We encourage you to review this policy periodically.

---

### 7. Contact Us

If you have any questions, concerns, or feedback regarding this Privacy Policy or the app's practices, please contact us:

- **GitHub Repository & Issues:** [https://github.com/sahuom24/focus-Bro](https://github.com/sahuom24/focus-Bro)
- **Email:** imsahuom@gmail.com
