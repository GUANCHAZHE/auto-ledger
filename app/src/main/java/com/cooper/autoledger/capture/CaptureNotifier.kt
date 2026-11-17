package com.cooper.autoledger.capture

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.cooper.autoledger.model.BillRecord
import com.cooper.autoledger.model.CurrencyUtils
import com.cooper.autoledger.model.TransactionType

object CaptureNotifier {

    private const val CHANNEL_ID = "auto_ledger_capture_channel"
    private const val CHANNEL_NAME = "自动记账提醒"
    private const val CHANNEL_DESC = "屏幕或通知自动识别到账单时弹出确认与一键撤销提醒"
    private const val NOTIFICATION_ID_BASE = 20_000

    fun notificationIdFor(billId: Long): Int {
        return NOTIFICATION_ID_BASE + ((billId and 0x7FFF_FFFFL).toInt() % 50_000)
    }

    fun formatCaptureBannerText(record: BillRecord): String {
        val sign = if (record.type == TransactionType.INCOME) "+" else "-"
        val formattedAmount = CurrencyUtils.formatYuan(record.amountCents)
        return "自动记账成功：${sign}¥${formattedAmount} · ${record.category.displayName} (${record.merchant})"
    }

    fun notifyBillCaptured(context: Context, record: BillRecord) {
        val appContext = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        ensureChannel(nm)

        val notificationId = notificationIdFor(record.id)
        val bannerTitle = formatCaptureBannerText(record)
        val detailBody = "${record.channel.displayName}自动捕获 · 点击进入账本或点击下方撤销"

        val launchIntent = appContext.packageManager
            .getLaunchIntentForPackage(appContext.packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        val contentPendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                appContext,
                notificationId,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val undoIntent = Intent(appContext, UndoCaptureReceiver::class.java).apply {
            action = UndoCaptureReceiver.ACTION_UNDO_BILL
            putExtra(UndoCaptureReceiver.EXTRA_BILL_ID, record.id)
            putExtra(UndoCaptureReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val undoPendingIntent = PendingIntent.getBroadcast(
            appContext,
            notificationId,
            undoIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smallIconRes = appContext.applicationInfo.icon.takeIf { it != 0 }
            ?: android.R.drawable.ic_dialog_info

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(smallIconRes)
            .setContentTitle(bannerTitle)
            .setContentText(detailBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$bannerTitle\n$detailBody"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .addAction(0, "撤销该笔", undoPendingIntent)

        if (contentPendingIntent != null) {
            builder.setContentIntent(contentPendingIntent)
        }

        try {
            nm.notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Ignore if notification permission was revoked concurrently.
        }
    }

    private fun ensureChannel(nm: NotificationManager) {
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESC
            enableVibration(true)
        }
        nm.createNotificationChannel(channel)
    }
}
