package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.CombatButtonInput
import com.example.model.CampaignStage
import com.example.model.CombatSnapshot
import com.example.model.FighterId
import com.example.model.FighterRuntimeState
import com.example.model.GameSettings
import com.example.model.MatchPhase
import com.example.ui.components.CombatControlsOverlay
import com.example.ui.theme.ArenaBorderSteel
import com.example.ui.theme.ArenaDeepSlate
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.ArenaSurface
import com.example.ui.theme.BuffaloSteelCyan
import com.example.ui.theme.DragonEmberGlow
import com.example.ui.theme.DragonMagmaRed
import com.example.ui.theme.HealthCritical
import com.example.ui.theme.HealthEmerald
import com.example.ui.theme.HealthGoldWarn
import com.example.ui.theme.SpecialMeterCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPure
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TigerFlameOrange
import com.example.ui.theme.TigerGold

@Composable
fun CombatScreenOverlay(
    snapshot: CombatSnapshot,
    settings: GameSettings,
    onMoveAxisChanged: (Float) -> Unit,
    onBlockHeldChanged: (Boolean) -> Unit,
    onActionPressed: (CombatButtonInput) -> Unit,
    onTogglePause: () -> Unit,
    onNextFight: () -> Unit,
    onRematch: () -> Unit,
    onReturnMainMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        if (snapshot.matchPhase == MatchPhase.MATCH_VICTORY || snapshot.matchPhase == MatchPhase.MATCH_DEFEAT) {
            onReturnMainMenu()
        } else {
            onTogglePause()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("combat_screen_overlay")
    ) {
        // 1. TOP ARCADE COMBAT HUD (Health Bars, Round Pips, Timer, Special Meter)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FighterHudPanel(
                    fighter = snapshot.player,
                    roundsWon = snapshot.playerRoundsWon,
                    isLeftPlayer = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("player_health_hud")
                )

                // Center Timer & Pause Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(82.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(ArenaObsidian.copy(alpha = 0.88f))
                            .border(1.5.dp, TigerGold.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .testTag("combat_timer_box")
                    ) {
                        Text(
                            text = snapshot.timerSeconds.toString().padStart(2, '0'),
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (snapshot.timerSeconds <= 15) HealthCritical else TextPure
                        )
                    }
                    Text(
                        text = "STAGE ${snapshot.stage.stageNumber}/3",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                        color = TigerGold
                    )
                }

                FighterHudPanel(
                    fighter = snapshot.opponent,
                    roundsWon = snapshot.opponentRoundsWon,
                    isLeftPlayer = false,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("opponent_health_hud")
                )

                IconButton(
                    onClick = onTogglePause,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ArenaObsidian.copy(alpha = 0.78f))
                        .border(1.dp, ArenaBorderSteel, CircleShape)
                        .testTag("btn_pause_combat")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Pause,
                        contentDescription = stringResource(R.string.cd_pause_game),
                        tint = TextPure
                    )
                }
            }

            // Dedicated Dragon Boss Phase & Boss Health Bar Indicator during Fight 3!
            if (snapshot.opponent.id == FighterId.DRAGON_BOSS) {
                Spacer(modifier = Modifier.height(4.dp))
                DragonBossPhaseBar(
                    boss = snapshot.opponent,
                    modifier = Modifier
                        .fillMaxWidth(0.68f)
                        .testTag("dragon_boss_phase_hud")
                )
            }

            // Boss Attack Telegraph Warning Banner with Counterplay Badge & Countdown Bar
            AnimatedVisibility(
                visible = snapshot.bossTelegraphWarning.isNotEmpty() && snapshot.matchPhase == MatchPhase.FIGHTING,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val isUnblockable = snapshot.bossTelegraphUnblockable
                val borderCol = if (isUnblockable) DragonMagmaRed else TigerGold
                val bgCol = if (isUnblockable) Color(0xFF450A0A).copy(alpha = 0.94f) else ArenaObsidian.copy(alpha = 0.92f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .widthIn(min = 260.dp, max = 380.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgCol)
                        .border(2.dp, borderCol, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("boss_telegraph_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = if (isUnblockable) DragonMagmaRed else TigerGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = snapshot.bossTelegraphWarning,
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                            color = TextPure
                        )
                    }
                    if (snapshot.bossTelegraphCounterHint.isNotEmpty()) {
                        Text(
                            text = snapshot.bossTelegraphCounterHint,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isUnblockable) TigerGold else BuffaloSteelCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Live Telegraph Charge Bar (gives player exact visual timing before strike lands!)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(ArenaSurface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(snapshot.bossTelegraphProgress.coerceIn(0f, 1f))
                                .background(
                                    if (isUnblockable) DragonMagmaRed else TigerFlameOrange
                                )
                        )
                    }
                }
            }
        }

        // Player Combo Counter Callout
        if (snapshot.player.comboCountDisplay >= 2 && snapshot.matchPhase == MatchPhase.FIGHTING) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp, bottom = 56.dp)
            ) {
                Text(
                    text = "${snapshot.player.comboCountDisplay} HIT",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TigerGold
                )
                Text(
                    text = "COMBO RUSH!",
                    style = MaterialTheme.typography.labelMedium,
                    color = TigerFlameOrange
                )
            }
        }

        // 2. CENTER CALLOUT BANNERS (BOSS ENTRANCE / ROUND 1 / FIGHT! / K.O.!)
        if (snapshot.bannerText.isNotEmpty() &&
            snapshot.matchPhase != MatchPhase.MATCH_VICTORY &&
            snapshot.matchPhase != MatchPhase.MATCH_DEFEAT &&
            snapshot.matchPhase != MatchPhase.PAUSED
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ArenaObsidian.copy(alpha = 0.78f))
                    .border(2.dp, TigerFlameOrange.copy(alpha = 0.75f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 32.dp, vertical = 14.dp)
            ) {
                Text(
                    text = snapshot.bannerText,
                    style = MaterialTheme.typography.displayMedium,
                    color = TigerGold,
                    textAlign = TextAlign.Center
                )
                if (snapshot.subBannerText.isNotEmpty()) {
                    Text(
                        text = snapshot.subBannerText,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPure,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 3. LIVE MOBILE TOUCH CONTROLS (Active during FIGHTING & ROUND_INTRO)
        if (snapshot.matchPhase == MatchPhase.FIGHTING || snapshot.matchPhase == MatchPhase.ROUND_INTRO) {
            CombatControlsOverlay(
                controlScale = settings.controlScale,
                specialMeter = snapshot.player.specialMeter,
                specialCooldown = snapshot.player.specialCooldown,
                playerFacing = snapshot.player.facing,
                onMoveAxisChanged = onMoveAxisChanged,
                onBlockHeldChanged = onBlockHeldChanged,
                onActionPressed = onActionPressed
            )
        }

        // 4. PAUSE MENU MODAL
        if (snapshot.matchPhase == MatchPhase.PAUSED) {
            PauseModalCard(
                stage = snapshot.stage,
                onResume = onTogglePause,
                onRestart = onRematch,
                onMainMenu = onReturnMainMenu,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // 5. CINEMATIC VICTORY / DEFEAT / CAMPAIGN COMPLETE SCREEN
        if (snapshot.matchPhase == MatchPhase.MATCH_VICTORY || snapshot.matchPhase == MatchPhase.MATCH_DEFEAT) {
            MatchResultPanel(
                snapshot = snapshot,
                onNextFight = onNextFight,
                onRematch = onRematch,
                onMainMenu = onReturnMainMenu,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
private fun FighterHudPanel(
    fighter: FighterRuntimeState,
    roundsWon: Int,
    isLeftPlayer: Boolean,
    modifier: Modifier = Modifier
) {
    val hpFrac = fighter.healthFraction
    val barColor = when {
        hpFrac > 0.50f -> HealthEmerald
        hpFrac > 0.25f -> HealthGoldWarn
        else -> HealthCritical
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isLeftPlayer) Arrangement.Start else Arrangement.End,
        modifier = modifier
    ) {
        if (isLeftPlayer) {
            FighterAvatarBadge(fighter.id, accent = TigerFlameOrange)
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isLeftPlayer) Alignment.Start else Alignment.End,
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!isLeftPlayer) {
                    RoundWinPips(roundsWon)
                }
                Text(
                    text = fighter.id.displayName,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    color = TextPure
                )
                if (isLeftPlayer) {
                    RoundWinPips(roundsWon)
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Primary Health Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ArenaObsidian.copy(alpha = 0.85f))
                    .border(1.dp, ArenaBorderSteel, RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(hpFrac)
                        .align(if (isLeftPlayer) Alignment.CenterStart else Alignment.CenterEnd)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(barColor, barColor.copy(alpha = 0.78f))
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Special Chi / Stampede Meter
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.76f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(ArenaObsidian.copy(alpha = 0.8f))
            ) {
                val specFrac = (fighter.specialMeter / 100f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(specFrac)
                        .align(if (isLeftPlayer) Alignment.CenterStart else Alignment.CenterEnd)
                        .background(if (fighter.specialMeter >= 50f) TigerGold else SpecialMeterCyan)
                )
            }
        }

        if (!isLeftPlayer) {
            Spacer(modifier = Modifier.width(8.dp))
            FighterAvatarBadge(
                fighter.id,
                accent = if (fighter.id == FighterId.DRAGON_BOSS) DragonMagmaRed else BuffaloSteelCyan
            )
        }
    }
}

@Composable
private fun FighterAvatarBadge(id: FighterId, accent: Color) {
    Image(
        painter = painterResource(id = id.portraitRes),
        contentDescription = "${id.displayName} portrait",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(2.dp, accent, RoundedCornerShape(10.dp))
    )
}

@Composable
private fun RoundWinPips(roundsWon: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(2) { idx ->
            val filled = idx < roundsWon
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (filled) TigerGold else ArenaSurface)
                    .border(1.dp, TigerGold.copy(alpha = 0.7f), CircleShape)
            )
        }
    }
}

@Composable
private fun DragonBossPhaseBar(
    boss: FighterRuntimeState,
    modifier: Modifier = Modifier
) {
    val isPhase2 = boss.bossPhase >= 2
    val phaseText = if (isPhase2) "PHASE II: INFERNO OVERDRIVE" else "PHASE I: MOLTEN SCALES"
    val barColor = if (isPhase2) DragonMagmaRed else DragonEmberGlow

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ArenaObsidian.copy(alpha = 0.82f))
            .border(1.dp, barColor.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "APEX BOSS • INFERNO DRAGON",
                style = MaterialTheme.typography.labelMedium,
                color = TigerGold
            )
            Text(
                text = phaseText,
                style = MaterialTheme.typography.labelMedium,
                color = if (isPhase2) DragonMagmaRed else TextPure,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ArenaSurface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(boss.healthFraction)
                    .background(
                        Brush.horizontalGradient(
                            listOf(DragonMagmaRed, TigerGold)
                        )
                    )
            )
        }
    }
}

@Composable
private fun PauseModalCard(
    stage: CampaignStage,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .widthIn(max = 360.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(ArenaDeepSlate.copy(alpha = 0.95f))
            .border(1.5.dp, TigerFlameOrange, RoundedCornerShape(18.dp))
            .padding(24.dp)
            .testTag("pause_modal_card")
    ) {
        Text(
            text = "MATCH PAUSED",
            style = MaterialTheme.typography.headlineMedium,
            color = TigerGold
        )
        Text(
            text = stage.title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Button(
            onClick = onResume,
            colors = ButtonDefaults.buttonColors(containerColor = TigerFlameOrange),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_resume_fight")
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = ArenaObsidian)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.btn_resume), style = MaterialTheme.typography.labelLarge, color = ArenaObsidian)
        }

        OutlinedButton(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_pause_rematch")
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, tint = TextPure)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.btn_rematch), style = MaterialTheme.typography.labelLarge, color = TextPure)
        }

        OutlinedButton(
            onClick = onMainMenu,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_pause_main_menu")
        ) {
            Icon(Icons.Filled.Home, contentDescription = null, tint = TextPure)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.btn_return_menu), style = MaterialTheme.typography.labelLarge, color = TextPure)
        }
    }
}

@Composable
private fun MatchResultPanel(
    snapshot: CombatSnapshot,
    onNextFight: () -> Unit,
    onRematch: () -> Unit,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVictory = snapshot.matchPhase == MatchPhase.MATCH_VICTORY
    val isCampaignComplete = isVictory && snapshot.stage == CampaignStage.FIGHT_3_BOSS
    val accent = if (isVictory) TigerGold else DragonMagmaRed

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .padding(end = 24.dp)
            .widthIn(min = 290.dp, max = 360.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        ArenaDeepSlate.copy(alpha = 0.94f),
                        ArenaObsidian.copy(alpha = 0.96f)
                    )
                )
            )
            .border(2.dp, accent, RoundedCornerShape(20.dp))
            .padding(22.dp)
            .testTag("match_result_panel")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isVictory) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = TigerGold,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = when {
                    isCampaignComplete -> stringResource(R.string.label_campaign_complete)
                    isVictory -> stringResource(R.string.label_victory)
                    else -> stringResource(R.string.label_defeat)
                },
                style = MaterialTheme.typography.headlineMedium,
                color = accent
            )
        }

        Text(
            text = if (isCampaignComplete) {
                "${snapshot.player.id.displayName} conquered the Inferno Dragon and claimed the Underground Crown!"
            } else if (isVictory) {
                "Winner: ${snapshot.player.id.displayName} • Stage ${snapshot.stage.stageNumber + 1} Unlocked!"
            } else {
                "Winner: ${snapshot.opponent.id.displayName} • Study their timing and strike back!"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = TextPure
        )

        if (isVictory && snapshot.stage.stageNumber < 3) {
            Button(
                onClick = onNextFight,
                colors = ButtonDefaults.buttonColors(containerColor = TigerFlameOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_next_fight")
            ) {
                Text(
                    text = stringResource(R.string.btn_next_fight),
                    style = MaterialTheme.typography.labelLarge,
                    color = ArenaObsidian
                )
            }
        }

        OutlinedButton(
            onClick = onRematch,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_result_rematch")
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, tint = TextPure)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.btn_rematch), style = MaterialTheme.typography.labelLarge, color = TextPure)
        }

        OutlinedButton(
            onClick = onMainMenu,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_result_main_menu")
        ) {
            Icon(Icons.Filled.Home, contentDescription = null, tint = TextMuted)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.btn_return_menu), style = MaterialTheme.typography.labelLarge, color = TextPure)
        }
    }
}
