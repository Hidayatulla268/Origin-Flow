package com.originflow.system.ui.workspace

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GlassSurface = Color(0xEE1E1E1E)
private val GlassBorder = Color(0x33FFFFFF)
private val AccentCyan = Color(0xFF00E5FF)
private val AccentPurple = Color(0xFFB388FF)
private val TextPrimary = Color(0xFFF5F5F5)
private val TextSecondary = Color(0xFFA0A0A0)

/**
 * Movable, Collapsible Floating Workspace Stack.
 *
 * Supports two distinct states:
 * 1. Bubble (Collapsed): A compact floating puck indicating pending/buffered context items.
 * 2. Card (Expanded): A rich multi-app context card displaying AI-processed output,
 *    original sources, and productivity quick-actions.
 */
@Composable
fun FloatingWorkspaceCard(
    items: List<WorkspaceItem>,
    onDragDelta: (dx: Float, dy: Float) -> Unit,
    onCopyText: (String) -> Unit,
    onClearAll: () -> Unit,
    onRemoveItem: (WorkspaceItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = Color.Black)
            .clip(RoundedCornerShape(24.dp))
            .background(GlassSurface)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color(0x55FFFFFF), Color(0x10FFFFFF))
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) togetherWith
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
            },
            label = "ExpandCollapseAnimation"
        ) { expanded ->
            if (expanded) {
                ExpandedWorkspaceContent(
                    items = items,
                    onCollapse = { isExpanded = false },
                    onDragDelta = onDragDelta,
                    onCopyText = onCopyText,
                    onClearAll = onClearAll,
                    onRemoveItem = onRemoveItem,
                    onDismiss = onDismiss
                )
            } else {
                CollapsedBubbleContent(
                    itemCount = items.size,
                    onExpand = { isExpanded = true },
                    onDragDelta = onDragDelta
                )
            }
        }
    }
}

/**
 * Collapsed Floating Bubble / Puck view.
 */
@Composable
private fun CollapsedBubbleContent(
    itemCount: Int,
    onExpand: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragDelta(dragAmount.x, dragAmount.y)
                }
            }
            .clickable(onClick = onExpand),
        contentAlignment = Alignment.Center
    ) {
        BadgedBox(
            badge = {
                if (itemCount > 0) {
                    Badge(
                        containerColor = AccentCyan,
                        contentColor = Color.Black,
                        modifier = Modifier.size(18.dp)
                    ) {
                        Text(
                            text = itemCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        ) {
            Icon(
                imageVector = Icons.Rounded.Layers,
                contentDescription = "Expand Origin-Flow Stack",
                tint = AccentCyan,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

/**
 * Expanded multi-card context inspection view.
 */
@Composable
private fun ExpandedWorkspaceContent(
    items: List<WorkspaceItem>,
    onCollapse: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit,
    onCopyText: (String) -> Unit,
    onClearAll: () -> Unit,
    onRemoveItem: (WorkspaceItem) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(320.dp)
            .padding(14.dp)
    ) {
        // Top Drag Handle & Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount.x, dragAmount.y)
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.DragIndicator,
                    contentDescription = "Drag Handle",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Origin Workspace",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Minimize Button
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.UnfoldLess,
                        contentDescription = "Collapse",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Close Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close Overlay",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        HorizontalDivider(
            color = GlassBorder,
            thickness = 0.8.dp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Empty state vs active list
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No context items pinned yet.\nSelect text anywhere to trigger.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        } else {
            // Scrollable list of items
            Column(
                modifier = Modifier
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items.forEach { item ->
                    WorkspaceItemCard(
                        item = item,
                        onCopy = { onCopyText(item.processedResult.ifBlank { item.originalText }) },
                        onDelete = { onRemoveItem(item) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onClearAll,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Clear All Stack",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkspaceItemCard(
    item: WorkspaceItem,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x40121212))
            .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Column {
            // Action chip & package badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.actionType.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan
                    )
                }

                Row {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Delete item",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Processed Content / Summary
            Text(
                text = item.processedResult.ifBlank { item.originalText },
                color = TextPrimary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )

            if (item.sourcePackage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "via ${item.sourcePackage}",
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Preview(name = "Floating Workspace - Expanded", showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
fun FloatingWorkspaceExpandedPreview() {
    val sampleItems = listOf(
        WorkspaceItem(
            sourcePackage = "com.slack",
            originalText = "Ship the release candidate build by 5 PM EST today.",
            processedResult = "📌 Summary: Critical RC build delivery scheduled today before 5:00 PM EST.",
            actionType = "Summarize"
        ),
        WorkspaceItem(
            sourcePackage = "com.google.android.gm",
            originalText = "Flight AI-802 departs at 08:30 tomorrow from Gate B12.",
            processedResult = "📅 Auto-Schedule: Added flight to calendar for tomorrow 08:30 AM (Gate B12).",
            actionType = "Auto-Schedule"
        )
    )

    Box(
        modifier = Modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        FloatingWorkspaceCard(
            items = sampleItems,
            onDragDelta = { _, _ -> },
            onCopyText = {},
            onClearAll = {},
            onRemoveItem = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "Floating Workspace - Collapsed Bubble", showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
fun FloatingWorkspaceCollapsedPreview() {
    Box(
        modifier = Modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        FloatingWorkspaceCard(
            items = listOf(
                WorkspaceItem(
                    sourcePackage = "com.twitter.android",
                    originalText = "Quick note to buffer",
                    processedResult = "Pinned to buffer",
                    actionType = "Save"
                )
            ),
            onDragDelta = { _, _ -> },
            onCopyText = {},
            onClearAll = {},
            onRemoveItem = {},
            onDismiss = {}
        )
    }
}
