package com.originflow.system.ui.intentwheel

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Supported radial actions in the Intent-Wheel.
 */
enum class IntentWheelAction(
    val title: String,
    val description: String,
    val angleDegrees: Float // Angle in polar coordinates (0 = Right, 90 = Down, 180 = Left, 270 = Up)
) {
    SUMMARIZE(
        title = "Summarize",
        description = "Condense selected context",
        angleDegrees = 270f // Top
    ),
    AUTO_SCHEDULE(
        title = "Auto-Schedule",
        description = "Detect time/events & create agenda",
        angleDegrees = 0f // Right
    ),
    SOLVE_EXTRACT(
        title = "Solve/Extract",
        description = "Parse equations, code, or structured entities",
        angleDegrees = 90f // Bottom
    ),
    SAVE_TO_STACK(
        title = "Save to Stack",
        description = "Pin to floating workspace buffer",
        angleDegrees = 180f // Left
    )
}

data class ContextPayload(
    val text: String,
    val packageName: String?,
    val timestamp: Long = System.currentTimeMillis()
)
