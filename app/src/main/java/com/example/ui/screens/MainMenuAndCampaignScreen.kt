package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CampaignStage
import com.example.model.FighterId
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

@Composable
fun MainMenuOverlay(
    soundOn: Boolean,
    campaignCompleted: Boolean,
    unlockedStage: Int,
    onPlayCampaign: () -> Unit,
    onOpenRoster: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleQuickSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            // Subtle asymmetric left-side dark vignette so 3D Shadow Tiger & Iron Buffalo shine on center-right!
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        ArenaObsidian.copy(alpha = 0.88f),
                        ArenaObsidian.copy(alpha = 0.45f),
                        Color.Transparent
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .testTag("main_menu_screen")
    ) {
        // Top-Right Quick Sound Control & Champion Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            if (campaignCompleted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ArenaDeepSlate.copy(alpha = 0.88f))
                        .border(1.dp, TigerGold, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = TigerGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "ARENA CHAMPION",
                        style = MaterialTheme.typography.labelMedium,
                        color = TigerGold
                    )
                }
            }

            IconButton(
                onClick = onToggleQuickSound,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ArenaDeepSlate.copy(alpha = 0.88f))
                    .border(
                        width = 1.5.dp,
                        color = if (soundOn) BuffaloSteelCyan else ArenaBorderSteel,
                        shape = CircleShape
                    )
                    .testTag("sound_toggle_button")
            ) {
                Icon(
                    imageVector = if (soundOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = stringResource(R.string.cd_sound_toggle),
                    tint = if (soundOn) BuffaloSteelCyan else TextMuted
                )
            }
        }

        // Left Column: Bold SN TEKKEN Logo + Clean Primary Navigation Buttons
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .widthIn(max = 350.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Accent Tag
            Text(
                text = "UNDERGROUND 3D TOURNAMENT",
                style = MaterialTheme.typography.labelMedium,
                color = BuffaloSteelCyan
            )
            Text(
                text = "SN TEKKEN",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 42.sp),
                color = TextPure
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(TigerFlameOrange, DragonMagmaRed)
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 3.dp)
            ) {
                Text(
                    text = stringResource(R.string.game_subtitle),
                    style = MaterialTheme.typography.labelLarge,
                    color = ArenaObsidian
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Prominent PLAY CAMPAIGN Button
            Button(
                onClick = onPlayCampaign,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TigerFlameOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("play_campaign_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = ArenaObsidian,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(R.string.btn_play_campaign),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
                        color = ArenaObsidian
                    )
                    Text(
                        text = "STAGE $unlockedStage OF 3 UNLOCKED",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                        color = ArenaObsidian.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // FIGHTER ROSTER Button (Shows strictly the 2 playable warriors)
            OutlinedButton(
                onClick = onOpenRoster,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(ArenaDeepSlate.copy(alpha = 0.72f), RoundedCornerShape(14.dp))
                    .testTag("fighter_roster_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Groups,
                    contentDescription = null,
                    tint = TigerGold
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.btn_fighter_roster),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPure
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // SETTINGS Button
            OutlinedButton(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(ArenaDeepSlate.copy(alpha = 0.72f), RoundedCornerShape(14.dp))
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    tint = BuffaloSteelCyan
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.btn_settings),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPure
                )
            }
        }

        // Bottom-Right Live 3D Stage Showcase Caption
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .clip(RoundedCornerShape(12.dp))
                .background(ArenaObsidian.copy(alpha = 0.76f))
                .border(1.dp, ArenaBorderSteel, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = "SHADOW TIGER",
                style = MaterialTheme.typography.labelMedium,
                color = TigerFlameOrange
            )
            Text(
                text = "VS",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
            )
            Text(
                text = "IRON BUFFALO",
                style = MaterialTheme.typography.labelMedium,
                color = BuffaloSteelCyan
            )
        }
    }
}

@Composable
fun CampaignSelectOverlay(
    selectedStage: CampaignStage,
    effectiveUnlockedStage: Int,
    fight3ChosenFighter: FighterId,
    onSelectStage: (CampaignStage) -> Unit,
    onSelectFight3Fighter: (FighterId) -> Unit,
    onStartStage: (CampaignStage) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ArenaObsidian.copy(alpha = 0.88f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("campaign_select_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ArenaSurface)
                        .testTag("campaign_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Main Menu",
                        tint = TextPure
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "ARCADE CAMPAIGN",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TigerGold
                    )
                    Text(
                        text = "Three Sequential Battles • Defeat Both Rivals to Challenge the Dragon Boss",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3 Stage Cards + Selected Stage Launch Panel
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Left: 3 Sequential Fights List
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    CampaignStage.entries.forEach { stage ->
                        val isUnlocked = stage.stageNumber <= effectiveUnlockedStage
                        val isSelected = stage == selectedStage
                        val borderColor = when {
                            isSelected -> TigerFlameOrange
                            isUnlocked -> ArenaBorderSteel
                            else -> ArenaBorderSteel.copy(alpha = 0.35f)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) ArenaSurface else ArenaDeepSlate.copy(alpha = 0.85f)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = borderColor,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable(enabled = isUnlocked) { onSelectStage(stage) }
                                .padding(12.dp)
                                .testTag("campaign_stage_card_${stage.stageNumber}")
                        ) {
                            Image(
                                painter = painterResource(id = stage.opponent.portraitRes),
                                contentDescription = "${stage.opponent.displayName} thumbnail",
                                contentScale = ContentScale.Crop,
                                alpha = if (isUnlocked) 1f else 0.35f,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stage.title,
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                                    color = if (isUnlocked) TextPure else TextMuted
                                )
                                Text(
                                    text = stage.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isUnlocked) TigerGold else TextMuted
                                )
                            }
                            if (!isUnlocked) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = "Locked fight",
                                    tint = TextMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Right: Selected Fight Details & Launch Button
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ArenaDeepSlate.copy(alpha = 0.94f))
                        .border(1.5.dp, ArenaBorderSteel, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedStage.difficultyLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selectedStage == CampaignStage.FIGHT_3_BOSS) DragonMagmaRed else BuffaloSteelCyan
                            )
                            Text(
                                text = "BEST OF 3 ROUNDS",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted
                            )
                        }

                        Text(
                            text = selectedStage.subtitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPure
                        )

                        Text(
                            text = selectedStage.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )

                        // For Fight 3 (Dragon Boss), let player choose between the 2 playable warriors!
                        if (selectedStage.allowFighterChoice) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "CHOOSE YOUR CHAMPION FOR THE FINAL BOSS:",
                                style = MaterialTheme.typography.labelMedium,
                                color = TigerGold
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FighterId.playableRoster.forEach { fighter ->
                                    val chosen = fighter == fight3ChosenFighter
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (chosen) TigerFlameOrange.copy(alpha = 0.25f) else ArenaSurface)
                                            .border(
                                                width = if (chosen) 2.dp else 1.dp,
                                                color = if (chosen) TigerFlameOrange else ArenaBorderSteel,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable { onSelectFight3Fighter(fighter) }
                                            .testTag("boss_challenger_${fighter.name.lowercase()}")
                                    ) {
                                        Text(
                                            text = fighter.displayName,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextPure,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onStartStage(selectedStage) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedStage == CampaignStage.FIGHT_3_BOSS) DragonMagmaRed else TigerFlameOrange
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_enter_arena")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = if (selectedStage == CampaignStage.FIGHT_3_BOSS) TextPure else ArenaObsidian
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ENTER THE ARENA",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
                            color = if (selectedStage == CampaignStage.FIGHT_3_BOSS) TextPure else ArenaObsidian
                        )
                    }
                }
            }
        }
    }
}
