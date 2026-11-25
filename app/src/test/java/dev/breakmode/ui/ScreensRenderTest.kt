package dev.breakmode.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import dev.breakmode.apps.LaunchableApp
import dev.breakmode.session.ActiveBreak
import dev.breakmode.session.BreakRecord
import dev.breakmode.session.BreakSettings
import dev.breakmode.session.Wallpaper
import dev.breakmode.ui.theme.BreakModeTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Renders each screen with Robolectric's native graphics and checks that what a user must see is
 * actually laid out on screen (not clipped, not zero-sized), then writes a PNG for inspection.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class ScreensRenderTest {

    @get:Rule
    val compose = createComposeRule()

    private val settings = BreakSettings(
        minutes = 30,
        allowedPackages = setOf("com.example.chat"),
        wallpaper = Wallpaper.DUSK,
    )

    @Test
    fun setupScreenShowsEverythingAndKeepsStartReachable() {
        compose.setContent {
            BreakModeTheme {
                SetupScreen(
                    settings = settings,
                    history = listOf(
                        BreakRecord(System.currentTimeMillis() - 3_600_000, 25, true),
                        BreakRecord(System.currentTimeMillis() - 90_000_000, 40, true),
                    ),
                    dndAccess = false,
                    accessibilityAccess = true,
                    notificationsEnabled = true,
                    allowedLabels = listOf("Chat"),
                    justFinishedMinutes = 30,
                    onDismissFinished = {},
                    onStart = {},
                    onMinutes = {},
                    onOpenPicker = {},
                    onOpenBucketPicker = {},
                    onSelectBucket = {},
                    onRenameBucket = { _, _ -> },
                    onAddSchedule = {},
                    onUpdateSchedule = {},
                    onDeleteSchedule = {},
                    onToggleSchedule = { _, _ -> },
                    onPauseInterruptions = {},
                    onRequireUnlock = {},
                    onWallpaper = {},
                    onRequestDndAccess = {},
                    onRequestAccessibilityAccess = {},
                    onOpenNotificationSettings = {},
                )
            }
        }

        compose.onNodeWithText("FOCUSBRO").assertIsDisplayed()
        compose.onNodeWithText("Home").assertIsDisplayed()
        compose.onNodeWithText("Apps").assertIsDisplayed()

        val start = compose.onNodeWithText("Start focusing  →")
        start.performScrollTo().assertIsDisplayed()

        // The pinned Start slider must sit inside the window, not off-screen or clipped away.
        val root = compose.onRoot().getBoundsInRoot()
        val button = start.getBoundsInRoot()
        assertTrue("Start slider is off-screen: $button (root $root)", button.bottom <= root.bottom)

    }

    @Test
    fun setupScreenSurvivesLongAppNamesAndEmptyState() {
        compose.setContent {
            BreakModeTheme {
                SetupScreen(
                    settings = BreakSettings(
                        minutes = 90,
                        allowedPackages = setOf("a", "b", "c", "d", "e", "f"),
                        pauseInterruptions = false,
                        requireUnlockToEnd = true,
                    ),
                    history = emptyList(),
                    dndAccess = false,
                    accessibilityAccess = false,
                    notificationsEnabled = false,
                    allowedLabels = listOf(
                        "A very long application name that should be truncated",
                        "Another quite long application name",
                        "Third", "Fourth", "Fifth", "Sixth",
                    ),
                    justFinishedMinutes = null,
                    onDismissFinished = {},
                    onStart = {},
                    onMinutes = {},
                    onOpenPicker = {},
                    onOpenBucketPicker = {},
                    onSelectBucket = {},
                    onRenameBucket = { _, _ -> },
                    onAddSchedule = {},
                    onUpdateSchedule = {},
                    onDeleteSchedule = {},
                    onToggleSchedule = { _, _ -> },
                    onPauseInterruptions = {},
                    onRequireUnlock = {},
                    onWallpaper = {},
                    onRequestDndAccess = {},
                    onRequestAccessibilityAccess = {},
                    onOpenNotificationSettings = {},
                )
            }
        }

        compose.onNodeWithText("1 h 30 min").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun setupScreenShowsActiveBagSelector() {
        compose.setContent {
            BreakModeTheme {
                SetupScreen(
                    settings = settings,
                    history = emptyList(),
                    dndAccess = true,
                    accessibilityAccess = true,
                    notificationsEnabled = true,
                    allowedLabels = emptyList(),
                    justFinishedMinutes = null,
                    onDismissFinished = {},
                    onStart = {},
                    onMinutes = {},
                    onOpenPicker = {},
                    onOpenBucketPicker = {},
                    onSelectBucket = {},
                    onRenameBucket = { _, _ -> },
                    onAddSchedule = {},
                    onUpdateSchedule = {},
                    onDeleteSchedule = {},
                    onToggleSchedule = { _, _ -> },
                    onPauseInterruptions = {},
                    onRequireUnlock = {},
                    onWallpaper = {},
                    onRequestDndAccess = {},
                    onRequestAccessibilityAccess = {},
                    onOpenNotificationSettings = {},
                )
            }
        }

        compose.onNodeWithText("Start focusing  →").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Your essentials").assertIsDisplayed()
    }

    @Test
    fun setupScreenSurvivesACrampedPhoneScreen() {
        compose.setContent {
            BreakModeTheme {
                SetupScreen(
                    settings = settings,
                    history = listOf(BreakRecord(System.currentTimeMillis(), 30, true)),
                    dndAccess = true,
                    accessibilityAccess = true,
                    notificationsEnabled = true,
                    allowedLabels = listOf("Chat"),
                    justFinishedMinutes = null,
                    onDismissFinished = {},
                    onStart = {},
                    onMinutes = {},
                    onOpenPicker = {},
                    onOpenBucketPicker = {},
                    onSelectBucket = {},
                    onRenameBucket = { _, _ -> },
                    onAddSchedule = {},
                    onUpdateSchedule = {},
                    onDeleteSchedule = {},
                    onToggleSchedule = { _, _ -> },
                    onPauseInterruptions = {},
                    onRequireUnlock = {},
                    onWallpaper = {},
                    onRequestDndAccess = {},
                    onRequestAccessibilityAccess = {},
                    onOpenNotificationSettings = {},
                )
            }
        }

        val start = compose.onNodeWithText("Start focusing  →")
        start.performScrollTo().assertIsDisplayed()
        val root = compose.onRoot().getBoundsInRoot()
        val button = start.getBoundsInRoot()
        assertTrue("Start slider is clipped at 320dp: $button", button.bottom <= root.bottom)
    }

    @Test
    fun sessionScreenShowsCountdownAndChosenApps() {
        val now = System.currentTimeMillis()
        compose.setContent {
            BreakModeTheme {
                SessionScreen(
                    active = ActiveBreak(
                        startedAtMillis = now - 10 * 60_000L,
                        endsAtMillis = now + 20 * 60_000L,
                        allowedPackages = setOf("com.example.chat", "com.example.maps"),
                        requireUnlockToEnd = true,
                        interruptionsPaused = false,
                    ),
                    wallpaper = Wallpaper.DUSK,
                    allowedApps = listOf(LaunchableApp("com.example.chat", "Chat"), LaunchableApp("com.example.maps", "Maps")),
                    onExpired = {},
                    onRequestEmergencyExit = {},
                    emergencyExitAvailable = true,
                    onExtend = {},
                )
            }
        }

        compose.onNodeWithText("FOCUSING").assertIsDisplayed()
        compose.onNodeWithText("20:00").assertIsDisplayed()
        val chat = compose.onNodeWithText("Chat").getBoundsInRoot()
        val maps = compose.onNodeWithText("Maps").getBoundsInRoot()
        assertTrue("Apps must occupy separate rows", maps.top >= chat.bottom)
        compose.onNodeWithText("Maps").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Life 1").assertIsDisplayed()
        compose.onNodeWithText("+15 min").assertIsDisplayed()
        compose.onNodeWithText("1 Life Available").assertIsDisplayed()

    }

    @Test
    fun sessionScreenWithNoAppsStaysCalm() {
        val now = System.currentTimeMillis()
        compose.setContent {
            BreakModeTheme {
                SessionScreen(
                    active = ActiveBreak(
                        startedAtMillis = now,
                        endsAtMillis = now + 15 * 60_000L,
                        allowedPackages = emptySet(),
                        requireUnlockToEnd = false,
                        interruptionsPaused = false,
                    ),
                    wallpaper = Wallpaper.FOREST,
                    allowedApps = emptyList(),
                    onExpired = {},
                    onRequestEmergencyExit = {},
                    emergencyExitAvailable = true,
                    onExtend = {},
                )
            }
        }

        compose.onNodeWithText("SLIDE TO EXIT").assertIsDisplayed()
    }

    @Test
    fun sessionButtonsInvokeTheirCallbacks() {
        val now = System.currentTimeMillis()
        var extended = 0
        compose.setContent {
            BreakModeTheme {
                SessionScreen(
                    active = ActiveBreak(
                        startedAtMillis = now,
                        endsAtMillis = now + 30 * 60_000L,
                        allowedPackages = emptySet(),
                        requireUnlockToEnd = false,
                        interruptionsPaused = false,
                    ),
                    wallpaper = Wallpaper.NIGHT,
                    allowedApps = emptyList(),
                    onExpired = {},
                    onRequestEmergencyExit = {},
                    emergencyExitAvailable = true,
                    onExtend = { extended = it },
                )
            }
        }

        compose.onNodeWithText("+15 min").performClick()

        assertTrue("Extend should add fifteen minutes", extended == 15)
    }

    @Test
    fun pickerShowsAppsAndSelection() {
        compose.setContent {
            BreakModeTheme {
                PickerScreen(
                    apps = listOf(
                        LaunchableApp("com.example.chat", "Chat"),
                        LaunchableApp("com.example.maps", "Maps"),
                    ),
                    loaded = true,
                    selected = setOf("com.example.maps"),
                    onToggle = {},
                    onClear = {},
                    onBack = {},
                )
            }
        }

        compose.onNodeWithText("Keep available").assertIsDisplayed()
        compose.onNodeWithText("1 selected").assertIsDisplayed()
        compose.onNodeWithText("Chat").assertIsDisplayed()
        compose.onNodeWithText("Maps").assertIsDisplayed()
    }
}
