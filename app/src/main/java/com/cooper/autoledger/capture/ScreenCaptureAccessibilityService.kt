package com.cooper.autoledger.capture

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.cooper.autoledger.data.LedgerRepository
import java.util.concurrent.Executors

class ScreenCaptureAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val ioExecutor = Executors.newSingleThreadExecutor()

    private var lastScanElapsedMs: Long = 0L
    private var isTrailingScanScheduled: Boolean = false
    private var pendingPackageName: String = ""

    private var lastParsedKey: String = ""
    private var lastParsedElapsedMs: Long = 0L

    private val trailingScanRunnable = Runnable {
        isTrailingScanScheduled = false
        val pkg = pendingPackageName
        lastScanElapsedMs = SystemClock.elapsedRealtime()
        inspectActiveWindow(pkg)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val ev = event ?: return
        val eventType = ev.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        val eventPkg = ev.packageName?.toString()?.trim().orEmpty()
        if (shouldSkipPackage(eventPkg)) {
            return
        }

        val repo = LedgerRepository.getInstance(applicationContext)
        if (!repo.autoCaptureEnabled.value) {
            return
        }

        val now = SystemClock.elapsedRealtime()
        val elapsedSinceLast = now - lastScanElapsedMs
        pendingPackageName = eventPkg

        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || elapsedSinceLast >= DEBOUNCE_MS) {
            lastScanElapsedMs = now
            inspectActiveWindow(eventPkg)
            scheduleTrailingFollowUp(DEBOUNCE_MS)
        } else if (!isTrailingScanScheduled) {
            val remaining = (DEBOUNCE_MS - elapsedSinceLast).coerceIn(120L, DEBOUNCE_MS)
            scheduleTrailingFollowUp(remaining)
        }
    }

    private fun scheduleTrailingFollowUp(delayMs: Long) {
        mainHandler.removeCallbacks(trailingScanRunnable)
        isTrailingScanScheduled = true
        mainHandler.postDelayed(trailingScanRunnable, delayMs)
    }

    private fun inspectActiveWindow(fallbackPkg: String) {
        val repo = LedgerRepository.getInstance(applicationContext)
        if (!repo.autoCaptureEnabled.value) return

        val rootNode = try {
            rootInActiveWindow
        } catch (_: Exception) {
            null
        } ?: return

        val activePkg = try {
            rootNode.packageName?.toString()?.trim().orEmpty().ifEmpty { fallbackPkg }
        } catch (_: Exception) {
            fallbackPkg
        }

        if (activePkg.isEmpty() || shouldSkipPackage(activePkg)) {
            return
        }

        val collectedTexts = ArrayList<String>(64)
        collectVisibleNodeTexts(rootNode, collectedTexts, depth = 0)
        if (collectedTexts.isEmpty()) return

        val parsed = ScreenBillParser.parseScreenNodes(activePkg, collectedTexts) ?: return

        val now = SystemClock.elapsedRealtime()
        val quickKey = "$activePkg|${parsed.type}|${parsed.amountCents}|${parsed.merchant}"
        if (quickKey == lastParsedKey && now - lastParsedElapsedMs < QUICK_CACHE_MS) {
            return
        }
        lastParsedKey = quickKey
        lastParsedElapsedMs = now

        val appContext = applicationContext
        ioExecutor.execute {
            try {
                val saved = LedgerRepository.getInstance(appContext).insertFromParsedIfNotDuplicate(parsed)
                if (saved != null) {
                    CaptureNotifier.notifyBillCaptured(appContext, saved)
                }
            } catch (_: Exception) {
                // Guard background capture thread against unexpected DB/IPC errors.
            }
        }
    }

    private fun collectVisibleNodeTexts(
        node: AccessibilityNodeInfo?,
        outTexts: MutableList<String>,
        depth: Int
    ) {
        if (node == null || depth > MAX_TRAVERSAL_DEPTH || outTexts.size >= MAX_COLLECTED_NODES) {
            return
        }

        try {
            if (!node.isVisibleToUser) return

            val text = node.text?.toString()?.trim().orEmpty()
            val desc = node.contentDescription?.toString()?.trim().orEmpty()

            if (text.isNotEmpty()) {
                outTexts.add(text)
            }
            if (desc.isNotEmpty() && desc != text && outTexts.size < MAX_COLLECTED_NODES) {
                outTexts.add(desc)
            }

            val childCount = node.childCount
            for (i in 0 until childCount) {
                if (outTexts.size >= MAX_COLLECTED_NODES) break
                val child = node.getChild(i) ?: continue
                collectVisibleNodeTexts(child, outTexts, depth + 1)
            }
        } catch (_: Exception) {
            // AccessibilityNodeInfo IPC calls can fail if the remote window closes mid-traversal.
        }
    }

    private fun shouldSkipPackage(pkg: String): Boolean {
        if (pkg.isEmpty()) return false
        if (pkg == packageName || pkg == OWN_PACKAGE) return true
        return pkg in IGNORED_SYSTEM_PACKAGES
    }

    override fun onInterrupt() {
        mainHandler.removeCallbacksAndMessages(null)
        isTrailingScanScheduled = false
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        isTrailingScanScheduled = false
        ioExecutor.shutdownNow()
        super.onDestroy()
    }

    companion object {
        private const val OWN_PACKAGE = "com.cooper.autoledger"
        private const val DEBOUNCE_MS = 500L
        private const val QUICK_CACHE_MS = 4_000L
        private const val MAX_TRAVERSAL_DEPTH = 40
        private const val MAX_COLLECTED_NODES = 400

        private val IGNORED_SYSTEM_PACKAGES = setOf(
            OWN_PACKAGE,
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.inputmethod.latin",
            "com.baidu.input",
            "com.sohu.inputmethod.sogou",
            "com.tencent.wetype",
            "com.iflytek.inputmethod"
        )

        fun isServiceEnabled(context: Context): Boolean {
            val accessibilityEnabled = try {
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED,
                    0
                )
            } catch (_: Exception) {
                0
            }
            if (accessibilityEnabled != 1) return false

            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val expectedComponent = ComponentName(context, ScreenCaptureAccessibilityService::class.java)
            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(enabledServices)
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
