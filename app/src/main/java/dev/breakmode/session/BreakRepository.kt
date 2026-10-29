package dev.breakmode.session

import android.content.Context
import android.content.SharedPreferences
import dev.breakmode.apps.DefaultCommunicationApps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Single source of truth for the app, backed by one private SharedPreferences file.
 *
 * Everything stays on the device: the file is not backed up (see `allowBackup="false"` and
 * `data_extraction_rules.xml`) and the app has no INTERNET permission.
 */
class BreakRepository private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<BreakSettings> = _settings.asStateFlow()

    private val _active = MutableStateFlow(readActive())
    val active: StateFlow<ActiveBreak?> = _active.asStateFlow()

    private val _history = MutableStateFlow(readHistory())
    val history: StateFlow<List<BreakRecord>> = _history.asStateFlow()

    @androidx.annotation.VisibleForTesting
    internal var currentDateProvider: () -> String = { java.time.LocalDate.now().toString() }

    private val _emergencyExitAvailable = MutableStateFlow(isEmergencyExitAvailable())
    val emergencyExitAvailable: StateFlow<Boolean> = _emergencyExitAvailable.asStateFlow()

    fun isEmergencyExitAvailable(): Boolean {
        val lastUsedDate = prefs.getString(KEY_LAST_EMERGENCY_EXIT_DATE, null)
        return lastUsedDate != currentDateProvider()
    }

    fun refreshEmergencyExitStatus() {
        _emergencyExitAvailable.value = isEmergencyExitAvailable()
    }

    // ---------------------------------------------------------------- settings

    fun updateSettings(transform: (BreakSettings) -> BreakSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        prefs.edit()
            .putInt(KEY_MINUTES, updated.minutes)
            .putStringSet(KEY_ALLOWED, updated.allowedPackages)
            .putBoolean(KEY_PAUSE, updated.pauseInterruptions)
            .putBoolean(KEY_UNLOCK, updated.requireUnlockToEnd)
            .putString(KEY_WALLPAPER, updated.wallpaper.name)
            .putString(KEY_SELECTED_BUCKET, updated.selectedBucketId)
            .apply()

        writeBuckets(updated.buckets)
        writeSchedules(updated.schedules)
        FocusScheduleManager.updateNextAlarm(appContext, updated.schedules)
    }

    fun toggleAllowedPackage(packageName: String) {
        updateSettings { current ->
            current.copy(
                allowedPackages = if (packageName in current.allowedPackages) {
                    current.allowedPackages - packageName
                } else {
                    current.allowedPackages + packageName
                },
            )
        }
    }

    fun selectBucket(bucketId: String?) {
        updateSettings { it.copy(selectedBucketId = bucketId) }
    }

    fun updateBucket(bucketId: String, name: String, packages: Set<String>) {
        updateSettings { current ->
            val updatedBuckets = current.buckets.map { b ->
                if (b.id == bucketId) {
                    b.copy(name = name, appPackages = packages.take(AppBucket.MAX_APPS_PER_BUCKET).toSet())
                } else {
                    b
                }
            }
            current.copy(buckets = updatedBuckets)
        }
    }

    fun toggleAppInBucket(bucketId: String, packageName: String) {
        updateSettings { current ->
            val updatedBuckets = current.buckets.map { b ->
                if (b.id == bucketId) {
                    val newPackages = if (packageName in b.appPackages) {
                        b.appPackages - packageName
                    } else {
                        if (b.appPackages.size < AppBucket.MAX_APPS_PER_BUCKET) {
                            b.appPackages + packageName
                        } else {
                            b.appPackages
                        }
                    }
                    b.copy(appPackages = newPackages)
                } else {
                    b
                }
            }
            current.copy(buckets = updatedBuckets)
        }
    }

    fun addSchedule(schedule: FocusSchedule) {
        updateSettings { current ->
            current.copy(schedules = current.schedules + schedule)
        }
    }

    fun updateSchedule(schedule: FocusSchedule) {
        updateSettings { current ->
            val updated = current.schedules.map { if (it.id == schedule.id) schedule else it }
            current.copy(schedules = updated)
        }
    }

    fun deleteSchedule(scheduleId: String) {
        updateSettings { current ->
            current.copy(schedules = current.schedules.filterNot { it.id == scheduleId })
        }
    }

    fun toggleSchedule(scheduleId: String, enabled: Boolean) {
        updateSettings { current ->
            val updated = current.schedules.map {
                if (it.id == scheduleId) it.copy(enabled = enabled) else it
            }
            current.copy(schedules = updated)
        }
    }

    // ------------------------------------------------------------------ break

    /**
     * Starts a break using the current settings and returns it, or the already running break.
     *
     * Called only from an explicit user action (the Start button).
     */
    fun start(): ActiveBreak {
        _active.value?.let { return it }

        val settings = _settings.value
        val now = System.currentTimeMillis()
        val paused = settings.pauseInterruptions && Interruptions.pause(appContext)
        val allowedWithComm = buildSet {
            addAll(settings.effectiveAllowedPackages)
            DefaultCommunicationApps.getPhonePackage(appContext)?.let { add(it) }
            DefaultCommunicationApps.getMessagesPackage(appContext)?.let { add(it) }
        }

        val active = ActiveBreak(
            startedAtMillis = now,
            endsAtMillis = now + settings.minutes * 60_000L,
            allowedPackages = allowedWithComm,
            requireUnlockToEnd = settings.requireUnlockToEnd,
            interruptionsPaused = paused,
        )

        _active.value = active
        prefs.edit()
            .putLong(KEY_STARTED_AT, active.startedAtMillis)
            .putLong(KEY_ENDS_AT, active.endsAtMillis)
            .putStringSet(KEY_SESSION_ALLOWED, active.allowedPackages)
            .putBoolean(KEY_SESSION_UNLOCK, active.requireUnlockToEnd)
            .putBoolean(KEY_SESSION_PAUSED, active.interruptionsPaused)
            .apply()

        BreakNotifier.showOngoing(appContext, active)
        BreakAlarms.schedule(appContext, active.endsAtMillis)
        return active
    }

    /**
     * Starts a break triggered automatically from a schedule.
     */
    fun startScheduled(schedule: FocusSchedule): ActiveBreak {
        _active.value?.let { return it }

        val settings = _settings.value
        val bucket = settings.buckets.firstOrNull { it.id == schedule.bucketId }
        val allowed = bucket?.appPackages ?: settings.allowedPackages
        val now = System.currentTimeMillis()
        val paused = settings.pauseInterruptions && Interruptions.pause(appContext)
        val allowedWithComm = buildSet {
            addAll(allowed)
            DefaultCommunicationApps.getPhonePackage(appContext)?.let { add(it) }
            DefaultCommunicationApps.getMessagesPackage(appContext)?.let { add(it) }
        }

        val active = ActiveBreak(
            startedAtMillis = now,
            endsAtMillis = now + schedule.durationMinutes * 60_000L,
            allowedPackages = allowedWithComm,
            requireUnlockToEnd = settings.requireUnlockToEnd,
            interruptionsPaused = paused,
        )

        _active.value = active
        prefs.edit()
            .putLong(KEY_STARTED_AT, active.startedAtMillis)
            .putLong(KEY_ENDS_AT, active.endsAtMillis)
            .putStringSet(KEY_SESSION_ALLOWED, active.allowedPackages)
            .putBoolean(KEY_SESSION_UNLOCK, active.requireUnlockToEnd)
            .putBoolean(KEY_SESSION_PAUSED, active.interruptionsPaused)
            .apply()

        BreakNotifier.showOngoing(appContext, active)
        BreakAlarms.schedule(appContext, active.endsAtMillis)
        return active
    }

    /**
     * Uses the daily emergency exit to end a running break early.
     * Allowed only once per calendar day; resets at midnight.
     *
     * @return true if the emergency exit was successfully used, false if already exhausted today.
     */
    fun useEmergencyExit(): Boolean {
        if (!isEmergencyExitAvailable()) return false
        val today = currentDateProvider()
        prefs.edit().putString(KEY_LAST_EMERGENCY_EXIT_DATE, today).apply()
        _emergencyExitAvailable.value = false
        finish(completed = false)
        return true
    }

    /**
     * Ends the running break, recording it in the history.
     *
     * @param completed true when the timer ran out, false when the user ended it early.
     */
    fun finish(completed: Boolean) {
        val active = _active.value ?: return

        val elapsedMinutes = (((System.currentTimeMillis() - active.startedAtMillis) / 60_000L) + 1)
            .coerceIn(1L, (active.totalMillis / 60_000L).coerceAtLeast(1L))
            .toInt()
        val minutes = if (completed) (active.totalMillis / 60_000L).toInt() else elapsedMinutes

        if (active.interruptionsPaused) Interruptions.resume(appContext)

        BreakAlarms.cancel(appContext)
        BreakNotifier.clearOngoing(appContext)
        if (completed) BreakNotifier.showComplete(appContext, minutes)

        _history.value = (listOf(BreakRecord(System.currentTimeMillis(), minutes, completed)) +
            _history.value).take(MAX_HISTORY)
        writeHistory(_history.value)

        _active.value = null
        prefs.edit()
            .remove(KEY_STARTED_AT)
            .remove(KEY_ENDS_AT)
            .remove(KEY_SESSION_ALLOWED)
            .remove(KEY_SESSION_UNLOCK)
            .remove(KEY_SESSION_PAUSED)
            .apply()
    }

    /** Adds minutes to the running break, capped at two extra hours. */
    fun extend(minutes: Int) {
        if (minutes <= 0) return
        val active = _active.value ?: return
        val extended = active.copy(
            endsAtMillis = active.endsAtMillis + minutes.coerceAtMost(120) * 60_000L,
        )
        _active.value = extended
        prefs.edit().putLong(KEY_ENDS_AT, extended.endsAtMillis).apply()
        BreakNotifier.showOngoing(appContext, extended)
        BreakAlarms.schedule(appContext, extended.endsAtMillis)
    }

    /**
     * Closes out a break whose time has passed while the app was in the background or killed.
     * Safe to call on every resume: it never starts or extends anything.
     */
    fun syncWithClock() {
        refreshEmergencyExitStatus()
        val active = _active.value ?: return
        if (active.isOver()) finish(completed = true)
    }

    fun clearHistory() {
        _history.value = emptyList()
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    // -------------------------------------------------------------- persistence

    private fun readSettings(): BreakSettings {
        val wallpaper = runCatching {
            Wallpaper.valueOf(prefs.getString(KEY_WALLPAPER, Wallpaper.AMOLED.name)!!)
        }.getOrDefault(Wallpaper.AMOLED)
        return BreakSettings(
            minutes = prefs.getInt(KEY_MINUTES, 30).coerceIn(5, 480),
            allowedPackages = prefs.getStringSet(KEY_ALLOWED, emptySet()).orEmpty().toSet(),
            pauseInterruptions = prefs.getBoolean(KEY_PAUSE, true),
            requireUnlockToEnd = prefs.getBoolean(KEY_UNLOCK, false),
            wallpaper = wallpaper,
            buckets = readBuckets(),
            selectedBucketId = prefs.getString(KEY_SELECTED_BUCKET, "travel"),
            schedules = readSchedules(),
        )
    }

    private fun readBuckets(): List<AppBucket> {
        val raw = prefs.getString(KEY_BUCKETS, null) ?: return BreakSettings.defaultBuckets()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    val name = obj.getString("name")
                    val pkgArray = obj.optJSONArray("packages")
                    val pkgs = if (pkgArray != null) {
                        buildSet {
                            for (j in 0 until pkgArray.length()) {
                                add(pkgArray.getString(j))
                            }
                        }
                    } else {
                        emptySet()
                    }
                    add(AppBucket(id = id, name = name, appPackages = pkgs))
                }
            }.take(AppBucket.MAX_BUCKETS)
        }.getOrDefault(BreakSettings.defaultBuckets())
    }

    private fun writeBuckets(buckets: List<AppBucket>) {
        val array = JSONArray()
        buckets.take(AppBucket.MAX_BUCKETS).forEach { bucket ->
            val obj = JSONObject()
                .put("id", bucket.id)
                .put("name", bucket.name)
            val pkgs = JSONArray()
            bucket.appPackages.take(AppBucket.MAX_APPS_PER_BUCKET).forEach { pkgs.put(it) }
            obj.put("packages", pkgs)
            array.put(obj)
        }
        prefs.edit().putString(KEY_BUCKETS, array.toString()).apply()
    }

    private fun readSchedules(): List<FocusSchedule> {
        val raw = prefs.getString(KEY_SCHEDULES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val daysArray = obj.optJSONArray("days")
                    val days = if (daysArray != null) {
                        buildSet {
                            for (j in 0 until daysArray.length()) add(daysArray.getInt(j))
                        }
                    } else {
                        setOf(1, 2, 3, 4, 5, 6, 7)
                    }
                    add(
                        FocusSchedule(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            startHour = obj.getInt("startHour"),
                            startMinute = obj.getInt("startMinute"),
                            durationMinutes = obj.getInt("durationMinutes"),
                            daysOfWeek = days,
                            bucketId = obj.optString("bucketId").takeIf { it.isNotEmpty() },
                            enabled = obj.optBoolean("enabled", true),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeSchedules(schedules: List<FocusSchedule>) {
        val array = JSONArray()
        schedules.forEach { s ->
            val obj = JSONObject()
                .put("id", s.id)
                .put("name", s.name)
                .put("startHour", s.startHour)
                .put("startMinute", s.startMinute)
                .put("durationMinutes", s.durationMinutes)
                .put("enabled", s.enabled)
            s.bucketId?.let { obj.put("bucketId", it) }
            val days = JSONArray()
            s.daysOfWeek.forEach { days.put(it) }
            obj.put("days", days)
            array.put(obj)
        }
        prefs.edit().putString(KEY_SCHEDULES, array.toString()).apply()
    }

    private fun readActive(): ActiveBreak? {
        val startedAt = prefs.getLong(KEY_STARTED_AT, 0L)
        val endsAt = prefs.getLong(KEY_ENDS_AT, 0L)
        if (startedAt == 0L || endsAt == 0L) return null
        return ActiveBreak(
            startedAtMillis = startedAt,
            endsAtMillis = endsAt,
            allowedPackages = prefs.getStringSet(KEY_SESSION_ALLOWED, emptySet()).orEmpty().toSet(),
            requireUnlockToEnd = prefs.getBoolean(KEY_SESSION_UNLOCK, false),
            interruptionsPaused = prefs.getBoolean(KEY_SESSION_PAUSED, false),
        )
    }

    private fun readHistory(): List<BreakRecord> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        BreakRecord(
                            endedAtMillis = item.getLong("at"),
                            minutes = item.getInt("min"),
                            completed = item.optBoolean("done", true),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeHistory(records: List<BreakRecord>) {
        val array = JSONArray()
        records.take(MAX_HISTORY).forEach { record ->
            array.put(
                JSONObject()
                    .put("at", record.endedAtMillis)
                    .put("min", record.minutes)
                    .put("done", record.completed),
            )
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "break_mode_prefs"
        private const val MAX_HISTORY = 60

        private const val KEY_MINUTES = "minutes"
        private const val KEY_ALLOWED = "allowed_packages"
        private const val KEY_PAUSE = "pause_interruptions"
        private const val KEY_UNLOCK = "require_unlock"
        private const val KEY_WALLPAPER = "wallpaper"
        private const val KEY_BUCKETS = "app_buckets"
        private const val KEY_SELECTED_BUCKET = "selected_bucket_id"
        private const val KEY_SCHEDULES = "focus_schedules"

        private const val KEY_STARTED_AT = "break_started_at"
        private const val KEY_ENDS_AT = "break_ends_at"
        private const val KEY_SESSION_ALLOWED = "break_allowed_packages"
        private const val KEY_SESSION_UNLOCK = "break_require_unlock"
        private const val KEY_SESSION_PAUSED = "break_interruptions_paused"

        private const val KEY_HISTORY = "history"
        private const val KEY_LAST_EMERGENCY_EXIT_DATE = "last_emergency_exit_date"

        @Volatile
        private var instance: BreakRepository? = null

        fun get(context: Context): BreakRepository = instance ?: synchronized(this) {
            instance ?: BreakRepository(context).also { instance = it }
        }

        /** Drops the cached instance so a test starts from a clean repository. Test-only. */
        @androidx.annotation.VisibleForTesting
        internal fun resetForTests() {
            synchronized(this) { instance = null }
        }
    }
}
