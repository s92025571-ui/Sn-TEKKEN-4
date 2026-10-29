package com.example.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.ArcadeSoundEngine
import com.example.audio.MusicTrackMode
import com.example.audio.SoundCue
import com.example.data.GamePreferencesRepository
import com.example.model.CampaignStage
import com.example.model.CombatSnapshot
import com.example.model.FighterId
import com.example.model.GameSettings
import com.example.model.GraphicsQuality
import com.example.model.MatchPhase
import com.example.model.ParticleEvent
import com.example.model.ScreenRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

data class AppUiState(
    val route: ScreenRoute = ScreenRoute.MAIN_MENU,
    val settings: GameSettings = GameSettings(),
    val unlockedStage: Int = 1,
    val campaignCompleted: Boolean = false,
    val selectedStage: CampaignStage = CampaignStage.FIGHT_1,
    val fight3ChosenFighter: FighterId = FighterId.SHADOW_TIGER,
    val rosterSelectedFighter: FighterId = FighterId.SHADOW_TIGER,
    val rosterYawDeg: Float = 18f,
    val combat: CombatSnapshot = CombatSnapshot(),
    val particleBatchToken: Long = 0L
) {
    val effectiveUnlockedStage: Int
        get() = if (settings.devUnlockAllStages) 3 else unlockedStage
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = GamePreferencesRepository(application)
    private val soundEngine = ArcadeSoundEngine(application)
    private val particleQueue = ConcurrentLinkedQueue<ParticleEvent>()

    val combatEngine = CombatEngine(
        onSoundCue = { cue -> soundEngine.playCue(cue) },
        onParticles = { list ->
            for (ev in list) particleQueue.offer(ev)
            _uiState.update { it.copy(particleBatchToken = it.particleBatchToken + 1L) }
        },
        onPlayerWonMatch = { stageWon ->
            handleCampaignVictorySave(stageWon)
        }
    )

    private val _uiState = MutableStateFlow(
        run {
            val savedSettings = repo.loadSettings()
            soundEngine.sfxEnabled = savedSettings.sfxEnabled
            soundEngine.musicEnabled = savedSettings.musicEnabled
            AppUiState(
                route = ScreenRoute.MAIN_MENU,
                settings = savedSettings,
                unlockedStage = repo.getUnlockedStage(),
                campaignCompleted = repo.isCampaignCompleted()
            )
        }
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var gameLoopJob: Job? = null

    init {
        soundEngine.setMusicMode(MusicTrackMode.MENU)
    }

    fun navigateTo(route: ScreenRoute) {
        soundEngine.playCue(SoundCue.UI_CLICK)
        if (route != ScreenRoute.COMBAT) {
            stopCombatLoop()
            soundEngine.setMusicMode(MusicTrackMode.MENU)
        }
        _uiState.update { it.copy(route = route) }
    }

    fun openCampaignHub() {
        soundEngine.playCue(SoundCue.UI_CLICK)
        val highestPlayable = CampaignStage.fromNumber(_uiState.value.effectiveUnlockedStage)
        _uiState.update {
            it.copy(
                route = ScreenRoute.CAMPAIGN_SELECT,
                selectedStage = highestPlayable
            )
        }
    }

    fun selectCampaignStage(stage: CampaignStage) {
        val current = _uiState.value
        if (stage.stageNumber > current.effectiveUnlockedStage) return
        soundEngine.playCue(SoundCue.UI_CLICK)
        _uiState.update { it.copy(selectedStage = stage) }
    }

    fun selectFight3Fighter(fighter: FighterId) {
        if (!fighter.isPlayable) return
        soundEngine.playCue(SoundCue.UI_CLICK)
        _uiState.update { it.copy(fight3ChosenFighter = fighter) }
    }

    fun launchCampaignStage(stage: CampaignStage) {
        val current = _uiState.value
        if (stage.stageNumber > current.effectiveUnlockedStage) return
        soundEngine.playCue(SoundCue.UI_CLICK)

        val chosen = if (stage == CampaignStage.FIGHT_3_BOSS) current.fight3ChosenFighter else stage.defaultPlayer
        combatEngine.playerMoveAxis = 0f
        combatEngine.playerHoldingBlock = false
        combatEngine.startMatch(stage, chosen)

        soundEngine.setMusicMode(
            if (stage == CampaignStage.FIGHT_3_BOSS) MusicTrackMode.BOSS_COMBAT else MusicTrackMode.COMBAT
        )

        _uiState.update {
            it.copy(
                route = ScreenRoute.COMBAT,
                selectedStage = stage,
                combat = combatEngine.snapshot
            )
        }
        startCombatLoop()
    }

    fun rematchCurrentStage() {
        launchCampaignStage(_uiState.value.combat.stage)
    }

    fun advanceToNextFight() {
        val curStageNum = _uiState.value.combat.stage.stageNumber
        if (curStageNum < 3) {
            val nextStage = CampaignStage.fromNumber(curStageNum + 1)
            launchCampaignStage(nextStage)
        } else {
            navigateTo(ScreenRoute.MAIN_MENU)
        }
    }

    fun setPlayerMoveAxis(axis: Float) {
        combatEngine.playerMoveAxis = axis.coerceIn(-1f, 1f)
    }

    fun setPlayerBlockHeld(held: Boolean) {
        combatEngine.playerHoldingBlock = held
    }

    fun onCombatActionPressed(input: CombatButtonInput) {
        combatEngine.queuePlayerButton(input)
    }

    fun togglePauseCombat() {
        val phase = combatEngine.snapshot.matchPhase
        if (phase == MatchPhase.FIGHTING || phase == MatchPhase.ROUND_INTRO || phase == MatchPhase.BOSS_ENTRANCE) {
            soundEngine.playCue(SoundCue.UI_CLICK)
            combatEngine.setPaused(true)
            _uiState.update { it.copy(combat = combatEngine.snapshot) }
        } else if (phase == MatchPhase.PAUSED) {
            soundEngine.playCue(SoundCue.UI_CLICK)
            combatEngine.setPaused(false)
            _uiState.update { it.copy(combat = combatEngine.snapshot) }
        }
    }

    fun selectRosterFighter(fighter: FighterId) {
        if (!fighter.isPlayable) return
        soundEngine.playCue(
            if (fighter == FighterId.SHADOW_TIGER) SoundCue.TIGER_ROAR else SoundCue.BUFFALO_BELLOW
        )
        _uiState.update {
            it.copy(
                rosterSelectedFighter = fighter,
                rosterYawDeg = 18f
            )
        }
    }

    fun rotateRosterPreview(deltaDegrees: Float) {
        _uiState.update {
            it.copy(rosterYawDeg = (it.rosterYawDeg + deltaDegrees) % 360f)
        }
    }

    fun toggleQuickSound() {
        val cur = _uiState.value.settings
        val next = cur.copy(sfxEnabled = !cur.sfxEnabled, musicEnabled = !cur.sfxEnabled)
        updateSettings(next)
    }

    fun updateSettings(newSettings: GameSettings) {
        repo.saveSettings(newSettings)
        soundEngine.sfxEnabled = newSettings.sfxEnabled
        soundEngine.refreshMusicState(newSettings.musicEnabled)
        _uiState.update { it.copy(settings = newSettings) }
    }

    fun setGraphicsQuality(quality: GraphicsQuality) {
        soundEngine.playCue(SoundCue.UI_CLICK)
        updateSettings(_uiState.value.settings.copy(graphicsQuality = quality))
    }

    fun drainParticleBatch(): List<ParticleEvent> {
        val list = ArrayList<ParticleEvent>()
        var ev = particleQueue.poll()
        while (ev != null) {
            list.add(ev)
            ev = particleQueue.poll()
        }
        return list
    }

    private fun handleCampaignVictorySave(stageWon: CampaignStage) {
        val nextUnlock = (stageWon.stageNumber + 1).coerceAtMost(3)
        repo.unlockStage(nextUnlock)
        val completedAll = stageWon == CampaignStage.FIGHT_3_BOSS || repo.isCampaignCompleted()
        if (stageWon == CampaignStage.FIGHT_3_BOSS) {
            repo.setCampaignCompleted(true)
        }
        _uiState.update {
            it.copy(
                unlockedStage = repo.getUnlockedStage(),
                campaignCompleted = completedAll
            )
        }
    }

    private fun startCombatLoop() {
        stopCombatLoop()
        gameLoopJob = viewModelScope.launch {
            var lastNs = System.nanoTime()
            while (isActive) {
                delay(16L) // ~60 FPS simulation tick
                val now = System.nanoTime()
                val dt = ((now - lastNs) / 1_000_000_000f).coerceIn(0.005f, 0.04f)
                lastNs = now
                combatEngine.step(dt)
                _uiState.update { it.copy(combat = combatEngine.snapshot) }
            }
        }
    }

    private fun stopCombatLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    override fun onCleared() {
        stopCombatLoop()
        soundEngine.release()
        super.onCleared()
    }
}
