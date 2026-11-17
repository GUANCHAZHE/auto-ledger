package com.cooper.autoledger.capture

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import com.cooper.autoledger.data.LedgerRepository
import java.util.concurrent.Executors

class PaymentNotificationService : NotificationListenerService() {

    private val ioExecutor = Executors.newSingleThreadExecutor()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val statusBarNotification = sbn ?: return
        val pkg = statusBarNotification.packageName?.trim().orEmpty()
        if (pkg.isEmpty() || pkg == packageName || pkg == OWN_PACKAGE) {
            return
        }

        val repo = LedgerRepository.getInstance(applicationContext)
        if (!repo.autoCaptureEnabled.value) {
            return
        }

        val notification = statusBarNotification.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()

        val content = when {
            bigText.isNotEmpty() && text.isNotEmpty() && !bigText.contains(text) -> "$text\n$bigText"
            bigText.isNotEmpty() -> bigText
            else -> text
        }
        if (title.isEmpty() && content.isEmpty()) {
            return
        }

        val parsed = ScreenBillParser.parseNotification(pkg, title, content) ?: return

        val appContext = applicationContext
        ioExecutor.execute {
            try {
                val saved = LedgerRepository.getInstance(appContext).insertFromParsedIfNotDuplicate(parsed)
                if (saved != null) {
                    CaptureNotifier.notifyBillCaptured(appContext, saved)
                }
            } catch (_: Exception) {
                // Guard notification listener worker against unexpected DB errors.
            }
        }
    }

    override fun onDestroy() {
        ioExecutor.shutdownNow()
        super.onDestroy()
    }

    companion object {
        private const val OWN_PACKAGE = "com.cooper.autoledger"
        private const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"

        fun isNotificationAccessEnabled(context: Context): Boolean {
            val flatListeners = Settings.Secure.getString(
                context.contentResolver,
                ENABLED_NOTIFICATION_LISTENERS
            ) ?: return false

            val expectedComponent = ComponentName(context, PaymentNotificationService::class.java)
            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(flatListeners)
            while (splitter.hasNext()) {
                val entry = splitter.next()
                val component = ComponentName.unflattenFromString(entry)
                if (component == expectedComponent) {
                    return true
                }
            }
            return false
        }
    }
}
