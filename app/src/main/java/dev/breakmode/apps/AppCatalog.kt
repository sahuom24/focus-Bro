package dev.breakmode.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/** An app the user can keep available during a break. */
data class LaunchableApp(
    val packageName: String,
    val label: String,
)

/**
 * Reads the apps that have a launcher entry.
 *
 * Visibility comes from the narrow `<queries>` intent in the manifest (MAIN/LAUNCHER), not from
 * QUERY_ALL_PACKAGES. Only the package name and label are read, only in memory, and they are
 * written to private storage at most — never sent anywhere, because the app cannot open a socket.
 */
object AppCatalog {

    suspend fun load(context: Context): List<LaunchableApp> = withContext(Dispatchers.IO) {
        val manager = context.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        runCatching {
            manager.queryIntentActivities(query, 0)
                .asSequence()
                .mapNotNull { it.activityInfo?.packageName }
                .filter { it != context.packageName }
                .distinct()
                .mapNotNull { packageName -> readApp(manager, packageName) }
                .sortedBy { it.label.lowercase(Locale.getDefault()) }
                .toList()
        }.getOrDefault(emptyList())
    }

    /** Resolves the launch intent for a package, or null when the app cannot be opened. */
    fun launchIntent(context: Context, packageName: String): Intent? =
        context.packageManager.getLaunchIntentForPackage(packageName)

    private fun readApp(manager: PackageManager, packageName: String): LaunchableApp? =
        runCatching {
            val info = manager.getApplicationInfo(packageName, 0)
            LaunchableApp(
                packageName = packageName,
                label = manager.getApplicationLabel(info).toString(),
            )
        }.getOrNull()
}
