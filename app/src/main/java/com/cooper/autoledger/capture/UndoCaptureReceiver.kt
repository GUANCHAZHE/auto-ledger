package com.cooper.autoledger.capture

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cooper.autoledger.data.LedgerRepository

class UndoCaptureReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_UNDO_BILL) return
        val billId = intent.getLongExtra(EXTRA_BILL_ID, -1L)
        if (billId <= 0L) return

        val appContext = context.applicationContext
        val notificationId = intent.getIntExtra(
            EXTRA_NOTIFICATION_ID,
            CaptureNotifier.notificationIdFor(billId)
        )

        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(notificationId)

        val pendingResult = goAsync()
        Thread {
            try {
                LedgerRepository.getInstance(appContext).deleteRecord(billId)
            } finally {
                pendingResult?.finish()
            }
        }.start()
    }

    companion object {
        const val ACTION_UNDO_BILL = "com.cooper.autoledger.ACTION_UNDO_BILL"
        const val EXTRA_BILL_ID = "extra_bill_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
