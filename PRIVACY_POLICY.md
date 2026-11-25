# Privacy Policy for FocusBro (Break Mode)

**Last Updated:** November 26, 2026  
**Effective Date:** November 24, 2026  

FocusBro ("Break Mode", "we", "our", or "the app"), published under package `com.focusbro.myapp`, is committed to protecting your privacy. This Privacy Policy outlines our practices regarding data collection, usage, and device permissions.

---

### 1. Summary: 100% Offline & Zero Data Collection

- **No Data Collection:** We do not collect, transmit, store on remote servers, sell, or share any personal information, browsing history, app usage habits, or device identifiers.
- **No Internet Access:** FocusBro **does not declare or use the `android.permission.INTERNET` permission**. The application is physically incapable of transmitting any data off your device or connecting to external servers.
- **No Third-Party SDKs or Trackers:** There are no advertising SDKs, analytics libraries (such as Google Analytics or Firebase), crash-reporting services, or third-party trackers integrated into the app.

---

### 2. Device Permissions and How They Are Used

FocusBro operates strictly locally on your device. To provide core focus, scheduling, and distraction-blocking functionality, the app requests the following Android permissions:

#### A. Accessibility Service (`android.permission.BIND_ACCESSIBILITY_SERVICE`)
- **Purpose:** FocusBro uses an optional Accessibility Service (`FocusAccessibilityService`) to check the foreground app package and window class (to recognize system security prompts) during an active focus session. When an unselected (distracting) app is opened, FocusBro brings your FocusBro session screen back to the foreground to help you stay on track.
- **User Control & Non-Interference:** System Settings and package installer/uninstall screens are **never** blocked, and you can grant, decline, or revoke Accessibility access at any time in Android System Settings.
- **Privacy Assurance:** The Accessibility Service:
  - **Does NOT** inspect, read, or record screen content, text, passwords, or personal messages (`canRetrieveWindowContent="false"`).
  - **Does NOT** log or track your keystrokes or inputs.
  - **Does NOT** collect, store, or share personal or sensitive user data.
  - **Does NOT** transmit any information over any network (the app has zero `INTERNET` permission).

#### B. Notification Management (`android.permission.ACCESS_NOTIFICATION_POLICY` & `android.permission.POST_NOTIFICATIONS`)
- **Post Notifications:** Used to display ongoing break countdowns and completion notifications.
- **Access Notification Policy (Do Not Disturb):** An optional permission that allows FocusBro to automatically silence interruptions (turn on Do Not Disturb) during your focus session and restore your previous notification settings when the session ends. FocusBro does not read or intercept your notifications.

#### C. Alarms & Scheduling (`android.permission.SCHEDULE_EXACT_ALARM`)
- **Purpose:** Used exclusively to trigger your custom focus routines at the scheduled start time, even when the device is in Doze mode.

#### D. Startup on Boot (`android.permission.RECEIVE_BOOT_COMPLETED`)
- **Purpose:** Used to reschedule your active focus timers or recurring focus schedules when your device restarts.

#### E. Installed Application Queries (`<queries>`)
- **Purpose:** FocusBro queries launcher apps locally on your device to display a selection list, allowing you to choose which apps remain accessible during a focus session (such as Phone, Messages, Calendar, or UPI). This list is processed entirely in device memory and is never exported.

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
