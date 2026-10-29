package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.engine3d.FightingGLViewport
import com.example.engine3d.ViewportSceneMode
import com.example.game.GameViewModel
import com.example.model.ScreenRoute
import com.example.ui.screens.CampaignSelectOverlay
import com.example.ui.screens.CombatScreenOverlay
import com.example.ui.screens.FighterRosterOverlay
import com.example.ui.screens.MainMenuOverlay
import com.example.ui.screens.SettingsOverlay
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.SnTekkenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureImmersiveGameBars()
        setContent {
            SnTekkenTheme {
                SnTekkenApp()
            }
        }
    }

    private fun configureImmersiveGameBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
fun SnTekkenApp(
    gameViewModel: GameViewModel = viewModel()
) {
    val state by gameViewModel.uiState.collectAsStateWithLifecycle()

    val sceneMode = when (state.route) {
        ScreenRoute.FIGHTER_ROSTER -> ViewportSceneMode.ROSTER_PREVIEW
        ScreenRoute.COMBAT -> ViewportSceneMode.COMBAT_ARENA
        ScreenRoute.MAIN_MENU,
        ScreenRoute.CAMPAIGN_SELECT,
        ScreenRoute.SETTINGS -> ViewportSceneMode.MAIN_MENU_SHOWCASE
    }

    Scaffold(
        containerColor = ArenaObsidian,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Persistent Real-Time 3D OpenGL ES Arena & Rigged Animal Warriors Viewport
            FightingGLViewport(
                sceneMode = sceneMode,
                combatSnapshot = state.combat,
                rosterFighter = state.rosterSelectedFighter,
                rosterYawDeg = state.rosterYawDeg,
                graphicsQuality = state.settings.graphicsQuality,
                cameraSensitivity = state.settings.cameraSensitivity,
                particleBatchToken = state.particleBatchToken,
                drainParticleBatch = gameViewModel::drainParticleBatch,
                modifier = Modifier.fillMaxSize()
            )

            // Active Screen UI Layer
            when (state.route) {
                ScreenRoute.MAIN_MENU -> {
                    MainMenuOverlay(
                        soundOn = state.settings.sfxEnabled || state.settings.musicEnabled,
                        campaignCompleted = state.campaignCompleted,
                        unlockedStage = state.effectiveUnlockedStage,
                        onPlayCampaign = gameViewModel::openCampaignHub,
                        onOpenRoster = { gameViewModel.navigateTo(ScreenRoute.FIGHTER_ROSTER) },
                        onOpenSettings = { gameViewModel.navigateTo(ScreenRoute.SETTINGS) },
                        onToggleQuickSound = gameViewModel::toggleQuickSound
                    )
                }

                ScreenRoute.CAMPAIGN_SELECT -> {
                    CampaignSelectOverlay(
                        selectedStage = state.selectedStage,
                        effectiveUnlockedStage = state.effectiveUnlockedStage,
                        fight3ChosenFighter = state.fight3ChosenFighter,
                        onSelectStage = gameViewModel::selectCampaignStage,
                        onSelectFight3Fighter = gameViewModel::selectFight3Fighter,
                        onStartStage = gameViewModel::launchCampaignStage,
                        onBack = { gameViewModel.navigateTo(ScreenRoute.MAIN_MENU) }
                    )
                }

                ScreenRoute.FIGHTER_ROSTER -> {
                    FighterRosterOverlay(
                        selectedFighter = state.rosterSelectedFighter,
                        onSelectFighter = gameViewModel::selectRosterFighter,
                        onRotatePreview = gameViewModel::rotateRosterPreview,
                        onBack = { gameViewModel.navigateTo(ScreenRoute.MAIN_MENU) }
                    )
                }

                ScreenRoute.SETTINGS -> {
                    SettingsOverlay(
                        settings = state.settings,
                        onUpdateSettings = gameViewModel::updateSettings,
                        onSelectGraphics = gameViewModel::setGraphicsQuality,
                        onReturnToMenu = { gameViewModel.navigateTo(ScreenRoute.MAIN_MENU) }
                    )
                }

                ScreenRoute.COMBAT -> {
                    CombatScreenOverlay(
                        snapshot = state.combat,
                        settings = state.settings,
                        onMoveAxisChanged = gameViewModel::setPlayerMoveAxis,
                        onBlockHeldChanged = gameViewModel::setPlayerBlockHeld,
                        onActionPressed = gameViewModel::onCombatActionPressed,
                        onTogglePause = gameViewModel::togglePauseCombat,
                        onNextFight = gameViewModel::advanceToNextFight,
                        onRematch = gameViewModel::rematchCurrentStage,
                        onReturnMainMenu = { gameViewModel.navigateTo(ScreenRoute.MAIN_MENU) }
                    )
                }
            }
        }
    }
}
