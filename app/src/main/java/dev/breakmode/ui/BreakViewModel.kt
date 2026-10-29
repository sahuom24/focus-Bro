package dev.breakmode.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.breakmode.apps.AppCatalog
import dev.breakmode.apps.LaunchableApp
import dev.breakmode.blocker.FocusAccessibilityService
import dev.breakmode.session.ActiveBreak
import dev.breakmode.session.AppBucket
import dev.breakmode.session.BreakRecord
import dev.breakmode.session.BreakRepository
import dev.breakmode.session.BreakSettings
import dev.breakmode.session.FocusSchedule
import dev.breakmode.session.Interruptions
import dev.breakmode.session.Wallpaper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BreakViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BreakRepository.get(application)

    val settings: StateFlow<BreakSettings> = repository.settings
    val active: StateFlow<ActiveBreak?> = repository.active
    val history: StateFlow<List<BreakRecord>> = repository.history
    val emergencyExitAvailable: StateFlow<Boolean> = repository.emergencyExitAvailable

    private val _apps = MutableStateFlow<List<LaunchableApp>>(emptyList())
    val apps: StateFlow<List<LaunchableApp>> = _apps.asStateFlow()

    private val _appsLoaded = MutableStateFlow(false)
    val appsLoaded: StateFlow<Boolean> = _appsLoaded.asStateFlow()

    private val _picking = MutableStateFlow(false)
    val picking: StateFlow<Boolean> = _picking.asStateFlow()

    private val _editingBucketId = MutableStateFlow<String?>(null)
    val editingBucketId: StateFlow<String?> = _editingBucketId.asStateFlow()

    private val _dndAccess = MutableStateFlow(false)
    val dndAccess: StateFlow<Boolean> = _dndAccess.asStateFlow()

    private val _accessibilityAccess = MutableStateFlow(false)
    val accessibilityAccess: StateFlow<Boolean> = _accessibilityAccess.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    /** Minutes of the break that just finished, surfaced once on the setup screen. */
    private val _justFinishedMinutes = MutableStateFlow<Int?>(null)
    val justFinishedMinutes: StateFlow<Int?> = _justFinishedMinutes.asStateFlow()

    init {
        refreshAccess()
        viewModelScope.launch {
            _apps.value = AppCatalog.load(getApplication())
            _appsLoaded.value = true
        }
    }

    // -------------------------------------------------------------- lifecycle

    /** Called on resume: closes out an expired break and refreshes special-access state. */
    fun sync() {
        repository.syncWithClock()
        refreshAccess()
    }

    fun refreshAccess() {
        val context = getApplication<Application>()
        _dndAccess.value = Interruptions.hasAccess(context)
        _accessibilityAccess.value = FocusAccessibilityService.isServiceEnabled(context)
        _notificationsEnabled.value = Interruptions.notificationsAllowed(context)
    }

    // ------------------------------------------------------------------ break

    fun startBreak() {
        repository.syncWithClock()
        repository.start()
        _justFinishedMinutes.value = null
    }

    fun useEmergencyExit(): Boolean {
        val success = repository.useEmergencyExit()
        if (success) {
            _justFinishedMinutes.value = null
        }
        return success
    }

    fun endBreak(completed: Boolean) {
        val active = repository.active.value ?: return
        if (completed) {
            repository.finish(completed = true)
            _justFinishedMinutes.value = (active.totalMillis / 60_000L).toInt()
        } else {
            repository.useEmergencyExit()
        }
    }

    fun extendBreak(minutes: Int) = repository.extend(minutes)

    fun dismissFinished() {
        _justFinishedMinutes.value = null
    }

    // --------------------------------------------------------------- settings

    fun setMinutes(minutes: Int) =
        repository.updateSettings { it.copy(minutes = minutes.coerceIn(5, 480)) }

    fun toggleApp(packageName: String) = repository.toggleAllowedPackage(packageName)

    fun clearAllowedApps() =
        repository.updateSettings { it.copy(allowedPackages = emptySet()) }

    fun selectBucket(bucketId: String?) = repository.selectBucket(bucketId)

    fun updateBucket(bucketId: String, name: String, packages: Set<String>) =
        repository.updateBucket(bucketId, name, packages)

    fun toggleAppInBucket(bucketId: String, packageName: String) =
        repository.toggleAppInBucket(bucketId, packageName)

    fun addSchedule(schedule: FocusSchedule) = repository.addSchedule(schedule)

    fun updateSchedule(schedule: FocusSchedule) = repository.updateSchedule(schedule)

    fun deleteSchedule(scheduleId: String) = repository.deleteSchedule(scheduleId)

    fun toggleSchedule(scheduleId: String, enabled: Boolean) =
        repository.toggleSchedule(scheduleId, enabled)

    fun setPauseInterruptions(enabled: Boolean) =
        repository.updateSettings { it.copy(pauseInterruptions = enabled) }

    fun setRequireUnlock(enabled: Boolean) =
        repository.updateSettings { it.copy(requireUnlockToEnd = enabled) }

    fun setWallpaper(wallpaper: Wallpaper) =
        repository.updateSettings { it.copy(wallpaper = wallpaper) }

    fun openPicker() {
        _editingBucketId.value = null
        _picking.value = true
    }

    fun openBucketPicker(bucketId: String) {
        _editingBucketId.value = bucketId
        _picking.value = true
    }

    fun closePicker() {
        _editingBucketId.value = null
        _picking.value = false
    }

    fun labelFor(packageName: String): String =
        _apps.value.firstOrNull { it.packageName == packageName }?.label ?: packageName

    fun clearHistory() = repository.clearHistory()
}
