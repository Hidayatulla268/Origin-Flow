package com.originflow.system.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.originflow.system.overlay.OverlayWindowManager
import com.originflow.system.ui.intentwheel.ContextPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Origin-Flow Spatial Multi-App Context Engine Accessibility Service.
 *
 * Intercepts text selection, long-press gestures, and clipboard changes across any app,
 * extracts spatial bounds and textual semantics, and anchors the radial Intent-Wheel overlay.
 */
class OriginFlowAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "OriginFlowService"
        private const val DEBOUNCE_WINDOW_MS = 350L
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var overlayWindowManager: OverlayWindowManager
    private var clipboardManager: ClipboardManager? = null

    private var lastSelectedText: String? = null
    private var debounceJob: Job? = null

    private val clipChangedListener = ClipboardManager.OnPrimaryClipChangedListener {
        handleClipboardChange()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "OriginFlowAccessibilityService connected.")

        overlayWindowManager = OverlayWindowManager(this, serviceScope)

        // Setup accessibility capabilities programmatically or via XML
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_LONG_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED

            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS

            notificationTimeout = 100
        }

        // Register system clipboard observer
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboardManager?.addPrimaryClipChangedListener(clipChangedListener)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!hasOverlayPermission()) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                handleTextSelectionEvent(event)
            }
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> {
                handleLongClickEvent(event)
            }
        }
    }

    /**
     * Extracts text selection changes and anchors the Intent Wheel at the selection bounds.
     */
    private fun handleTextSelectionEvent(event: AccessibilityEvent) {
        val rawText = extractTextFromEvent(event)
        if (rawText.isNullOrBlank() || rawText.length < 2) return

        // Prevent repeated triggers for the exact same active selection
        if (rawText == lastSelectedText) return

        debounceJob?.cancel()
        debounceJob = serviceScope.launch {
            delay(DEBOUNCE_WINDOW_MS)

            val sourceNode = event.source ?: return@launch
            val bounds = Rect()
            sourceNode.getBoundsInScreen(bounds)

            // Calculate anchor coordinate centered over selected node
            val anchorX = if (bounds.width() > 0) bounds.centerX() else getFallbackCoordinates().first
            val anchorY = if (bounds.height() > 0) bounds.top else getFallbackCoordinates().second

            lastSelectedText = rawText

            val payload = ContextPayload(
                text = rawText,
                packageName = event.packageName?.toString()
            )

            overlayWindowManager.showIntentWheel(
                anchorX = anchorX,
                anchorY = anchorY,
                payload = payload
            )
        }
    }

    /**
     * Handles long press on UI elements across applications.
     */
    private fun handleLongClickEvent(event: AccessibilityEvent) {
        val sourceNode = event.source ?: return
        val nodeText = sourceNode.text?.toString() ?: sourceNode.contentDescription?.toString()

        if (nodeText.isNullOrBlank()) return

        val bounds = Rect()
        sourceNode.getBoundsInScreen(bounds)

        val anchorX = bounds.centerX()
        val anchorY = bounds.centerY()

        val payload = ContextPayload(
            text = nodeText,
            packageName = event.packageName?.toString()
        )

        overlayWindowManager.showIntentWheel(
            anchorX = anchorX,
            anchorY = anchorY,
            payload = payload
        )
    }

    /**
     * Intercepts new system clipboard items to offer spatial actions.
     */
    private fun handleClipboardChange() {
        if (!hasOverlayPermission()) return

        val clip = clipboardManager?.primaryClip ?: return
        if (clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString()
            if (!text.isNullOrBlank()) {
                val (fallbackX, fallbackY) = getFallbackCoordinates()
                overlayWindowManager.showIntentWheel(
                    anchorX = fallbackX,
                    anchorY = fallbackY,
                    payload = ContextPayload(
                        text = text,
                        packageName = "System Clipboard"
                    )
                )
            }
        }
    }

    private fun extractTextFromEvent(event: AccessibilityEvent): String? {
        val textList = event.text
        if (!textList.isNullOrEmpty()) {
            val fullText = textList.joinToString(" ")
            val fromIndex = event.fromIndex
            val toIndex = event.toIndex

            if (fromIndex in 0..toIndex && toIndex <= fullText.length && fromIndex != toIndex) {
                return fullText.substring(fromIndex, toIndex).trim()
            }
            return fullText.trim()
        }

        // Fallback to source node text inspection
        val source = event.source ?: return null
        return source.text?.toString()?.trim()
    }

    private fun getFallbackCoordinates(): Pair<Int, Int> {
        val metrics = resources.displayMetrics
        return Pair(metrics.widthPixels / 2, metrics.heightPixels / 2)
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "OriginFlowAccessibilityService interrupted.")
        overlayWindowManager.dismissIntentWheel()
    }

    override fun onDestroy() {
        super.onDestroy()
        clipboardManager?.removePrimaryClipChangedListener(clipChangedListener)
        overlayWindowManager.destroy()
        serviceScope.cancel()
        Log.i(TAG, "OriginFlowAccessibilityService destroyed.")
    }
}
