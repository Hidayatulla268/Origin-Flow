package com.originflow.system.ui.intentwheel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.ShortText
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Spec Glassmorphic Dark Colors
private val GlassBackground = Color(0xDD1E1E1E)
private val GlassBorder = Color(0x33FFFFFF)
private val AccentCyan = Color(0xFF00E5FF)
private val TextPrimary = Color(0xFFF0F0F0)
private val TextSecondary = Color(0xFFAAAAAA)

/**
 * Radial "Intent-Wheel" Quick-Action Overlay.
 *
 * Renders 4 actions spaced radially around a center point with smooth
 * physics-based entrance transitions, glassmorphic styling, and touch feedback.
 */
@Composable
fun IntentWheelOverlay(
    payload: ContextPayload,
    onActionSelected: (IntentWheelAction, ContextPayload) -> Unit,
    onDismissRequest: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Animation state drivers
    val scaleAnim = remember { Animatable(0.2f) }
    val alphaAnim = remember { Animatable(0f) }
    val rotationAnim = remember { Animatable(-35f) }
    val radialExpansion = remember { Animatable(0f) } // 0 to 1 progress

    LaunchedEffect(Unit) {
        // Run entrance animations concurrently
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
        }
        launch {
            rotationAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            radialExpansion.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.75f,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    // Dismiss with reverse exit animation
    val performDismiss = {
        coroutineScope.launch {
            alphaAnim.animateTo(0f, tween(150))
            scaleAnim.animateTo(0.3f, tween(150))
            onDismissRequest()
        }
    }

    // Outer full-screen scrim for tap-outside-to-dismiss
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                performDismiss()
            },
        contentAlignment = Alignment.Center
    ) {
        // Main wheel container
        Box(
            modifier = Modifier
                .size(320.dp)
                .alpha(alphaAnim.value)
                .scale(scaleAnim.value)
                .rotate(rotationAnim.value),
            contentAlignment = Alignment.Center
        ) {
            // Radial Orbit Background Rings (Visual spatial cues)
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color(0x15FFFFFF), CircleShape)
                    .background(Color(0x08FFFFFF))
            )

            // 4 Radial Action Nodes
            val orbitRadiusPx = 100.dp.value * radialExpansion.value

            IntentWheelAction.values().forEach { action ->
                val angleRad = Math.toRadians(action.angleDegrees.toDouble())
                val offsetX = (orbitRadiusPx * cos(angleRad)).roundToInt()
                val offsetY = (orbitRadiusPx * sin(angleRad)).roundToInt()

                RadialActionButton(
                    action = action,
                    modifier = Modifier.offset { IntOffset(offsetX, offsetY) },
                    onClick = {
                        coroutineScope.launch {
                            scaleAnim.animateTo(1.15f, tween(80))
                            scaleAnim.animateTo(0f, tween(120))
                            onActionSelected(action, payload)
                        }
                    }
                )
            }

            // Central Anchor Node (Close / Status)
            CentralAnchor(
                onClick = { performDismiss() }
            )
        }
    }
}

@Composable
private fun CentralAnchor(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .shadow(12.dp, CircleShape, spotColor = AccentCyan)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2A2A2A),
                        GlassBackground
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(AccentCyan.copy(alpha = 0.8f), GlassBorder)
                ),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Dismiss Intent Wheel",
            tint = AccentCyan,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun RadialActionButton(
    action: IntentWheelAction,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val icon = when (action) {
        IntentWheelAction.SUMMARIZE -> Icons.Rounded.ShortText
        IntentWheelAction.AUTO_SCHEDULE -> Icons.Rounded.CalendarMonth
        IntentWheelAction.SOLVE_EXTRACT -> Icons.Rounded.Psychology
        IntentWheelAction.SAVE_TO_STACK -> Icons.Rounded.Layers
    }

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon Circle with Glassmorphism
        Box(
            modifier = Modifier
                .size(62.dp)
                .shadow(elevation = 10.dp, shape = CircleShape, spotColor = Color.Black)
                .clip(CircleShape)
                .background(GlassBackground)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x55FFFFFF), Color(0x15FFFFFF))
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = action.title,
                tint = TextPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Action Label Chip
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xCC121212))
                .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = action.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
fun IntentWheelOverlayPreview() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        IntentWheelOverlay(
            payload = ContextPayload(
                text = "Meeting with product team at 4 PM to discuss multi-app context routing.",
                packageName = "com.google.android.gm"
            ),
            onActionSelected = { _, _ -> },
            onDismissRequest = {}
        )
    }
}
