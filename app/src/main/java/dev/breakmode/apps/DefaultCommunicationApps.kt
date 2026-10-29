package dev.breakmode.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import android.telecom.TelecomManager

/**
 * Resolves the phone and messaging apps on the device so they remain permanently
 * available regardless of the selected focus bucket.
 */
object DefaultCommunicationApps {

    fun getPhonePackage(context: Context): String? {
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val defaultDialer = telecom?.defaultDialerPackage
        if (!defaultDialer.isNullOrBlank()) return defaultDialer

        val dialIntent = Intent(Intent.ACTION_DIAL)
        val resolve = context.packageManager.resolveActivity(dialIntent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolve?.activityInfo?.packageName
    }

    fun getMessagesPackage(context: Context): String? {
        val defaultSms = Telephony.Sms.getDefaultSmsPackage(context)
        if (!defaultSms.isNullOrBlank()) return defaultSms

        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))
        val resolve = context.packageManager.resolveActivity(smsIntent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolve?.activityInfo?.packageName
    }

    fun openPhone(context: Context) {
        val pkg = getPhonePackage(context)
        val intent = (if (pkg != null) context.packageManager.getLaunchIntentForPackage(pkg) else null)
            ?: Intent(Intent.ACTION_DIAL)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        runCatching { context.startActivity(intent) }
    }

    fun openMessages(context: Context) {
        val pkg = getMessagesPackage(context)
        val intent = (if (pkg != null) context.packageManager.getLaunchIntentForPackage(pkg) else null)
            ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        runCatching { context.startActivity(intent) }
    }
}
