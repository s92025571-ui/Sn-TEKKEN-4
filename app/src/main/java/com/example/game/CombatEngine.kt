package com.example.game

import com.example.audio.SoundCue
import com.example.model.AttackSpec
import com.example.model.CampaignStage
import com.example.model.CombatSnapshot
import com.example.model.FighterActionState
import com.example.model.FighterId
import com.example.model.FighterRuntimeState
import com.example.model.MatchPhase
import com.example.model.ParticleEvent
import com.example.model.ParticleKind
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class CombatButtonInput {
    PUNCH,
    KICK,
    SPECIAL,
    DODGE_BACK,
    DODGE_FORWARD
}

class CombatEngine(
    private val onSoundCue: (SoundCue) -> Unit = {},
    private val onParticles: (List<ParticleEvent>) -> Unit = {},
    private val onPlayerWonMatch: (CampaignStage) -> Unit = {}
) {
    var snapshot: CombatSnapshot = CombatSnapshot()
        private set

    // Live continuous inputs from player thumb controls
    var playerMoveAxis: Float = 0f // -1f (left) .. +1f (right)
    var playerHoldingBlock: Boolean = false
    private var bufferedPlayerInput: CombatButtonInput? = null
    private var bufferedInputTimer: Float = 0f

    private var phaseTimer: Float = 0f
    private var roundClockAccum: Float = 0f
    private var hitStopTimer: Float = 0f
    private var prePausePhase: MatchPhase = MatchPhase.FIGHTING
    private var prePauseBanner: String = ""

    // AI brain state
    private var aiDecisionTimer: Float = 0.35f
    private var aiMoveIntent: Float = 0f
    private var aiBlockTimer: Float = 0f
    private var aiQueuedComboAction: FighterActionState? = null
    private var bossPatternCursor: Int = 0
    private val rng = Random(2026)

    fun startMatch(stage: CampaignStage, chosenPlayerFighter: FighterId? = null) {
        val playerFighter = if (stage.allowFighterChoice && chosenPlayerFighter?.isPlayable == true) {
            chosenPlayerFighter
        } else {
            stage.defaultPlayer
        }
        val opponentFighter = stage.opponent

        val startsWithBossIntro = opponentFighter == FighterId.DRAGON_BOSS
        val initialPhase = if (startsWithBossIntro) MatchPhase.BOSS_ENTRANCE else MatchPhase.ROUND_INTRO
        phaseTimer = 0f
        roundClockAccum = 0f
        hitStopTimer = 0f
        bufferedPlayerInput = null

        val playerState = FighterRuntimeState(
            id = playerFighter,
            x = -2.0f,
            facing = 1f,
            specialMeter = 40f
        )
        val opponentState = FighterRuntimeState(
            id = opponentFighter,
            x = 2.0f,
            facing = -1f,
            action = if (startsWithBossIntro) FighterActionState.BOSS_INTRO else FighterActionState.IDLE,
            specialMeter = 30f
        )

        snapshot = CombatSnapshot(
            stage = stage,
            roundNumber = 1,
            playerRoundsWon = 0,
            opponentRoundsWon = 0,
            timerSeconds = ROUND_DURATION_SECONDS,
            matchPhase = initialPhase,
            bannerText = if (startsWithBossIntro) "WARNING: APEX BOSS" else "ROUND 1",
            subBannerText = if (startsWithBossIntro) "INFERNO DRAGON DESCENDS" else stage.subtitle,
            player = playerState,
            opponent = opponentState
        )

        if (startsWithBossIntro) {
            onSoundCue(SoundCue.DRAGON_ROAR)
        } else {
            onSoundCue(SoundCue.ROUND_ANNOUNCE)
        }
    }

    fun queuePlayerButton(input: CombatButtonInput) {
        val phase = snapshot.matchPhase
        if (phase != MatchPhase.FIGHTING && phase != MatchPhase.ROUND_INTRO) return
        bufferedPlayerInput = input
        bufferedInputTimer = 0.28f
    }

    fun setPaused(paused: Boolean) {
        val phase = snapshot.matchPhase
        if (paused && (phase == MatchPhase.FIGHTING || phase == MatchPhase.ROUND_INTRO || phase == MatchPhase.BOSS_ENTRANCE)) {
            prePausePhase = phase
            prePauseBanner = snapshot.bannerText
            snapshot = snapshot.copy(matchPhase = MatchPhase.PAUSED, bannerText = "PAUSED")
        } else if (!paused && phase == MatchPhase.PAUSED) {
            snapshot = snapshot.copy(matchPhase = prePausePhase, bannerText = prePauseBanner)
        }
    }

    fun step(rawDt: Float) {
        val dt = rawDt.coerceIn(0.001f, 0.05f)
        val cur = snapshot
        if (cur.matchPhase == MatchPhase.PAUSED) return

        // Decay camera shake
        val nextShake = max(0f, cur.cameraShakeIntensity - dt * 2.8f)
        val nextElapsed = cur.elapsedArenaTime + dt

        if (hitStopTimer > 0f) {
            hitStopTimer = max(0f, hitStopTimer - dt)
            snapshot = cur.copy(cameraShakeIntensity = nextShake, elapsedArenaTime = nextElapsed)
            return
        }

        phaseTimer += dt

        when (cur.matchPhase) {
            MatchPhase.BOSS_ENTRANCE -> {
                val opp = advanceFighterTimers(cur.opponent, dt)
                if (phaseTimer in 0.64f..0.70f && cur.cameraShakeIntensity < 0.3f) {
                    onParticles(
                        listOf(
                            ParticleEvent(ParticleKind.DRAGON_FIRE, opp.x, 0.2f, count = 22),
                            ParticleEvent(ParticleKind.DUST_PUFF, opp.x, 0.1f, count = 18)
                        )
                    )
                    snapshot = cur.copy(
                        opponent = opp,
                        cameraShakeIntensity = 0.75f,
                        elapsedArenaTime = nextElapsed
                    )
                    return
                }
                if (phaseTimer >= 2.0f) {
                    phaseTimer = 0f
                    onSoundCue(SoundCue.ROUND_ANNOUNCE)
                    snapshot = cur.copy(
                        matchPhase = MatchPhase.ROUND_INTRO,
                        bannerText = "FINAL ROUND",
                        subBannerText = "DEFEAT THE DRAGON BOSS",
                        opponent = transitionAction(opp, FighterActionState.IDLE),
                        cameraShakeIntensity = nextShake,
                        elapsedArenaTime = nextElapsed
                    )
                } else {
                    snapshot = cur.copy(
                        opponent = opp,
                        cameraShakeIntensity = nextShake,
                        elapsedArenaTime = nextElapsed
                    )
                }
            }

            MatchPhase.ROUND_INTRO -> {
                val p = advanceFighterTimers(cur.player, dt)
                val o = advanceFighterTimers(cur.opponent, dt)
                val banner = if (phaseTimer < 0.85f) "ROUND ${cur.roundNumber}" else "FIGHT!"
                if (phaseTimer >= 1.35f) {
                    phaseTimer = 0f
                    snapshot = cur.copy(
                        matchPhase = MatchPhase.FIGHTING,
                        bannerText = "",
                        subBannerText = "",
                        player = p,
                        opponent = o,
                        cameraShakeIntensity = nextShake,
                        elapsedArenaTime = nextElapsed
                    )
                } else {
                    snapshot = cur.copy(
                        bannerText = banner,
                        player = p,
                        opponent = o,
                        cameraShakeIntensity = nextShake,
                        elapsedArenaTime = nextElapsed
                    )
                }
            }

            MatchPhase.FIGHTING -> {
                stepActiveCombat(cur, dt, nextShake, nextElapsed)
            }

            MatchPhase.ROUND_OVER -> {
                val p = advanceStateCompletion(advanceFighterTimers(cur.player, dt))
                val o = advanceStateCompletion(advanceFighterTimers(cur.opponent, dt))
                if (phaseTimer >= 1.9f) {
                    // Check if match is decided (first to 2 rounds!)
                    if (cur.playerRoundsWon >= ROUNDS_TO_WIN) {
                        phaseTimer = 0f
                        onSoundCue(SoundCue.VICTORY_FANFARE)
                        onPlayerWonMatch(cur.stage)
                        snapshot = cur.copy(
                            matchPhase = MatchPhase.MATCH_VICTORY,
                            bannerText = if (cur.stage == CampaignStage.FIGHT_3_BOSS) "CAMPAIGN COMPLETE" else "VICTORY",
                            subBannerText = "${cur.player.id.displayName} WINS",
                            player = transitionAction(p, FighterActionState.VICTORY),
                            opponent = transitionAction(o, FighterActionState.DEFEAT),
                            cameraShakeIntensity = 0f,
                            elapsedArenaTime = nextElapsed
                        )
                    } else if (cur.opponentRoundsWon >= ROUNDS_TO_WIN) {
                        phaseTimer = 0f
                        snapshot = cur.copy(
                            matchPhase = MatchPhase.MATCH_DEFEAT,
                            bannerText = "DEFEAT",
                            subBannerText = "${cur.opponent.id.displayName} PREVAILS",
                            player = transitionAction(p, FighterActionState.DEFEAT),
                            opponent = transitionAction(o, FighterActionState.VICTORY),
                            cameraShakeIntensity = 0f,
                            elapsedArenaTime = nextElapsed
                        )
                    } else {
                        // Start next round!
                        val nextRound = cur.roundNumber + 1
                        phaseTimer = 0f
                        roundClockAccum = 0f
                        onSoundCue(SoundCue.ROUND_ANNOUNCE)
                        snapshot = cur.copy(
                            roundNumber = nextRound,
                            timerSeconds = ROUND_DURATION_SECONDS,
                            matchPhase = MatchPhase.ROUND_INTRO,
                            bannerText = "ROUND $nextRound",
                            subBannerText = "",
                            bossTelegraphWarning = "",
                            bossTelegraphCounterHint = "",
                            bossTelegraphProgress = 0f,
                            bossTelegraphReach = 0f,
                            bossTelegraphUnblockable = false,
                            player = FighterRuntimeState(
                                id = cur.player.id,
                                x = -2.0f,
                                facing = 1f,
                                specialMeter = (cur.player.specialMeter + 20f).coerceAtMost(100f)
                            ),
                            opponent = FighterRuntimeState(
                                id = cur.opponent.id,
                                x = 2.0f,
                                facing = -1f,
                                specialMeter = (cur.opponent.specialMeter + 20f).coerceAtMost(100f)
                            ),
                            cameraShakeIntensity = 0f,
                            elapsedArenaTime = nextElapsed
                        )
                    }
                } else {
                    snapshot = cur.copy(
                        player = p,
                        opponent = o,
                        cameraShakeIntensity = nextShake,
                        elapsedArenaTime = nextElapsed
                    )
                }
            }

            MatchPhase.MATCH_VICTORY, MatchPhase.MATCH_DEFEAT -> {
                val p = advanceFighterTimers(cur.player, dt)
                val o = advanceFighterTimers(cur.opponent, dt)
                snapshot = cur.copy(
                    player = p,
                    opponent = o,
                    cameraShakeIntensity = nextShake,
                    elapsedArenaTime = nextElapsed
                )
            }

            MatchPhase.PAUSED -> Unit
        }
    }

    private fun stepActiveCombat(
        cur: CombatSnapshot,
        dt: Float,
        baseShake: Float,
        nextElapsed: Float
    ) {
        // 1. Round Countdown Timer
        roundClockAccum += dt
        var timerSec = cur.timerSeconds
        if (roundClockAccum >= 1.0f) {
            roundClockAccum -= 1.0f
            timerSec = max(0, timerSec - 1)
        }

        // Decay buffered input timer
        if (bufferedInputTimer > 0f) {
            bufferedInputTimer = max(0f, bufferedInputTimer - dt)
            if (bufferedInputTimer <= 0f) bufferedPlayerInput = null
        }

        var player = advanceStateCompletion(advanceFighterTimers(cur.player, dt))
        var opponent = advanceStateCompletion(advanceFighterTimers(cur.opponent, dt))

        // Check Dragon Boss Phase 2 transition (< 55% HP)
        var extraShake = baseShake
        if (opponent.id == FighterId.DRAGON_BOSS && opponent.bossPhase == 1 && opponent.healthFraction <= 0.55f) {
            opponent = opponent.copy(bossPhase = 2)
            extraShake = max(extraShake, 0.85f)
            onSoundCue(SoundCue.DRAGON_ROAR)
            onParticles(
                listOf(
                    ParticleEvent(ParticleKind.DRAGON_FIRE, opponent.x, 1.2f, count = 28)
                )
            )
        }

        // Update natural facing toward opponent when standing freely
        if (!player.action.isAttacking && !player.action.isIncapacitated) {
            val desiredFacing = if (opponent.x >= player.x) 1f else -1f
            player = player.copy(facing = desiredFacing)
        }
        if (!opponent.action.isAttacking && !opponent.action.isIncapacitated) {
            val desiredFacing = if (player.x >= opponent.x) 1f else -1f
            opponent = opponent.copy(facing = desiredFacing)
        }

        // 2. Process Player Controls (Movement, Block, Dodge, Combos, Special)
        player = processPlayerControls(player)

        // 3. Process Progressive Opponent AI
        opponent = processOpponentAI(opponent, player, cur.stage, dt)

        // 4. Integrate Horizontal Velocities, Pushbox Separation & Arena Wall Clamping
        var px = player.x + player.vx * dt
        var ox = opponent.x + opponent.vx * dt

        val minSep = (player.id.bodyRadius + opponent.id.bodyRadius) * 0.78f
        val delta = ox - px
        val dist = abs(delta)
        if (dist < minSep) {
            val push = (minSep - dist) * 0.5f
            val dir = if (delta >= 0f) 1f else -1f
            px -= dir * push
            ox += dir * push
        }

        px = px.coerceIn(-ARENA_HALF_WIDTH, ARENA_HALF_WIDTH)
        ox = ox.coerceIn(-ARENA_HALF_WIDTH, ARENA_HALF_WIDTH)

        // Apply friction to knockback/impulse velocities
        val pVxNext = dampenVelocity(player, dt)
        val oVxNext = dampenVelocity(opponent, dt)

        player = player.copy(x = px, vx = pVxNext)
        opponent = opponent.copy(x = ox, vx = oVxNext)

        // 5. Hit Detection & Damage / Block / Knockback Resolution
        val pHitResult = resolveAttackHit(attacker = player, defender = opponent)
        player = pHitResult.first
        opponent = pHitResult.second
        extraShake = max(extraShake, pHitResult.third)

        val oHitResult = resolveAttackHit(attacker = opponent, defender = player)
        opponent = oHitResult.first
        player = oHitResult.second
        extraShake = max(extraShake, oHitResult.third)

        // Boss Telegraph Metrics for 3D Floor Danger Zone & Player HUD Readability
        val telegraph = computeBossTelegraphInfo(opponent)

        // 6. Check K.O. or Time Over
        if (player.health <= 0f || opponent.health <= 0f || timerSec <= 0) {
            val playerWonRound = when {
                opponent.health <= 0f -> true
                player.health <= 0f -> false
                else -> player.healthFraction >= opponent.healthFraction
            }
            phaseTimer = 0f
            hitStopTimer = 0.14f
            onSoundCue(SoundCue.KO_IMPACT)

            val pEnd = if (playerWonRound) {
                transitionAction(player, FighterActionState.IDLE)
            } else {
                transitionAction(player.copy(health = max(0f, player.health)), FighterActionState.KNOCKDOWN)
            }
            val oEnd = if (!playerWonRound) {
                transitionAction(opponent, FighterActionState.IDLE)
            } else {
                transitionAction(opponent.copy(health = max(0f, opponent.health)), FighterActionState.KNOCKDOWN)
            }

            snapshot = cur.copy(
                timerSeconds = timerSec,
                matchPhase = MatchPhase.ROUND_OVER,
                playerRoundsWon = cur.playerRoundsWon + (if (playerWonRound) 1 else 0),
                opponentRoundsWon = cur.opponentRoundsWon + (if (!playerWonRound) 1 else 0),
                bannerText = if (timerSec <= 0 && player.health > 0f && opponent.health > 0f) "TIME OVER" else "K.O.!",
                subBannerText = if (playerWonRound) "${player.id.displayName} TAKES THE ROUND" else "${opponent.id.displayName} TAKES THE ROUND",
                bossTelegraphWarning = "",
                bossTelegraphCounterHint = "",
                bossTelegraphProgress = 0f,
                bossTelegraphReach = 0f,
                bossTelegraphUnblockable = false,
                player = pEnd,
                opponent = oEnd,
                cameraShakeIntensity = 0.85f,
                elapsedArenaTime = nextElapsed
            )
            return
        }

        snapshot = cur.copy(
            timerSeconds = timerSec,
            bossTelegraphWarning = telegraph.warningTitle,
            bossTelegraphCounterHint = telegraph.counterHint,
            bossTelegraphProgress = telegraph.progress,
            bossTelegraphReach = telegraph.reach,
            bossTelegraphUnblockable = telegraph.unblockable,
            player = player,
            opponent = opponent,
            cameraShakeIntensity = extraShake,
            elapsedArenaTime = nextElapsed
        )
    }

    private fun processPlayerControls(fighter: FighterRuntimeState): FighterRuntimeState {
        var f = fighter
        if (f.action.isIncapacitated || f.action == FighterActionState.BLOCK_STUN) return f

        // Check buffered attack or dodge inputs (supports combo canceling!)
        val input = bufferedPlayerInput
        if (input != null) {
            val canActFreely = !f.action.isAttacking &&
                f.action != FighterActionState.DODGE_BACK &&
                f.action != FighterActionState.DODGE_FORWARD

            val canChainCombo = f.action.isAttacking && canChainFromCurrentAttack(f)

            if (canActFreely || canChainCombo) {
                val triggered = tryTriggerPlayerInput(f, input)
                if (triggered != null) {
                    bufferedPlayerInput = null
                    bufferedInputTimer = 0f
                    return triggered
                }
            }
        }

        if (f.action.isAttacking ||
            f.action == FighterActionState.DODGE_BACK ||
            f.action == FighterActionState.DODGE_FORWARD
        ) {
            return f
        }

        // Guard + Directional Stick = Responsive Tactical Dodge Step!
        if (playerHoldingBlock && abs(playerMoveAxis) > 0.62f && f.dodgeCooldown <= 0f) {
            val movingTowardOpponent = (playerMoveAxis * f.facing) > 0f
            val dodgeAction = if (movingTowardOpponent) FighterActionState.DODGE_FORWARD else FighterActionState.DODGE_BACK
            val dodgeVel = if (movingTowardOpponent) f.facing * 5.4f else -f.facing * 5.2f
            onSoundCue(SoundCue.DODGE_WHOOSH)
            onParticles(listOf(ParticleEvent(ParticleKind.DUST_PUFF, f.x, 0.06f, dirX = -dodgeVel * 0.2f, count = 10)))
            return transitionAction(f, dodgeAction).copy(vx = dodgeVel, dodgeCooldown = 0.65f)
        }

        // Block Guard
        if (playerHoldingBlock) {
            return transitionAction(f, FighterActionState.BLOCK).copy(vx = 0f)
        }

        // Joystick Horizontal Movement (simultaneous with ready guard)
        if (abs(playerMoveAxis) > 0.16f) {
            val speed = f.id.moveSpeed
            val moveVx = playerMoveAxis * speed
            val isForward = (playerMoveAxis * f.facing) >= 0f
            val walkState = if (isForward) FighterActionState.WALK_FORWARD else FighterActionState.WALK_BACKWARD
            return transitionAction(f, walkState).copy(vx = moveVx)
        }

        return transitionAction(f, FighterActionState.IDLE).copy(vx = 0f)
    }

    private fun tryTriggerPlayerInput(
        f: FighterRuntimeState,
        input: CombatButtonInput
    ): FighterRuntimeState? {
        return when (input) {
            CombatButtonInput.DODGE_BACK -> {
                if (f.dodgeCooldown > 0f) return null
                onSoundCue(SoundCue.DODGE_WHOOSH)
                onParticles(listOf(ParticleEvent(ParticleKind.DUST_PUFF, f.x, 0.06f, count = 10)))
                transitionAction(f, FighterActionState.DODGE_BACK).copy(
                    vx = -f.facing * 5.2f,
                    dodgeCooldown = 0.58f
                )
            }
            CombatButtonInput.DODGE_FORWARD -> {
                if (f.dodgeCooldown > 0f) return null
                onSoundCue(SoundCue.DODGE_WHOOSH)
                onParticles(listOf(ParticleEvent(ParticleKind.DUST_PUFF, f.x, 0.06f, count = 10)))
                transitionAction(f, FighterActionState.DODGE_FORWARD).copy(
                    vx = f.facing * 5.4f,
                    dodgeCooldown = 0.58f
                )
            }
            CombatButtonInput.PUNCH -> {
                val nextPunch = when (f.action) {
                    FighterActionState.ATTACK_PUNCH_1 -> FighterActionState.ATTACK_PUNCH_2
                    FighterActionState.ATTACK_PUNCH_2 -> FighterActionState.ATTACK_PUNCH_3
                    else -> FighterActionState.ATTACK_PUNCH_1
                }
                val spec = getAttackSpec(f.id, nextPunch)
                transitionAction(f, nextPunch).copy(
                    vx = f.facing * spec.forwardImpulse,
                    hitRegistered = false
                )
            }
            CombatButtonInput.KICK -> {
                val nextKick = when (f.action) {
                    FighterActionState.ATTACK_KICK_1,
                    FighterActionState.ATTACK_PUNCH_1,
                    FighterActionState.ATTACK_PUNCH_2 -> FighterActionState.ATTACK_KICK_2
                    else -> FighterActionState.ATTACK_KICK_1
                }
                val spec = getAttackSpec(f.id, nextKick)
                transitionAction(f, nextKick).copy(
                    vx = f.facing * spec.forwardImpulse,
                    hitRegistered = false
                )
            }
            CombatButtonInput.SPECIAL -> {
                if (f.specialMeter < SPECIAL_COST || f.specialCooldown > 0f) return null
                val spec = getAttackSpec(f.id, FighterActionState.ATTACK_SPECIAL)
                onSoundCue(
                    if (f.id == FighterId.SHADOW_TIGER) SoundCue.TIGER_ROAR else SoundCue.BUFFALO_BELLOW
                )
                onParticles(
                    listOf(
                        ParticleEvent(
                            kind = if (f.id == FighterId.SHADOW_TIGER) ParticleKind.TIGER_FLAME else ParticleKind.BUFFALO_SHOCKWAVE,
                            worldX = f.x,
                            worldY = 1.0f,
                            dirX = f.facing,
                            count = 20
                        )
                    )
                )
                transitionAction(f, FighterActionState.ATTACK_SPECIAL).copy(
                    vx = f.facing * spec.forwardImpulse,
                    specialMeter = (f.specialMeter - SPECIAL_COST).coerceAtLeast(0f),
                    specialCooldown = 4.0f,
                    hitRegistered = false
                )
            }
        }
    }

    private fun processOpponentAI(
        ai: FighterRuntimeState,
        player: FighterRuntimeState,
        stage: CampaignStage,
        dt: Float
    ): FighterRuntimeState {
        var f = ai
        if (f.action.isIncapacitated || f.action == FighterActionState.BLOCK_STUN) return f

        // Execute queued AI combo follow-up if ready
        val queued = aiQueuedComboAction
        if (queued != null && f.action.isAttacking && canChainFromCurrentAttack(f)) {
            aiQueuedComboAction = null
            val spec = getAttackSpec(f.id, queued)
            return transitionAction(f, queued).copy(
                vx = f.facing * spec.forwardImpulse,
                hitRegistered = false
            )
        }

        if (f.action.isAttacking ||
            f.action == FighterActionState.DODGE_BACK ||
            f.action == FighterActionState.DODGE_FORWARD
        ) {
            return f
        }

        if (aiBlockTimer > 0f) {
            aiBlockTimer = max(0f, aiBlockTimer - dt)
            return transitionAction(f, FighterActionState.BLOCK).copy(vx = 0f)
        }

        aiDecisionTimer -= dt
        val dist = abs(player.x - f.x)
        val dirToPlayer = if (player.x >= f.x) 1f else -1f

        if (aiDecisionTimer <= 0f) {
            // Difficulty scaling across Campaign Fight 1 -> Fight 2 -> Fight 3 Boss!
            val reactionInterval = when (stage) {
                CampaignStage.FIGHT_1 -> 0.42f + rng.nextFloat() * 0.26f
                CampaignStage.FIGHT_2 -> 0.24f + rng.nextFloat() * 0.18f
                CampaignStage.FIGHT_3_BOSS -> if (f.bossPhase >= 2) 0.18f + rng.nextFloat() * 0.14f else 0.26f + rng.nextFloat() * 0.16f
            }
            aiDecisionTimer = reactionInterval

            // Reactive Block or Dodge if player is swinging in range!
            if (player.action.isAttacking && dist < 2.2f) {
                val blockChance = when (stage) {
                    CampaignStage.FIGHT_1 -> 0.22f
                    CampaignStage.FIGHT_2 -> 0.48f
                    CampaignStage.FIGHT_3_BOSS -> 0.25f
                }
                if (rng.nextFloat() < blockChance) {
                    aiBlockTimer = 0.36f
                    return transitionAction(f, FighterActionState.BLOCK).copy(vx = 0f)
                } else if (stage == CampaignStage.FIGHT_2 && rng.nextFloat() < 0.25f && f.dodgeCooldown <= 0f) {
                    onSoundCue(SoundCue.DODGE_WHOOSH)
                    return transitionAction(f, FighterActionState.DODGE_BACK).copy(
                        vx = -f.facing * 4.8f,
                        dodgeCooldown = 1.1f
                    )
                }
            }

            // Dragon Boss Specialized Tactical Attack Rotation (Claw Swipe, Ground-Shattering Stomp, Fire-Breath, Tail Sweep)
            if (f.id == FighterId.DRAGON_BOSS) {
                if (dist <= 3.65f) {
                    val bossMove = selectNextDragonBossPattern(dist, f.bossPhase)
                    val spec = getAttackSpec(f.id, bossMove)
                    when (bossMove) {
                        FighterActionState.BOSS_FIRE_BREATH -> onSoundCue(SoundCue.DRAGON_ROAR)
                        FighterActionState.BOSS_GROUND_STOMP -> onSoundCue(SoundCue.BUFFALO_BELLOW)
                        else -> Unit
                    }
                    return transitionAction(f, bossMove).copy(
                        vx = f.facing * spec.forwardImpulse,
                        hitRegistered = false
                    )
                } else {
                    aiMoveIntent = dirToPlayer
                }
            } else {
                // Fighter 1 / Fighter 2 AI Offense & Spacing
                val attackRange = 1.72f
                if (dist <= attackRange) {
                    val attackRoll = rng.nextFloat()
                    val aggression = if (stage == CampaignStage.FIGHT_1) 0.56f else 0.82f
                    if (attackRoll < aggression) {
                        if (f.specialMeter >= SPECIAL_COST && f.specialCooldown <= 0f && rng.nextFloat() < 0.35f) {
                            val spec = getAttackSpec(f.id, FighterActionState.ATTACK_SPECIAL)
                            onSoundCue(if (f.id == FighterId.SHADOW_TIGER) SoundCue.TIGER_ROAR else SoundCue.BUFFALO_BELLOW)
                            return transitionAction(f, FighterActionState.ATTACK_SPECIAL).copy(
                                vx = f.facing * spec.forwardImpulse,
                                specialMeter = f.specialMeter - SPECIAL_COST,
                                specialCooldown = 5.0f,
                                hitRegistered = false
                            )
                        }
                        val starter = if (rng.nextFloat() < 0.62f) {
                            FighterActionState.ATTACK_PUNCH_1
                        } else {
                            FighterActionState.ATTACK_KICK_1
                        }
                        // Queue combo follow-up more frequently in Fight 2
                        val comboChance = if (stage == CampaignStage.FIGHT_1) 0.25f else 0.65f
                        aiQueuedComboAction = if (rng.nextFloat() < comboChance) {
                            if (starter == FighterActionState.ATTACK_PUNCH_1) FighterActionState.ATTACK_PUNCH_2
                            else FighterActionState.ATTACK_KICK_2
                        } else null

                        val spec = getAttackSpec(f.id, starter)
                        return transitionAction(f, starter).copy(
                            vx = f.facing * spec.forwardImpulse,
                            hitRegistered = false
                        )
                    } else {
                        aiMoveIntent = -dirToPlayer * 0.75f // Tactical micro-retreat
                    }
                } else {
                    aiMoveIntent = dirToPlayer
                }
            }
        }

        if (abs(aiMoveIntent) > 0.1f && dist > 1.38f) {
            val vx = aiMoveIntent * f.id.moveSpeed * (if (stage == CampaignStage.FIGHT_1) 0.82f else 1.0f)
            val walkState = if (aiMoveIntent * f.facing >= 0f) {
                FighterActionState.WALK_FORWARD
            } else {
                FighterActionState.WALK_BACKWARD
            }
            return transitionAction(f, walkState).copy(vx = vx)
        }

        return transitionAction(f, FighterActionState.IDLE).copy(vx = 0f)
    }

    private fun resolveAttackHit(
        attacker: FighterRuntimeState,
        defender: FighterRuntimeState
    ): Triple<FighterRuntimeState, FighterRuntimeState, Float> {
        if (!attacker.action.isAttacking || attacker.hitRegistered) {
            return Triple(attacker, defender, 0f)
        }
        val spec = getAttackSpec(attacker.id, attacker.action)
        val inActiveWindow = attacker.stateTime >= spec.startupSec &&
            attacker.stateTime <= (spec.startupSec + spec.activeSec)
        if (!inActiveWindow) {
            return Triple(attacker, defender, 0f)
        }

        // Check directional reach & horizontal overlap
        val dx = defender.x - attacker.x
        val inFront = (dx * attacker.facing) >= -0.25f
        val dist = abs(dx)
        if (!inFront || dist > spec.reach) {
            return Triple(attacker, defender, 0f)
        }

        // Invulnerable dodge / knockdown frames evade cleanly!
        if (defender.action.isInvulnerable) {
            return Triple(attacker, defender, 0f)
        }

        val updatedAttacker = attacker.copy(
            hitRegistered = true,
            specialMeter = (attacker.specialMeter + 12f).coerceAtMost(100f),
            comboCountDisplay = if (defender.action == FighterActionState.HIT_LIGHT || defender.action == FighterActionState.HIT_HEAVY) {
                attacker.comboCountDisplay + 1
            } else 1,
            comboDisplayTimer = 1.4f
        )

        val impactX = (attacker.x + defender.x) * 0.5f

        // Check if Defender is Blocking (and attack does not guard-break)
        val isDefenderBlocking = (defender.action == FighterActionState.BLOCK || defender.action == FighterActionState.BLOCK_STUN) &&
            !spec.guardBreak

        if (isDefenderBlocking) {
            onSoundCue(SoundCue.BLOCK_CLANG)
            onParticles(
                listOf(
                    ParticleEvent(ParticleKind.BLOCK_SPARK, impactX, 1.25f, dirX = -attacker.facing, count = 14)
                )
            )
            hitStopTimer = 0.035f
            val nextDefender = transitionAction(defender, FighterActionState.BLOCK_STUN).copy(
                health = max(1f, defender.health - spec.chipDamage),
                vx = attacker.facing * (spec.knockback * 0.55f),
                specialMeter = (defender.specialMeter + 7f).coerceAtMost(100f)
            )
            return Triple(updatedAttacker, nextDefender, 0.16f)
        }

        // Clean Hit Landed!
        val sound = when {
            attacker.action == FighterActionState.BOSS_FIRE_BREATH -> SoundCue.FIRE_BREATH
            spec.causesKnockdown || spec.isHeavyHit -> if (attacker.action == FighterActionState.ATTACK_KICK_1 || attacker.action == FighterActionState.ATTACK_KICK_2) SoundCue.KICK_HIT else SoundCue.HEAVY_SLAM
            else -> SoundCue.PUNCH_HIT
        }
        onSoundCue(sound)

        val pKind = when {
            attacker.id == FighterId.DRAGON_BOSS && attacker.action == FighterActionState.BOSS_FIRE_BREATH -> ParticleKind.DRAGON_FIRE
            attacker.id == FighterId.DRAGON_BOSS && attacker.action == FighterActionState.BOSS_GROUND_STOMP -> ParticleKind.STOMP_SEISMIC
            attacker.action == FighterActionState.ATTACK_SPECIAL && attacker.id == FighterId.SHADOW_TIGER -> ParticleKind.TIGER_FLAME
            attacker.action == FighterActionState.ATTACK_SPECIAL && attacker.id == FighterId.IRON_BUFFALO -> ParticleKind.BUFFALO_SHOCKWAVE
            else -> ParticleKind.HIT_SPARK
        }
        onParticles(
            listOf(
                ParticleEvent(pKind, impactX, if (pKind == ParticleKind.STOMP_SEISMIC) 0.14f else 1.22f, dirX = attacker.facing, count = if (spec.isHeavyHit) 24 else 14),
                ParticleEvent(ParticleKind.DUST_PUFF, defender.x, 0.08f, dirX = attacker.facing, count = 10)
            )
        )

        hitStopTimer = if (spec.isHeavyHit || spec.causesKnockdown) 0.065f else 0.038f
        val defReaction = when {
            spec.causesKnockdown -> FighterActionState.KNOCKDOWN
            spec.isHeavyHit -> FighterActionState.HIT_HEAVY
            else -> FighterActionState.HIT_LIGHT
        }

        val defenseMult = when (defender.id) {
            FighterId.IRON_BUFFALO -> 0.90f
            FighterId.DRAGON_BOSS -> 0.85f
            FighterId.SHADOW_TIGER -> 1.0f
        }
        val finalDamage = spec.damage * defenseMult

        val nextDefender = transitionAction(defender, defReaction).copy(
            health = max(0f, defender.health - finalDamage),
            vx = attacker.facing * spec.knockback,
            specialMeter = (defender.specialMeter + 9f).coerceAtMost(100f)
        )
        val shake = if (spec.causesKnockdown) 0.65f else if (spec.isHeavyHit) 0.40f else 0.20f
        return Triple(updatedAttacker, nextDefender, shake)
    }

    private data class BossTelegraphInfo(
        val warningTitle: String = "",
        val counterHint: String = "",
        val progress: Float = 0f,
        val reach: Float = 0f,
        val unblockable: Boolean = false
    )

    private fun selectNextDragonBossPattern(dist: Float, bossPhase: Int): FighterActionState {
        // At long range (> 2.45m), punish distance with Cataclysm Fire-Breath without skipping melee rotation slots
        if (dist > 2.45f) {
            return FighterActionState.BOSS_FIRE_BREATH
        }
        // Cycles through distinct core patterns (Claw Swipe, Ground-Shattering Stomp, Fire-Breath, plus Tail Sweep)
        val slot = bossPatternCursor % 4
        bossPatternCursor++
        return when (slot) {
            0 -> FighterActionState.BOSS_CLAW_STRIKE
            1 -> FighterActionState.BOSS_GROUND_STOMP
            2 -> FighterActionState.BOSS_FIRE_BREATH
            else -> if (bossPhase >= 2) FighterActionState.BOSS_TAIL_SWEEP else FighterActionState.BOSS_GROUND_STOMP
        }
    }

    private fun computeBossTelegraphInfo(opponent: FighterRuntimeState): BossTelegraphInfo {
        if (opponent.id != FighterId.DRAGON_BOSS || !opponent.action.isAttacking) {
            return BossTelegraphInfo()
        }
        val spec = getAttackSpec(opponent.id, opponent.action)
        if (opponent.stateTime > spec.startupSec) {
            return BossTelegraphInfo()
        }
        val prog = (opponent.stateTime / spec.startupSec.coerceAtLeast(0.05f)).coerceIn(0f, 1f)
        return when (opponent.action) {
            FighterActionState.BOSS_CLAW_STRIKE -> BossTelegraphInfo(
                warningTitle = "DRACONIC CLAW SWIPE",
                counterHint = "HOLD BLOCK OR BACKSTEP!",
                progress = prog,
                reach = spec.reach,
                unblockable = false
            )
            FighterActionState.BOSS_GROUND_STOMP -> BossTelegraphInfo(
                warningTitle = "GROUND-SHATTERING STOMP",
                counterHint = "UNBLOCKABLE SHOCKWAVE — DODGE!",
                progress = prog,
                reach = spec.reach,
                unblockable = true
            )
            FighterActionState.BOSS_FIRE_BREATH -> BossTelegraphInfo(
                warningTitle = "CATACLYSM FIRE-BREATH",
                counterHint = "DODGE EVADE OR HOLD GUARD!",
                progress = prog,
                reach = spec.reach,
                unblockable = false
            )
            FighterActionState.BOSS_TAIL_SWEEP -> BossTelegraphInfo(
                warningTitle = "OBSIDIAN TAIL SWEEP",
                counterHint = "BLOCK OR BACKSTEP!",
                progress = prog,
                reach = spec.reach,
                unblockable = false
            )
            else -> BossTelegraphInfo()
        }
    }

    private fun canChainFromCurrentAttack(f: FighterRuntimeState): Boolean {
        val spec = getAttackSpec(f.id, f.action)
        val chainStart = spec.startupSec + spec.activeSec * 0.45f
        return when (f.action) {
            FighterActionState.ATTACK_PUNCH_1,
            FighterActionState.ATTACK_PUNCH_2,
            FighterActionState.ATTACK_KICK_1 -> f.stateTime >= chainStart
            else -> false
        }
    }

    private fun advanceFighterTimers(f: FighterRuntimeState, dt: Float): FighterRuntimeState {
        return f.copy(
            stateTime = f.stateTime + dt,
            blendFactor = min(1f, f.blendFactor + dt / 0.11f),
            specialCooldown = max(0f, f.specialCooldown - dt),
            dodgeCooldown = max(0f, f.dodgeCooldown - dt),
            specialMeter = min(100f, f.specialMeter + dt * 2.2f),
            comboDisplayTimer = max(0f, f.comboDisplayTimer - dt),
            comboCountDisplay = if (f.comboDisplayTimer - dt <= 0f) 0 else f.comboCountDisplay
        )
    }

    private fun advanceStateCompletion(f: FighterRuntimeState): FighterRuntimeState {
        if (f.action.isAttacking) {
            val spec = getAttackSpec(f.id, f.action)
            if (f.stateTime >= spec.totalDuration) {
                return transitionAction(f, FighterActionState.IDLE)
            }
            return f
        }
        return when (f.action) {
            FighterActionState.DODGE_BACK -> if (f.stateTime >= 0.34f) transitionAction(f, FighterActionState.IDLE) else f
            FighterActionState.DODGE_FORWARD -> if (f.stateTime >= 0.32f) transitionAction(f, FighterActionState.IDLE) else f
            FighterActionState.BLOCK_STUN -> if (f.stateTime >= 0.22f) transitionAction(f, FighterActionState.IDLE) else f
            FighterActionState.HIT_LIGHT -> if (f.stateTime >= 0.28f) transitionAction(f, FighterActionState.IDLE) else f
            FighterActionState.HIT_HEAVY -> if (f.stateTime >= 0.42f) transitionAction(f, FighterActionState.IDLE) else f
            FighterActionState.KNOCKDOWN -> if (f.stateTime >= 0.72f) transitionAction(f, FighterActionState.GET_UP) else f
            FighterActionState.GET_UP -> if (f.stateTime >= 0.46f) transitionAction(f, FighterActionState.IDLE) else f
            else -> f
        }
    }

    private fun transitionAction(f: FighterRuntimeState, next: FighterActionState): FighterRuntimeState {
        if (f.action == next) return f
        return f.copy(
            prevAction = f.action,
            action = next,
            stateTime = 0f,
            blendFactor = 0f,
            hitRegistered = false
        )
    }

    private fun dampenVelocity(f: FighterRuntimeState, dt: Float): Float {
        if (f.action == FighterActionState.WALK_FORWARD || f.action == FighterActionState.WALK_BACKWARD) {
            return f.vx
        }
        val decay = max(0f, 1f - dt * 8.5f)
        return if (abs(f.vx) < 0.05f) 0f else f.vx * decay
    }

    fun getAttackSpec(id: FighterId, action: FighterActionState): AttackSpec {
        val isBuffalo = id == FighterId.IRON_BUFFALO
        return when (action) {
            FighterActionState.ATTACK_PUNCH_1 -> if (!isBuffalo) {
                AttackSpec(action, startupSec = 0.07f, activeSec = 0.10f, recoverySec = 0.13f, reach = 1.68f, damage = 58f, chipDamage = 9f, knockback = 1.6f, forwardImpulse = 1.4f)
            } else {
                AttackSpec(action, startupSec = 0.10f, activeSec = 0.12f, recoverySec = 0.16f, reach = 1.75f, damage = 76f, chipDamage = 14f, knockback = 2.4f, forwardImpulse = 1.6f)
            }

            FighterActionState.ATTACK_PUNCH_2 -> if (!isBuffalo) {
                AttackSpec(action, startupSec = 0.08f, activeSec = 0.12f, recoverySec = 0.16f, reach = 1.76f, damage = 72f, chipDamage = 12f, knockback = 2.2f, forwardImpulse = 2.1f)
            } else {
                AttackSpec(action, startupSec = 0.12f, activeSec = 0.14f, recoverySec = 0.18f, reach = 1.82f, damage = 96f, chipDamage = 18f, knockback = 3.2f, forwardImpulse = 2.2f, isHeavyHit = true)
            }

            FighterActionState.ATTACK_PUNCH_3 -> if (!isBuffalo) {
                AttackSpec(action, startupSec = 0.11f, activeSec = 0.15f, recoverySec = 0.22f, reach = 1.88f, damage = 112f, chipDamage = 20f, knockback = 4.8f, forwardImpulse = 3.0f, causesKnockdown = true, isHeavyHit = true)
            } else {
                AttackSpec(action, startupSec = 0.15f, activeSec = 0.16f, recoverySec = 0.24f, reach = 1.94f, damage = 142f, chipDamage = 28f, knockback = 5.6f, forwardImpulse = 2.6f, causesKnockdown = true, isHeavyHit = true)
            }

            FighterActionState.ATTACK_KICK_1 -> if (!isBuffalo) {
                AttackSpec(action, startupSec = 0.10f, activeSec = 0.14f, recoverySec = 0.20f, reach = 1.95f, damage = 86f, chipDamage = 15f, knockback = 3.0f, forwardImpulse = 1.8f, isHeavyHit = true)
            } else {
                AttackSpec(action, startupSec = 0.14f, activeSec = 0.15f, recoverySec = 0.22f, reach = 1.92f, damage = 110f, chipDamage = 22f, knockback = 4.0f, forwardImpulse = 1.9f, isHeavyHit = true)
            }

            FighterActionState.ATTACK_KICK_2 -> if (!isBuffalo) {
                AttackSpec(action, startupSec = 0.13f, activeSec = 0.16f, recoverySec = 0.23f, reach = 2.08f, damage = 124f, chipDamage = 22f, knockback = 5.2f, forwardImpulse = 3.2f, causesKnockdown = true, isHeavyHit = true)
            } else {
                AttackSpec(action, startupSec = 0.16f, activeSec = 0.16f, recoverySec = 0.24f, reach = 2.02f, damage = 148f, chipDamage = 30f, knockback = 5.8f, forwardImpulse = 2.5f, causesKnockdown = true, isHeavyHit = true)
            }

            FighterActionState.ATTACK_SPECIAL -> if (!isBuffalo) {
                // Shadow Tiger: Blazing Tornado Claw
                AttackSpec(action, startupSec = 0.16f, activeSec = 0.30f, recoverySec = 0.26f, reach = 2.35f, damage = 195f, chipDamage = 42f, knockback = 6.4f, forwardImpulse = 5.4f, causesKnockdown = true, isHeavyHit = true)
            } else {
                // Iron Buffalo: Tectonic Horn Stampede (Guard-Crushing!)
                AttackSpec(action, startupSec = 0.18f, activeSec = 0.30f, recoverySec = 0.24f, reach = 2.45f, damage = 225f, chipDamage = 55f, knockback = 7.2f, forwardImpulse = 6.2f, causesKnockdown = true, isHeavyHit = true, guardBreak = true)
            }

            // --- DRAGON BOSS ATTACK SPECS (Generous Early-Reactable Telegraph Startup Windows: 0.62s - 0.85s!) ---
            FighterActionState.BOSS_CLAW_STRIKE -> AttackSpec(
                action, startupSec = 0.62f, activeSec = 0.18f, recoverySec = 0.26f,
                reach = 2.38f, damage = 130f, chipDamage = 22f, knockback = 4.6f, forwardImpulse = 2.4f,
                isHeavyHit = true, telegraphLevel = 1
            )
            FighterActionState.BOSS_GROUND_STOMP -> AttackSpec(
                action, startupSec = 0.80f, activeSec = 0.24f, recoverySec = 0.34f,
                reach = 2.75f, damage = 190f, chipDamage = 55f, knockback = 7.4f, forwardImpulse = 1.6f,
                causesKnockdown = true, isHeavyHit = true, guardBreak = true, telegraphLevel = 2
            )
            FighterActionState.BOSS_FIRE_BREATH -> AttackSpec(
                action, startupSec = 0.85f, activeSec = 0.56f, recoverySec = 0.31f,
                reach = 3.75f, damage = 175f, chipDamage = 32f, knockback = 5.6f, forwardImpulse = 0.3f,
                causesKnockdown = true, isHeavyHit = true, telegraphLevel = 2
            )
            FighterActionState.BOSS_TAIL_SWEEP -> AttackSpec(
                action, startupSec = 0.68f, activeSec = 0.24f, recoverySec = 0.28f,
                reach = 2.68f, damage = 148f, chipDamage = 26f, knockback = 6.0f, forwardImpulse = 1.6f,
                causesKnockdown = true, isHeavyHit = true, telegraphLevel = 1
            )

            else -> AttackSpec(action, 0.1f, 0.1f, 0.1f, 1.5f, 50f, 8f, 1.5f, 0f)
        }
    }

    companion object {
        const val ARENA_HALF_WIDTH = 4.35f
        const val ROUND_DURATION_SECONDS = 99
        const val ROUNDS_TO_WIN = 2
        const val SPECIAL_COST = 50f
    }
}
