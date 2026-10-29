package dev.breakmode.session

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * Runs the real repository against a Robolectric Android runtime: SharedPreferences, AlarmManager
 * and NotificationManager are all exercised, so a broken persistence path fails here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BreakRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: BreakRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("break_mode_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        BreakRepository.resetForTests()
        repository = BreakRepository.get(context)
    }

    @After
    fun tearDown() {
        BreakRepository.resetForTests()
    }

    @Test
    fun settingsArePersistedAndReloaded() {
        repository.updateSettings {
            it.copy(
                minutes = 45,
                allowedPackages = setOf("com.example.chat", "com.example.maps"),
                requireUnlockToEnd = true,
                wallpaper = Wallpaper.FOREST,
            )
        }

        BreakRepository.resetForTests()
        val reloaded = BreakRepository.get(context)

        assertEquals(45, reloaded.settings.value.minutes)
        assertEquals(setOf("com.example.chat", "com.example.maps"), reloaded.settings.value.allowedPackages)
        assertTrue(reloaded.settings.value.requireUnlockToEnd)
        assertEquals(Wallpaper.FOREST, reloaded.settings.value.wallpaper)
    }

    @Test
    fun startingABreakFreezesItsConfiguration() {
        repository.updateSettings {
            it.copy(minutes = 30, allowedPackages = setOf("com.example.chat"))
        }

        val active = repository.start()

        assertEquals(30L * 60_000L, active.totalMillis)
        assertEquals(setOf("com.example.chat"), active.allowedPackages)
        assertNotNull(repository.active.value)

        // Changing settings mid-break must not change the running break.
        repository.updateSettings { it.copy(allowedPackages = emptySet()) }
        assertEquals(setOf("com.example.chat"), repository.active.value?.allowedPackages)
    }

    @Test
    fun finishingACompletedBreakRecordsThePlannedMinutes() {
        repository.updateSettings { it.copy(minutes = 30) }
        repository.start()

        repository.finish(completed = true)

        val record = repository.history.value.single()
        assertEquals(30, record.minutes)
        assertTrue(record.completed)
        assertNull(repository.active.value)
    }

    @Test
    fun finishingEarlyRecordsAtLeastOneMinute() {
        repository.updateSettings { it.copy(minutes = 45) }
        repository.start()

        repository.finish(completed = false)

        val record = repository.history.value.single()
        assertEquals(1, record.minutes)
        assertTrue(!record.completed)
    }

    @Test
    fun anExpiredBreakIsClosedOnSyncButARunningOneIsNot() {
        repository.updateSettings { it.copy(minutes = 30) }
        repository.start()

        repository.syncWithClock()
        assertNotNull("A running break must survive a resume", repository.active.value)

        // Pretend the break ran out while the app was closed.
        val startedAt = System.currentTimeMillis() - 31 * 60_000L
        repository.finish(completed = true)
        repository.start()
        val active = repository.active.value!!
        forceEndsAt(active.copy(startedAtMillis = startedAt, endsAtMillis = System.currentTimeMillis() - 1000))

        repository.syncWithClock()

        assertNull("An expired break must be closed out", repository.active.value)
        assertEquals(2, repository.history.value.size)
    }

    @Test
    fun aRunningBreakIsRestoredFromDisk() {
        repository.updateSettings { it.copy(minutes = 25, allowedPackages = setOf("com.example.chat")) }
        repository.start()

        // Same as the process being killed and the app reopened mid-break.
        BreakRepository.resetForTests()
        val restored = BreakRepository.get(context)

        val active = restored.active.value
        assertNotNull("A running break must survive a restart", active)
        assertEquals(25L * 60_000L, active!!.totalMillis)
        assertEquals(setOf("com.example.chat"), active.allowedPackages)
    }

    @Test
    fun startingABreakPostsTheCountdownNotificationWhenAllowed() {
        val application = ApplicationProvider.getApplicationContext<android.app.Application>()
        Shadows.shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        repository.start()

        val manager = context.getSystemService(NotificationManager::class.java)
        val notifications = Shadows.shadowOf(manager).allNotifications
        assertEquals("Exactly one ongoing notification", 1, notifications.size)
        val notification = notifications.first()
        assertTrue(
            "Notification must be ongoing",
            (notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0,
        )
        assertTrue(
            "The countdown notification must not offer an End action to prevent bypassing focus",
            notification.actions == null || notification.actions.isEmpty(),
        )
    }

    @Test
    fun emergencyExitAllowedOnlyOncePerDayAndResetsAtMidnight() {
        var testDate = "2026-10-29"
        repository.currentDateProvider = { testDate }
        repository.refreshEmergencyExitStatus()

        assertTrue("Emergency exit must be available initially", repository.isEmergencyExitAvailable())
        assertTrue("StateFlow must reflect availability", repository.emergencyExitAvailable.value)

        // Start a break and use the emergency exit
        repository.start()
        val usedFirstTime = repository.useEmergencyExit()
        assertTrue("First emergency exit must succeed", usedFirstTime)
        assertNull("Break should be finished", repository.active.value)
        assertTrue(!repository.isEmergencyExitAvailable())
        assertTrue(!repository.emergencyExitAvailable.value)

        // Start another break on the same day: emergency exit should be rejected!
        repository.start()
        assertNotNull(repository.active.value)
        val usedSecondTime = repository.useEmergencyExit()
        assertTrue("Second emergency exit on the same day must be rejected", !usedSecondTime)
        assertNotNull("Break must continue running when emergency exit is exhausted", repository.active.value)

        // Midnight arrives: new calendar day!
        testDate = "2026-10-30"
        repository.syncWithClock()

        assertTrue("Emergency exit must reset at midnight", repository.isEmergencyExitAvailable())
        assertTrue("StateFlow must reflect reset after midnight", repository.emergencyExitAvailable.value)

        // Now emergency exit should succeed again
        val usedAfterMidnight = repository.useEmergencyExit()
        assertTrue("Emergency exit must succeed on the new day", usedAfterMidnight)
        assertNull("Break should now be finished", repository.active.value)
    }

    @Test
    fun extendingABreakMovesItsEndTime() {
        repository.start()
        val before = repository.active.value!!.endsAtMillis

        repository.extend(10)

        assertEquals(before + 10 * 60_000L, repository.active.value!!.endsAtMillis)
        // Extending must not be able to shrink a break into the past.
        repository.extend(-30)
        assertTrue(repository.active.value!!.endsAtMillis > before)
    }

    /** Writes a break straight into prefs, emulating state left over from a killed process. */
    private fun forceEndsAt(active: ActiveBreak) {
        context.getSharedPreferences("break_mode_prefs", Context.MODE_PRIVATE).edit()
            .putLong("break_started_at", active.startedAtMillis)
            .putLong("break_ends_at", active.endsAtMillis)
            .putBoolean("break_interruptions_paused", false)
            .commit()
        BreakRepository.resetForTests()
        repository = BreakRepository.get(context)
    }
}
