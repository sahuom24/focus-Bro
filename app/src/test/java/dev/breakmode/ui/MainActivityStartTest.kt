package dev.breakmode.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dev.breakmode.session.BreakRepository
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Boots the real MainActivity: theme, ViewModel, repository, app catalog query and Compose content
 * all run, so a crash on cold start (or a broken resource/manifest reference) fails here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class MainActivityStartTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    /** The repository the running Activity actually uses. */
    private val repository: BreakRepository
        get() = BreakRepository.get(compose.activity)

    @After
    fun tearDown() {
        // Leave no break behind for the next test in this JVM, then drop the cached singleton so
        // the next Activity binds to its own (fresh) application context.
        repository.finish(completed = false)
        BreakRepository.resetForTests()
    }

    @Test
    fun coldStartShowsTheSetupScreen() {
        compose.onNodeWithText("FOCUSBRO").assertIsDisplayed()
        compose.onNodeWithText("Phone & Messages always available").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Start focusing  →").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun startingABreakOpensTheSessionScreen() {
        repository.start()
        compose.waitForIdle()

        // Bounded wait: a hang here would mean the session screen never becomes idle.
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("FOCUSING").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithText("FOCUSING").assertIsDisplayed()
        compose.onNodeWithText("Life 1").assertIsDisplayed()
        assertTrue("The break should be running after Start", repository.active.value != null)
    }
}
