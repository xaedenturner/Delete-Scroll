package com.xaedenturner.deletescroll

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class ReelsBlockerService : AccessibilityService() {

    companion object {
        private const val TAG = "ReelsBlocker"
        private const val INSTAGRAM_PACKAGE = "com.instagram.android"
        private const val BLOCK_COOLDOWN_MS = 1500L
    }

    private val detector = ReelsDetector()
    private var lastBlockTime = 0L
    private var lastResult: ReelsDetector.DetectionResult? = null

    override fun onServiceConnected() {
        Log.i(TAG, "━━━ Service connected ━━━")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.packageName?.toString() != INSTAGRAM_PACKAGE) return

        val root = rootInActiveWindow ?: return
        val eventType = event.eventType
        val eventName = eventTypeName(eventType)

        try {
            val result = detector.detect(root)

            val scoreChanged = result.score != lastResult?.score
            val tabStateChanged = result.reelsTabSelected != lastResult?.reelsTabSelected
            val stateChanged = scoreChanged || tabStateChanged

            // Logging is gated on state change — only emit when something shifts
            if (stateChanged) {
                Log.i(TAG, "[$eventName] ${result.describe()}")
                if (lastResult?.triggered == true && !result.triggered) {
                    Log.i(TAG, "↩ Reels exited — monitoring resumed")
                }
                lastResult = result
            }

            // Block evaluation is NOT gated on state change.
            // Without this, returning to the same Reels state after a back action
            // is invisible to the service — the bug that caused the 32-second free window.
            val primaryTrigger = result.triggered && result.reelsTabSelected

            // Secondary path: fullscreen reel opened from home feed or chat DM.
            // Filtered to WINDOW_STATE_CHANGED only — this event fires when a new
            // view layer opens on top of the current one, which is what Instagram
            // does when you tap a reel outside the Reels tab. This correctly
            // excludes background-mounted reel content, which emits
            // WINDOW_CONTENT_CHANGED instead.
            val secondaryTrigger = eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && !result.reelsTabSelected
                    && result.reelReferenceCount >= 2

            when {
                primaryTrigger   -> attemptBlock(eventName, "reels_tab")
                secondaryTrigger -> attemptBlock(eventName, "fullscreen_reel")
            }

        } finally {
            root.recycle()
        }
    }

    private fun attemptBlock(triggeringEvent: String, reason: String) {
        val now = System.currentTimeMillis()
        if ((now - lastBlockTime) < BLOCK_COOLDOWN_MS) {
            Log.d(TAG, "⏱ Block suppressed — cooldown active ($reason)")
            return
        }

        Log.w(TAG, "🚫 BLOCKING [$reason] — triggered by $triggeringEvent")
        lastBlockTime = now
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun eventTypeName(eventType: Int): String = when (eventType) {
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED   -> "WINDOW_STATE_CHANGED"
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "WINDOW_CONTENT_CHANGED"
        AccessibilityEvent.TYPE_VIEW_SCROLLED          -> "VIEW_SCROLLED"
        AccessibilityEvent.TYPE_VIEW_CLICKED           -> "VIEW_CLICKED"
        AccessibilityEvent.TYPE_VIEW_FOCUSED           -> "VIEW_FOCUSED"
        AccessibilityEvent.TYPE_ANNOUNCEMENT           -> "ANNOUNCEMENT"
        else -> "EVENT_$eventType"
    }

    override fun onInterrupt() {
        Log.i(TAG, "━━━ Service interrupted ━━━")
    }

    override fun onDestroy() {
        Log.i(TAG, "━━━ Service destroyed ━━━")
    }
}