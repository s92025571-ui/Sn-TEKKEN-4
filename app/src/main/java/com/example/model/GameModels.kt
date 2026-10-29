package com.example.model

import androidx.annotation.DrawableRes
import com.example.R

enum class FighterId(
    val displayName: String,
    val title: String,
    val styleName: String,
    val specialMoveName: String,
    val bio: String,
    val isPlayable: Boolean,
    val maxHealth: Float,
    val moveSpeed: Float,
    val bodyRadius: Float,
    val powerStat: Int,
    val speedStat: Int,
    val defenseStat: Int,
    @DrawableRes val portraitRes: Int
) {
    SHADOW_TIGER(
        displayName = "SHADOW TIGER",
        title = "Crimson Claw Grandmaster",
        styleName = "Fierce Tiger Kung-Fu & Claw Rush",
        specialMoveName = "Blazing Tornado Claw",
        bio = "An agile, razor-sharp martial artist combining lightning claw combinations, spinning heel kicks, and explosive chi strikes.",
        isPlayable = true,
        maxHealth = 1000f,
        moveSpeed = 3.65f,
        bodyRadius = 0.56f,
        powerStat = 84,
        speedStat = 96,
        defenseStat = 76,
        portraitRes = R.drawable.img_portrait_tiger_1793269976113
    ),
    IRON_BUFFALO(
        displayName = "IRON BUFFALO",
        title = "Armored Juggernaut of the Pit",
        styleName = "Heavy Brawler & Seismic Horn Charge",
        specialMoveName = "Tectonic Horn Stampede",
        bio = "A towering iron-clad powerhouse whose crushing fists, armored blocks, and earth-shattering horn charges overwhelm any guard.",
        isPlayable = true,
        maxHealth = 1180f,
        moveSpeed = 2.75f,
        bodyRadius = 0.68f,
        powerStat = 97,
        speedStat = 68,
        defenseStat = 95,
        portraitRes = R.drawable.img_portrait_buffalo_1793269976921
    ),
    DRAGON_BOSS(
        displayName = "INFERNO DRAGON",
        title = "Ancient Apex Sovereign",
        styleName = "Draconic Talons, Tail Sweep & Hellfire",
        specialMoveName = "Cataclysm Hellfire Breath",
        bio = "The undefeated colossus ruling the deepest underground sanctuary. Unleashes telegraphed talon cleaves, tail sweeps, slams, and molten breath.",
        isPlayable = false,
        maxHealth = 1650f,
        moveSpeed = 2.95f,
        bodyRadius = 0.84f,
        powerStat = 100,
        speedStat = 82,
        defenseStat = 98,
        portraitRes = R.drawable.img_portrait_dragon_1793269978618
    );

    companion object {
        val playableRoster: List<FighterId> = listOf(SHADOW_TIGER, IRON_BUFFALO)
    }
}

enum class CampaignStage(
    val stageNumber: Int,
    val title: String,
    val subtitle: String,
    val defaultPlayer: FighterId,
    val opponent: FighterId,
    val difficultyLabel: String,
    val description: String,
    val allowFighterChoice: Boolean = false
) {
    FIGHT_1(
        stageNumber = 1,
        title = "FIGHT 1: CLASH OF FANGS & IRON",
        subtitle = "Shadow Tiger vs Iron Buffalo",
        defaultPlayer = FighterId.SHADOW_TIGER,
        opponent = FighterId.IRON_BUFFALO,
        difficultyLabel = "ROOKIE ARENA",
        description = "Master movement, guard blocks, rapid claw combos, and kicks as Shadow Tiger against the towering Iron Buffalo."
    ),
    FIGHT_2(
        stageNumber = 2,
        title = "FIGHT 2: UNBREAKABLE JUGGERNAUT",
        subtitle = "Iron Buffalo vs Shadow Tiger",
        defaultPlayer = FighterId.IRON_BUFFALO,
        opponent = FighterId.SHADOW_TIGER,
        difficultyLabel = "VETERAN CONTENDER",
        description = "Take command of Iron Buffalo. Absorb relentless Tiger combos with iron guard and counter with crushing charges."
    ),
    FIGHT_3_BOSS(
        stageNumber = 3,
        title = "FINAL FIGHT: SOVEREIGN OF ASH",
        subtitle = "Challenger vs Inferno Dragon Boss",
        defaultPlayer = FighterId.SHADOW_TIGER,
        opponent = FighterId.DRAGON_BOSS,
        difficultyLabel = "APEX BOSS",
        description = "Read the Dragon Boss's molten telegraphs, dodge devastating tail sweeps and fire breath, and claim the Arena Crown!",
        allowFighterChoice = true
    );

    companion object {
        fun fromNumber(stage: Int): CampaignStage =
            entries.firstOrNull { it.stageNumber == stage } ?: FIGHT_1
    }
}

enum class GraphicsQuality(
    val label: String,
    val radialSegments: Int,
    val maxParticles: Int,
    val enableFloorGridGlow: Boolean
) {
    LOW("Performance (Low)", radialSegments = 10, maxParticles = 36, enableFloorGridGlow = false),
    MEDIUM("Balanced (Medium)", radialSegments = 14, maxParticles = 75, enableFloorGridGlow = true),
    HIGH("Ultra Arena (High)", radialSegments = 18, maxParticles = 130, enableFloorGridGlow = true)
}

data class GameSettings(
    val sfxEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH,
    val cameraSensitivity: Float = 1.0f,
    val controlScale: Float = 1.0f,
    val devUnlockAllStages: Boolean = false
)

enum class ScreenRoute {
    MAIN_MENU,
    CAMPAIGN_SELECT,
    FIGHTER_ROSTER,
    SETTINGS,
    COMBAT
}

enum class FighterActionState {
    IDLE,
    WALK_FORWARD,
    WALK_BACKWARD,
    BLOCK,
    BLOCK_STUN,
    DODGE_BACK,
    DODGE_FORWARD,
    ATTACK_PUNCH_1,
    ATTACK_PUNCH_2,
    ATTACK_PUNCH_3,
    ATTACK_KICK_1,
    ATTACK_KICK_2,
    ATTACK_SPECIAL,
    BOSS_CLAW_STRIKE,
    BOSS_TAIL_SWEEP,
    BOSS_GROUND_STOMP,
    BOSS_FIRE_BREATH,
    HIT_LIGHT,
    HIT_HEAVY,
    KNOCKDOWN,
    GET_UP,
    VICTORY,
    DEFEAT,
    BOSS_INTRO;

    val isAttacking: Boolean
        get() = this == ATTACK_PUNCH_1 ||
            this == ATTACK_PUNCH_2 ||
            this == ATTACK_PUNCH_3 ||
            this == ATTACK_KICK_1 ||
            this == ATTACK_KICK_2 ||
            this == ATTACK_SPECIAL ||
            this == BOSS_CLAW_STRIKE ||
            this == BOSS_TAIL_SWEEP ||
            this == BOSS_GROUND_STOMP ||
            this == BOSS_FIRE_BREATH

    val isIncapacitated: Boolean
        get() = this == HIT_LIGHT ||
            this == HIT_HEAVY ||
            this == KNOCKDOWN ||
            this == GET_UP ||
            this == DEFEAT

    val isInvulnerable: Boolean
        get() = this == DODGE_BACK ||
            this == DODGE_FORWARD ||
            this == KNOCKDOWN ||
            this == GET_UP ||
            this == BOSS_INTRO
}

data class AttackSpec(
    val action: FighterActionState,
    val startupSec: Float,
    val activeSec: Float,
    val recoverySec: Float,
    val reach: Float,
    val damage: Float,
    val chipDamage: Float,
    val knockback: Float,
    val forwardImpulse: Float,
    val causesKnockdown: Boolean = false,
    val isHeavyHit: Boolean = false,
    val guardBreak: Boolean = false,
    val telegraphLevel: Int = 0 // 0=normal, 1=amber blockable, 2=crimson unblockable/heavy
) {
    val totalDuration: Float get() = startupSec + activeSec + recoverySec
}

data class FighterRuntimeState(
    val id: FighterId,
    val x: Float,
    val vx: Float = 0f,
    val facing: Float = 1f, // +1f faces +X (right), -1f faces -X (left)
    val health: Float = id.maxHealth,
    val maxHealth: Float = id.maxHealth,
    val specialMeter: Float = 35f, // 0..100
    val specialCooldown: Float = 0f,
    val dodgeCooldown: Float = 0f,
    val action: FighterActionState = FighterActionState.IDLE,
    val prevAction: FighterActionState = FighterActionState.IDLE,
    val stateTime: Float = 0f,
    val blendFactor: Float = 1f,
    val comboWindowTimer: Float = 0f,
    val comboCountDisplay: Int = 0,
    val comboDisplayTimer: Float = 0f,
    val hitRegistered: Boolean = false,
    val bossPhase: Int = 1
) {
    val healthFraction: Float
        get() = (health / maxHealth).coerceIn(0f, 1f)
}

enum class MatchPhase {
    BOSS_ENTRANCE,
    ROUND_INTRO,
    FIGHTING,
    ROUND_OVER,
    MATCH_VICTORY,
    MATCH_DEFEAT,
    PAUSED
}

enum class ParticleKind {
    HIT_SPARK,
    BLOCK_SPARK,
    DUST_PUFF,
    TIGER_FLAME,
    BUFFALO_SHOCKWAVE,
    DRAGON_FIRE,
    DRAGON_INHALE_EMBER,
    STOMP_SEISMIC,
    ARENA_EMBER
}

data class ParticleEvent(
    val kind: ParticleKind,
    val worldX: Float,
    val worldY: Float,
    val worldZ: Float = 0f,
    val dirX: Float = 0f,
    val count: Int = 14
)

data class CombatSnapshot(
    val stage: CampaignStage = CampaignStage.FIGHT_1,
    val roundNumber: Int = 1,
    val playerRoundsWon: Int = 0,
    val opponentRoundsWon: Int = 0,
    val timerSeconds: Int = 99,
    val matchPhase: MatchPhase = MatchPhase.ROUND_INTRO,
    val bannerText: String = "ROUND 1",
    val subBannerText: String = "",
    val bossTelegraphWarning: String = "",
    val bossTelegraphCounterHint: String = "",
    val bossTelegraphProgress: Float = 0f,
    val bossTelegraphReach: Float = 0f,
    val bossTelegraphUnblockable: Boolean = false,
    val player: FighterRuntimeState = FighterRuntimeState(FighterId.SHADOW_TIGER, x = -1.8f, facing = 1f),
    val opponent: FighterRuntimeState = FighterRuntimeState(FighterId.IRON_BUFFALO, x = 1.8f, facing = -1f),
    val cameraShakeIntensity: Float = 0f,
    val elapsedArenaTime: Float = 0f
)
