package com.originflow.system.overlay

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.originflow.system.ui.intentwheel.ContextPayload
import com.originflow.system.ui.intentwheel.IntentWheelAction
import com.originflow.system.ui.intentwheel.IntentWheelOverlay
import com.originflow.system.ui.workspace.FloatingWorkspaceCard
import com.originflow.system.ui.workspace.WorkspaceItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages WindowManager-level Compose views for Origin-Flow:
 * 1. IntentWheelOverlay: A transient radial overlay at specific (x, y) coordinates.
 * 2. FloatingWorkspaceCard: A persistent, draggable, and collapsible stack window.
 */
class OverlayWindowManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    // In-memory stack of captured context items
    val workspaceItems = mutableStateListOf<WorkspaceItem>()

    // Overlay View & Lifecycle references
    private var intentWheelView: View? = null
    private var intentWheelLifecycleOwner: OverlayLifecycleOwner? = null

    private var workspaceView: View? = null
    private var workspaceLifecycleOwner: OverlayLifecycleOwner? = null
    private var workspaceLayoutParams: WindowManager.LayoutParams? = null

    /**
     * Spawns the Intent-Wheel at screen coordinate (anchorX, anchorY).
     */
    fun showIntentWheel(
        anchorX: Int,
        anchorY: Int,
        payload: ContextPayload
    ) {
        // Dismiss any existing wheel first
        dismissIntentWheel()

        val lifecycleOwner = OverlayLifecycleOwner()
        lifecycleOwner.onCreate()
        lifecycleOwner.onStart()
        lifecycleOwner.onResume()
        intentWheelLifecycleOwner = lifecycleOwner

        val overlayWidthPx = (340 * context.resources.displayMetrics.density).toInt()
        val overlayHeightPx = (340 * context.resources.displayMetrics.density).toInt()

        // Calculate top-left placement centered over target coordinate
        val screenX = (anchorX - (overlayWidthPx / 2)).coerceAtLeast(0)
        val screenY = (anchorY - (overlayHeightPx / 2)).coerceAtLeast(0)

        val params = WindowManager.LayoutParams(
            overlayWidthPx,
            overlayHeightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenX
            y = screenY
        }

        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            lifecycleOwner.attachToView(this)
            setContent {
                IntentWheelOverlay(
                    payload = payload,
                    onActionSelected = { action, selectedPayload ->
                        handleWheelAction(action, selectedPayload)
                        dismissIntentWheel()
                    },
                    onDismissRequest = {
                        dismissIntentWheel()
                    }
                )
            }
        }

        intentWheelView = composeView
        try {
            windowManager.addView(composeView, params)
        } catch (e: Exception) {
            e.printStackTrace()
            dismissIntentWheel()
        }
    }

    /**
     * Dismisses and unbinds the Intent-Wheel.
     */
    fun dismissIntentWheel() {
        intentWheelView?.let { view ->
            try {
                windowManager.removeViewImmediate(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        intentWheelView = null

        intentWheelLifecycleOwner?.apply {
            onPause()
            onStop()
            onDestroy()
        }
        intentWheelLifecycleOwner = null
    }

    /**
     * Spawns or updates the Floating Workspace Stack overlay.
     */
    fun showFloatingWorkspace() {
        if (workspaceView != null) return // Already active

        val lifecycleOwner = OverlayLifecycleOwner()
        lifecycleOwner.onCreate()
        lifecycleOwner.onStart()
        lifecycleOwner.onResume()
        workspaceLifecycleOwner = lifecycleOwner

        val displayMetrics = context.resources.displayMetrics
        val initialX = (displayMetrics.widthPixels - (100 * displayMetrics.density)).toInt()
        val initialY = (displayMetrics.heightPixels * 0.35f).toInt()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }
        workspaceLayoutParams = params

        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            lifecycleOwner.attachToView(this)
            setContent {
                FloatingWorkspaceCard(
                    items = workspaceItems,
                    onDragDelta = { dx, dy ->
                        updateWorkspacePosition(dx, dy)
                    },
                    onCopyText = { text ->
                        copyToClipboard("Origin-Flow Context", text)
                    },
                    onClearAll = {
                        workspaceItems.clear()
                    },
                    onRemoveItem = { item ->
                        workspaceItems.remove(item)
                    },
                    onDismiss = {
                        dismissFloatingWorkspace()
                    }
                )
            }
        }

        workspaceView = composeView
        try {
            windowManager.addView(composeView, params)
        } catch (e: Exception) {
            e.printStackTrace()
            dismissFloatingWorkspace()
        }
    }

    /**
     * Drags the floating workspace smoothly across the display.
     */
    private fun updateWorkspacePosition(deltaX: Float, deltaY: Float) {
        val view = workspaceView ?: return
        val params = workspaceLayoutParams ?: return

        params.x = (params.x + deltaX.toInt())
        params.y = (params.y + deltaY.toInt())

        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismissFloatingWorkspace() {
        workspaceView?.let { view ->
            try {
                windowManager.removeViewImmediate(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        workspaceView = null

        workspaceLifecycleOwner?.apply {
            onPause()
            onStop()
            onDestroy()
        }
        workspaceLifecycleOwner = null
    }

    /**
     * Dispatches spatial context processing for radial actions.
     */
    private fun handleWheelAction(action: IntentWheelAction, payload: ContextPayload) {
        showFloatingWorkspace() // Ensure workspace is visible to display or buffer result

        coroutineScope.launch {
            // Simulated AI/Context Engine processing
            val result = when (action) {
                IntentWheelAction.SUMMARIZE -> {
                    "📌 Summary: \"${payload.text.take(120)}...\" [Key insights distilled]"
                }
                IntentWheelAction.AUTO_SCHEDULE -> {
                    "📅 Event parsed: Context scheduled for review today at 6:00 PM."
                }
                IntentWheelAction.SOLVE_EXTRACT -> {
                    "⚡ Solved/Extracted: Identified key parameters & code references from context."
                }
                IntentWheelAction.SAVE_TO_STACK -> {
                    payload.text
                }
            }

            workspaceItems.add(
                0,
                WorkspaceItem(
                    sourcePackage = payload.packageName,
                    originalText = payload.text,
                    processedResult = result,
                    actionType = action.title
                )
            )

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "${action.title} pinned to Workspace", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val clip = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun destroy() {
        dismissIntentWheel()
        dismissFloatingWorkspace()
    }
}
