package dev.breakmode.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.breakmode.session.BreakRepository
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GlassPreviewTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun capture(name: String) {
        compose.waitForIdle()
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val file = File("build/glass-preview/$name.png")
            file.parentFile.mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun gallery() {
        capture("Home")
        for (page in listOf("Apps", "Schedule", "Settings")) {
            compose.onNodeWithText(page).performClick()
            capture(page)
        }
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                val now = androidx.compose.runtime.remember { System.currentTimeMillis() }
                val apps = listOf("Calendar", "Maps", "Music").map {
                    dev.breakmode.apps.LaunchableApp("preview." + it.lowercase(), it)
                }
                dev.breakmode.ui.theme.BreakModeTheme {
                    SessionScreen(dev.breakmode.session.ActiveBreak(now, now + 1800000L,
                        apps.map { it.packageName }.toSet(), false, false),
                        dev.breakmode.session.Wallpaper.AMOLED, apps, {}, {}, true, {})
                }
            }
        }
        capture("Focus")
    }
}

