package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.FighterId
import com.example.model.GameSettings
import com.example.model.GraphicsQuality
import com.example.ui.theme.ArenaBorderSteel
import com.example.ui.theme.ArenaDeepSlate
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.ArenaSurface
import com.example.ui.theme.BuffaloSteelCyan
import com.example.ui.theme.DragonMagmaRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPure
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TigerFlameOrange
import com.example.ui.theme.TigerGold
import kotlin.math.roundToInt

@Composable
fun FighterRosterOverlay(
    selectedFighter: FighterId,
    onSelectFighter: (FighterId) -> Unit,
    onRotatePreview: (Float) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    // Strictly enforce the 2 playable fighters in the roster!
    val roster = FighterId.playableRoster

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        ArenaObsidian.copy(alpha = 0.90f),
                        ArenaObsidian.copy(alpha = 0.35f),
                        Color.Transparent
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("fighter_roster_screen")
    ) {
        // Right Half Interactive 360° Touch Rotation Zone over the Live 3D Warrior!
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxWidth(0.55f)
                .fillMaxHeight()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        onRotatePreview(dragAmount * 0.55f)
                    }
                }
                .testTag("roster_3d_drag_zone")
        ) {
            // Rotation Controls Pill at Bottom Right
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ArenaDeepSlate.copy(alpha = 0.88f))
                    .border(1.dp, ArenaBorderSteel, RoundedCornerShape(24.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                IconButton(
                    onClick = { onRotatePreview(-35f) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_rotate_left")
                ) {
                    Icon(
                        imageVector = Icons.Filled.RotateLeft,
                        contentDescription = "Rotate fighter left",
                        tint = TigerGold
                    )
                }
                Text(
                    text = "DRAG TO ROTATE 3D MODEL",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                IconButton(
                    onClick = { onRotatePreview(35f) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_rotate_right")
                ) {
                    Icon(
                        imageVector = Icons.Filled.RotateRight,
                        contentDescription = "Rotate fighter right",
                        tint = TigerGold
                    )
                }
            }
        }

        // Left Panel: Playable Roster Selector (Strictly 2 Fighters) + Detailed Martial Profile
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .widthIn(max = 390.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ArenaSurface)
                        .testTag("roster_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Return to Main Menu",
                        tint = TextPure
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.roster_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = TigerGold
                    )
                    Text(
                        text = stringResource(R.string.roster_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Two Playable Fighter Cards
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                roster.forEach { fighter ->
                    val isSelected = fighter == selectedFighter
                    val accent = if (fighter == FighterId.SHADOW_TIGER) TigerFlameOrange else BuffaloSteelCyan
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) accent.copy(alpha = 0.22f) else ArenaDeepSlate.copy(alpha = 0.88f)
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) accent else ArenaBorderSteel,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectFighter(fighter) }
                            .padding(8.dp)
                            .testTag("roster_card_${fighter.name.lowercase()}")
                    ) {
                        Image(
                            painter = painterResource(id = fighter.portraitRes),
                            contentDescription = "${fighter.displayName} portrait",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = fighter.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPure,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Detailed Fighter Bio & Combat Attributes Card
            val accentColor = if (selectedFighter == FighterId.SHADOW_TIGER) TigerFlameOrange else BuffaloSteelCyan
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ArenaDeepSlate.copy(alpha = 0.92f))
                    .border(1.5.dp, accentColor.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = selectedFighter.displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPure
                )
                Text(
                    text = "${selectedFighter.title} • ${selectedFighter.styleName}",
                    style = MaterialTheme.typography.titleMedium,
                    color = accentColor
                )
                Text(
                    text = selectedFighter.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Text(
                    text = "SIGNATURE SPECIAL: ${selectedFighter.specialMoveName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TigerGold
                )

                StatMeterRow(label = "STRIKE POWER", value = selectedFighter.powerStat, color = TigerFlameOrange)
                StatMeterRow(label = "COMBAT SPEED", value = selectedFighter.speedStat, color = TigerGold)
                StatMeterRow(label = "ARMOR DEFENSE", value = selectedFighter.defenseStat, color = BuffaloSteelCyan)
            }
        }
    }
}

@Composable
private fun StatMeterRow(label: String, value: Int, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Text(text = "$value", style = MaterialTheme.typography.labelMedium, color = TextPure)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(ArenaSurface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((value / 100f).coerceIn(0f, 1f))
                    .background(color)
            )
        }
    }
}

@Composable
fun SettingsOverlay(
    settings: GameSettings,
    onUpdateSettings: (GameSettings) -> Unit,
    onSelectGraphics: (GraphicsQuality) -> Unit,
    onReturnToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onReturnToMenu)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(ArenaObsidian.copy(alpha = 0.90f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .testTag("settings_screen")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(ArenaDeepSlate.copy(alpha = 0.96f))
                .border(1.5.dp, ArenaBorderSteel, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TigerGold
                )
                Button(
                    onClick = onReturnToMenu,
                    colors = ButtonDefaults.buttonColors(containerColor = TigerFlameOrange),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("return_menu_button")
                ) {
                    Text(
                        text = stringResource(R.string.btn_return_menu),
                        style = MaterialTheme.typography.labelLarge,
                        color = ArenaObsidian
                    )
                }
            }

            // Audio Toggles Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingsToggleTile(
                    title = stringResource(R.string.settings_sfx),
                    checked = settings.sfxEnabled,
                    onCheckedChange = { onUpdateSettings(settings.copy(sfxEnabled = it)) },
                    testTag = "sfx_switch",
                    modifier = Modifier.weight(1f)
                )
                SettingsToggleTile(
                    title = stringResource(R.string.settings_music),
                    checked = settings.musicEnabled,
                    onCheckedChange = { onUpdateSettings(settings.copy(musicEnabled = it)) },
                    testTag = "music_switch",
                    modifier = Modifier.weight(1f)
                )
            }

            // Graphics Quality Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.settings_graphics),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPure
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GraphicsQuality.entries.forEach { q ->
                        val active = settings.graphicsQuality == q
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) BuffaloSteelCyan.copy(alpha = 0.24f) else ArenaSurface)
                                .border(
                                    width = if (active) 2.dp else 1.dp,
                                    color = if (active) BuffaloSteelCyan else ArenaBorderSteel,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onSelectGraphics(q) }
                                .testTag("graphics_option_${q.name.lowercase()}")
                        ) {
                            Text(
                                text = q.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) TextPure else TextSecondary
                            )
                        }
                    }
                }
            }

            // Control Size & Camera Sensitivity Sliders
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${stringResource(R.string.settings_control_size)}: ${(settings.controlScale * 100).roundToInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPure
                    )
                    Slider(
                        value = settings.controlScale,
                        onValueChange = { onUpdateSettings(settings.copy(controlScale = it)) },
                        valueRange = 0.80f..1.25f,
                        colors = SliderDefaults.colors(thumbColor = TigerFlameOrange, activeTrackColor = TigerFlameOrange),
                        modifier = Modifier.testTag("control_scale_slider")
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${stringResource(R.string.settings_camera)}: ${(settings.cameraSensitivity * 100).roundToInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPure
                    )
                    Slider(
                        value = settings.cameraSensitivity,
                        onValueChange = { onUpdateSettings(settings.copy(cameraSensitivity = it)) },
                        valueRange = 0.60f..1.40f,
                        colors = SliderDefaults.colors(thumbColor = BuffaloSteelCyan, activeTrackColor = BuffaloSteelCyan),
                        modifier = Modifier.testTag("camera_sensitivity_slider")
                    )
                }
            }

            // Developer Testing Stage Unlock Option (Per Requirement 8)
            SettingsToggleTile(
                title = "${stringResource(R.string.settings_dev_unlock)} (Unlock All 3 Fights for Testing)",
                checked = settings.devUnlockAllStages,
                onCheckedChange = { onUpdateSettings(settings.copy(devUnlockAllStages = it)) },
                testTag = "dev_unlock_switch",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SettingsToggleTile(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ArenaSurface)
            .border(1.dp, ArenaBorderSteel, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPure,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ArenaObsidian,
                checkedTrackColor = TigerFlameOrange
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
