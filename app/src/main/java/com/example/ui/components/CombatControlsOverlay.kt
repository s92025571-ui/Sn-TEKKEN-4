package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.CombatButtonInput
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.ArenaSurfaceElevated
import com.example.ui.theme.BuffaloSteelCyan
import com.example.ui.theme.DragonMagmaRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPure
import com.example.ui.theme.TigerFlameOrange
import com.example.ui.theme.TigerGold
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CombatControlsOverlay(
    controlScale: Float,
    specialMeter: Float,
    specialCooldown: Float,
    playerFacing: Float,
    onMoveAxisChanged: (Float) -> Unit,
    onBlockHeldChanged: (Boolean) -> Unit,
    onActionPressed: (CombatButtonInput) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeScale = controlScale.coerceIn(0.8f, 1.25f)
    val joystickSize = (138f * safeScale).coerceAtLeast(112f).dp
    val actionBtnSize = (70f * safeScale).coerceAtLeast(56f).dp
    val specialBtnSize = (76f * safeScale).coerceAtLeast(60f).dp

    Box(modifier = modifier.fillMaxSize()) {
        // LEFT SIDE: Responsive Thumb Virtual Joystick + Quick Step Evade Trigger
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 14.dp)
        ) {
            VirtualThumbJoystick(
                size = joystickSize,
                playerFacing = playerFacing,
                onMoveAxisChanged = onMoveAxisChanged,
                onQuickFlickDodge = { forward ->
                    onActionPressed(
                        if (forward) CombatButtonInput.DODGE_FORWARD else CombatButtonInput.DODGE_BACK
                    )
                }
            )
            Spacer(modifier = Modifier.height(6.dp))
            // Subtle thumb-accessible quick-dodge pill right under stick for effortless evasion
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickStepChip(
                    label = "BACKSTEP",
                    testTag = "btn_dodge_back",
                    onTap = { onActionPressed(CombatButtonInput.DODGE_BACK) }
                )
                QuickStepChip(
                    label = "DASH IN",
                    testTag = "btn_dodge_forward",
                    onTap = { onActionPressed(CombatButtonInput.DODGE_FORWARD) }
                )
            }
        }

        // RIGHT SIDE: Four Clearly Separated 3D-Styled Arcade Action Buttons
        // Diamond ergonomic thumb layout:
        //         [SPECIAL]
        //   [PUNCH]       [KICK]
        //         [BLOCK]
        val spacing = (14f * safeScale).dp
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = 12.dp)
                .width(actionBtnSize * 2.65f + spacing)
                .height(actionBtnSize * 2.55f + spacing)
        ) {
            val specialReady = specialMeter >= 50f && specialCooldown <= 0f
            val specialProgress = (specialMeter / 50f).coerceIn(0f, 1f)

            // Top: SPECIAL
            ArcadeCombatButton(
                label = "SPECIAL",
                icon = Icons.Filled.AutoAwesome,
                accentColor = if (specialReady) TigerGold else TextMuted,
                size = specialBtnSize,
                enabledVisual = specialReady,
                ringProgress = if (specialCooldown > 0f) {
                    (1f - specialCooldown / 4.0f).coerceIn(0f, 1f)
                } else {
                    specialProgress
                },
                cooldownSec = specialCooldown,
                testTag = "btn_special",
                onPressDown = { onActionPressed(CombatButtonInput.SPECIAL) },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Left: PUNCH (Quick Strike)
            ArcadeCombatButton(
                label = "PUNCH",
                icon = Icons.Filled.SportsMma,
                accentColor = TigerFlameOrange,
                size = actionBtnSize,
                enabledVisual = true,
                ringProgress = 1f,
                cooldownSec = 0f,
                testTag = "btn_punch",
                onPressDown = { onActionPressed(CombatButtonInput.PUNCH) },
                modifier = Modifier.align(Alignment.CenterStart)
            )

            // Right: KICK (Heavy Leg Attack)
            ArcadeCombatButton(
                label = "KICK",
                icon = Icons.Filled.Bolt,
                accentColor = DragonMagmaRed,
                size = actionBtnSize,
                enabledVisual = true,
                ringProgress = 1f,
                cooldownSec = 0f,
                testTag = "btn_kick",
                onPressDown = { onActionPressed(CombatButtonInput.KICK) },
                modifier = Modifier.align(Alignment.CenterEnd)
            )

            // Bottom: BLOCK (Defensive Guard — hold to guard)
            ArcadeCombatButton(
                label = "BLOCK",
                icon = Icons.Filled.Shield,
                accentColor = BuffaloSteelCyan,
                size = actionBtnSize,
                enabledVisual = true,
                ringProgress = 1f,
                cooldownSec = 0f,
                testTag = "btn_block",
                onPressDown = { onBlockHeldChanged(true) },
                onReleaseUp = { onBlockHeldChanged(false) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun VirtualThumbJoystick(
    size: Dp,
    playerFacing: Float,
    onMoveAxisChanged: (Float) -> Unit,
    onQuickFlickDodge: (Boolean) -> Unit
) {
    val density = LocalDensity.current
    val maxRadiusPx = with(density) { (size.toPx() * 0.34f).coerceAtLeast(24f) }
    var knobOffsetX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var lastTapDir by remember { mutableFloatStateOf(0f) }
    var lastTapTimeMs by remember { mutableStateOf(0L) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        ArenaSurfaceElevated.copy(alpha = 0.78f),
                        ArenaObsidian.copy(alpha = 0.88f)
                    )
                )
            )
            .border(
                width = 2.dp,
                color = if (isDragging) TigerFlameOrange.copy(alpha = 0.85f) else BuffaloSteelCyan.copy(alpha = 0.42f),
                shape = CircleShape
            )
            .semantics {
                contentDescription = "Movement Joystick"
            }
            .pointerInput(maxRadiusPx, playerFacing) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val centerX = this.size.width / 2f
                    isDragging = true

                    fun updateFromX(px: Float): Float {
                        val dx = (px - centerX).coerceIn(-maxRadiusPx, maxRadiusPx)
                        knobOffsetX = dx
                        val norm = (dx / maxRadiusPx).coerceIn(-1f, 1f)
                        onMoveAxisChanged(norm)
                        return norm
                    }

                    val initialNorm = updateFromX(down.position.x)
                    val now = System.currentTimeMillis()
                    if (abs(initialNorm) > 0.55f) {
                        val dirSign = if (initialNorm > 0f) 1f else -1f
                        if (dirSign == lastTapDir && (now - lastTapTimeMs) in 40L..260L) {
                            onQuickFlickDodge((dirSign * playerFacing) > 0f)
                        }
                        lastTapDir = dirSign
                        lastTapTimeMs = now
                    }

                    var pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId }
                            ?: event.changes.firstOrNull()
                        if (change == null || !change.pressed) {
                            break
                        }
                        pointerId = change.id
                        updateFromX(change.position.x)
                        change.consume()
                    }

                    isDragging = false
                    knobOffsetX = 0f
                    onMoveAxisChanged(0f)
                }
            }
            .testTag("joystick_control")
    ) {
        // Directional chevrons & ring ticks
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
        ) {
            Text(
                text = "◀",
                color = if (knobOffsetX < -8f) TigerFlameOrange else TextMuted.copy(alpha = 0.65f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "▶",
                color = if (knobOffsetX > 8f) TigerFlameOrange else TextMuted.copy(alpha = 0.65f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 3D Metallic Thumb Puck
        val knobSize = size * 0.44f
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset { IntOffset(knobOffsetX.roundToInt(), 0) }
                .size(knobSize)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = if (isDragging) {
                            listOf(TigerFlameOrange, Color(0xFFB54108))
                        } else {
                            listOf( Color(0xFF3B4866), Color(0xFF1B2336))
                        }
                    )
                )
                .border(2.dp, TextPure.copy(alpha = 0.45f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.SwapHoriz,
                contentDescription = null,
                tint = TextPure,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun QuickStepChip(
    label: String,
    testTag: String,
    onTap: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (pressed) BuffaloSteelCyan.copy(alpha = 0.35f)
                else ArenaObsidian.copy(alpha = 0.78f)
            )
            .border(
                width = 1.dp,
                color = BuffaloSteelCyan.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp)
            .semantics {
                role = Role.Button
                contentDescription = label
                onClick {
                    onTap()
                    true
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    onTap()
                    while (true) {
                        val ev = awaitPointerEvent()
                        if (ev.changes.none { it.pressed }) break
                    }
                    pressed = false
                }
            }
            .testTag(testTag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextPure
        )
    }
}

@Composable
private fun ArcadeCombatButton(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    size: Dp,
    enabledVisual: Boolean,
    ringProgress: Float,
    cooldownSec: Float,
    testTag: String,
    onPressDown: () -> Unit,
    onReleaseUp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 600f),
        label = "btn_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .scale(scale)
            .semantics {
                role = Role.Button
                contentDescription = label
                onClick {
                    onPressDown()
                    onReleaseUp()
                    true
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    onPressDown()
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.none { it.pressed }) break
                        event.changes.forEach { it.consume() }
                    }
                    isPressed = false
                    onReleaseUp()
                }
            }
            .testTag(testTag)
    ) {
        // Outer radial glow & progress ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 3.5.dp.toPx()
            val radius = (this.size.minDimension - stroke) / 2f
            drawCircle(
                brush = Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(accentColor.copy(alpha = 0.85f), ArenaObsidian.copy(alpha = 0.95f))
                    } else {
                        listOf(
                            ArenaSurfaceElevated.copy(alpha = 0.92f),
                            ArenaObsidian.copy(alpha = 0.95f)
                        )
                    }
                ),
                radius = radius
            )
            // Background track
            drawCircle(
                color = accentColor.copy(alpha = 0.22f),
                radius = radius,
                style = Stroke(width = stroke)
            )
            // Charge / Ready Arc
            drawArc(
                color = if (enabledVisual) accentColor else accentColor.copy(alpha = 0.45f),
                startAngle = -90f,
                sweepAngle = 360f * ringProgress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = Offset(stroke / 2f, stroke / 2f),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabledVisual) TextPure else TextMuted,
                modifier = Modifier.size(size * 0.34f)
            )
            Text(
                text = if (cooldownSec > 0.1f) "${cooldownSec.roundToInt()}s" else label,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                color = if (enabledVisual) TextPure else TextMuted,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
