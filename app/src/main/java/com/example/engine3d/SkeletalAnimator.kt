package com.example.engine3d

import com.example.model.FighterActionState
import com.example.model.FighterId
import com.example.model.FighterRuntimeState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class RigPose(
    val rootOffsetY: Float = 0f,
    val rootOffsetZ: Float = 0f,
    val extraYawDeg: Float = 0f,
    val pelvisLeanForward: Float = 0f,
    val pelvisRoll: Float = 0f,
    val torsoLeanForward: Float = 8f,
    val torsoTwistYaw: Float = 0f,
    val torsoRoll: Float = 0f,
    val torsoBreathScale: Float = 1f,
    val headPitchForward: Float = 0f,
    val headYaw: Float = 0f,
    // Left arm (+X side)
    val lShoulderSwingFwd: Float = 32f,
    val lShoulderOutward: Float = 16f,
    val lElbowFlex: Float = 88f,
    // Right arm (-X side, closer to camera when facing right)
    val rShoulderSwingFwd: Float = 42f,
    val rShoulderOutward: Float = 18f,
    val rElbowFlex: Float = 96f,
    // Left leg (+X side)
    val lThighSwingFwd: Float = -14f,
    val lThighOutward: Float = 6f,
    val lKneeFlex: Float = 22f,
    val lAnkleLevelComp: Float = 0f,
    // Right leg (-X side)
    val rThighSwingFwd: Float = 22f,
    val rThighOutward: Float = 8f,
    val rKneeFlex: Float = 26f,
    val rAnkleLevelComp: Float = 0f,
    // Tail & Dragon Wings
    val tailPitch: Float = 0f,
    val tailYaw: Float = 0f,
    val wingFlapDeg: Float = 0f,
    val wingSpreadDeg: Float = 0f
)

object SkeletalAnimator {

    fun evaluateBlendedPose(
        fighter: FighterRuntimeState,
        globalTime: Float
    ): RigPose {
        val current = evaluateActionPose(
            id = fighter.id,
            action = fighter.action,
            t = fighter.stateTime,
            globalTime = globalTime
        )
        if (fighter.blendFactor >= 0.99f || fighter.prevAction == fighter.action) {
            return current
        }
        val previous = evaluateActionPose(
            id = fighter.id,
            action = fighter.prevAction,
            t = 0.18f,
            globalTime = globalTime
        )
        return lerpPose(previous, current, smoothStep(fighter.blendFactor))
    }

    fun evaluateMenuIdlePose(id: FighterId, time: Float): RigPose {
        return evaluateActionPose(id, FighterActionState.IDLE, time, time)
    }

    private fun evaluateActionPose(
        id: FighterId,
        action: FighterActionState,
        t: Float,
        globalTime: Float
    ): RigPose {
        val base = baseIdlePose(id, globalTime)
        return when (action) {
            FighterActionState.IDLE -> base

            FighterActionState.WALK_FORWARD -> {
                val freq = if (id == FighterId.SHADOW_TIGER) 9.5f else 7.2f
                val cyc = sin(t * freq)
                val cosCyc = cos(t * freq)
                base.copy(
                    rootOffsetY = base.rootOffsetY + kotlin.math.abs(cosCyc) * 0.035f,
                    torsoLeanForward = base.torsoLeanForward + 7f,
                    torsoTwistYaw = cyc * 10f,
                    lThighSwingFwd = cyc * 34f + 6f,
                    lKneeFlex = (24f - cosCyc * 18f).coerceAtLeast(8f),
                    rThighSwingFwd = -cyc * 34f + 6f,
                    rKneeFlex = (24f + cosCyc * 18f).coerceAtLeast(8f),
                    lShoulderSwingFwd = base.lShoulderSwingFwd - cyc * 16f,
                    rShoulderSwingFwd = base.rShoulderSwingFwd + cyc * 16f
                )
            }

            FighterActionState.WALK_BACKWARD -> {
                val freq = 8.0f
                val cyc = sin(t * freq)
                val cosCyc = cos(t * freq)
                base.copy(
                    rootOffsetY = base.rootOffsetY + kotlin.math.abs(cosCyc) * 0.025f,
                    torsoLeanForward = base.torsoLeanForward - 4f,
                    lThighSwingFwd = -cyc * 26f + 4f,
                    lKneeFlex = (26f + cosCyc * 14f).coerceAtLeast(10f),
                    rThighSwingFwd = cyc * 26f + 4f,
                    rKneeFlex = (26f - cosCyc * 14f).coerceAtLeast(10f),
                    lShoulderSwingFwd = 48f,
                    rShoulderSwingFwd = 52f,
                    lElbowFlex = 104f,
                    rElbowFlex = 108f
                )
            }

            FighterActionState.BLOCK -> base.copy(
                rootOffsetY = base.rootOffsetY - 0.05f,
                torsoLeanForward = 14f,
                torsoTwistYaw = -12f,
                headPitchForward = 12f,
                lShoulderSwingFwd = 68f,
                rShoulderSwingFwd = 74f,
                lShoulderOutward = 8f,
                rShoulderOutward = 8f,
                lElbowFlex = 118f,
                rElbowFlex = 122f,
                lThighSwingFwd = -22f,
                rThighSwingFwd = 28f,
                lKneeFlex = 32f,
                rKneeFlex = 34f
            )

            FighterActionState.BLOCK_STUN -> {
                val recoil = sin((t / 0.22f).coerceIn(0f, 1f) * PI.toFloat())
                base.copy(
                    rootOffsetY = base.rootOffsetY - 0.06f,
                    torsoLeanForward = 14f - recoil * 14f,
                    lShoulderSwingFwd = 72f,
                    rShoulderSwingFwd = 76f,
                    lElbowFlex = 122f,
                    rElbowFlex = 124f
                )
            }

            FighterActionState.DODGE_BACK -> {
                val arc = sin((t / 0.34f).coerceIn(0f, 1f) * PI.toFloat())
                base.copy(
                    rootOffsetY = base.rootOffsetY - 0.14f * arc,
                    torsoLeanForward = -12f * arc + 6f,
                    lShoulderSwingFwd = 20f,
                    rShoulderSwingFwd = 25f,
                    lThighSwingFwd = 32f * arc,
                    rThighSwingFwd = -18f * arc,
                    lKneeFlex = 46f * arc + 18f,
                    rKneeFlex = 38f * arc + 18f
                )
            }

            FighterActionState.DODGE_FORWARD -> {
                val arc = sin((t / 0.32f).coerceIn(0f, 1f) * PI.toFloat())
                base.copy(
                    rootOffsetY = base.rootOffsetY - 0.18f * arc,
                    torsoLeanForward = 32f * arc + 10f,
                    headPitchForward = -14f * arc,
                    lShoulderSwingFwd = -15f * arc + 25f,
                    rShoulderSwingFwd = 58f * arc + 30f
                )
            }

            FighterActionState.ATTACK_PUNCH_1 -> {
                // Fast lead jab / claw strike (startup -> snap impact -> recovery)
                val ext = attackEnvelope(t, startup = 0.07f, active = 0.10f, total = 0.30f)
                val windup = windupEnvelope(t, 0.07f)
                base.copy(
                    torsoLeanForward = base.torsoLeanForward + 10f * ext,
                    torsoTwistYaw = 26f * ext - 10f * windup,
                    rShoulderSwingFwd = base.rShoulderSwingFwd - 18f * windup + 56f * ext,
                    rShoulderOutward = 10f,
                    rElbowFlex = base.rElbowFlex + 15f * windup - 82f * ext,
                    lShoulderSwingFwd = 46f,
                    lElbowFlex = 108f
                )
            }

            FighterActionState.ATTACK_PUNCH_2 -> {
                // Heavy cross punch with opposite arm & full shoulder rotation
                val ext = attackEnvelope(t, startup = 0.09f, active = 0.12f, total = 0.36f)
                val windup = windupEnvelope(t, 0.09f)
                base.copy(
                    torsoLeanForward = base.torsoLeanForward + 15f * ext,
                    torsoTwistYaw = -36f * ext + 16f * windup,
                    lShoulderSwingFwd = base.lShoulderSwingFwd - 24f * windup + 66f * ext,
                    lShoulderOutward = 8f,
                    lElbowFlex = base.lElbowFlex + 16f * windup - 80f * ext,
                    rShoulderSwingFwd = 34f,
                    rElbowFlex = 105f
                )
            }

            FighterActionState.ATTACK_PUNCH_3 -> {
                // Finisher 3rd combo hit: Tiger Rising Double Claw / Buffalo Overhead Smash
                val ext = attackEnvelope(t, startup = 0.12f, active = 0.15f, total = 0.48f)
                val windup = windupEnvelope(t, 0.12f)
                if (id == FighterId.IRON_BUFFALO) {
                    base.copy(
                        rootOffsetY = base.rootOffsetY + 0.06f * windup - 0.12f * ext,
                        torsoLeanForward = -16f * windup + 34f * ext,
                        lShoulderSwingFwd = 138f * windup + 64f * ext,
                        rShoulderSwingFwd = 138f * windup + 64f * ext,
                        lElbowFlex = 45f * windup + 22f * ext,
                        rElbowFlex = 45f * windup + 22f * ext
                    )
                } else {
                    base.copy(
                        rootOffsetY = base.rootOffsetY - 0.08f * windup + 0.16f * ext,
                        torsoLeanForward = 18f * windup - 8f * ext,
                        torsoTwistYaw = 38f * ext,
                        rShoulderSwingFwd = -22f * windup + 122f * ext,
                        rElbowFlex = 105f * windup + 28f * ext,
                        lShoulderSwingFwd = 55f * ext + 25f
                    )
                }
            }

            FighterActionState.ATTACK_KICK_1 -> {
                // Snappy High / Mid Martial Kick
                val ext = attackEnvelope(t, startup = 0.11f, active = 0.14f, total = 0.44f)
                val windup = windupEnvelope(t, 0.11f)
                base.copy(
                    torsoLeanForward = base.torsoLeanForward - 18f * ext,
                    torsoTwistYaw = -24f * ext,
                    rThighSwingFwd = 45f * windup + 96f * ext,
                    rThighOutward = 18f * ext,
                    rKneeFlex = 78f * windup + 8f * (1f - ext),
                    lThighSwingFwd = -8f,
                    lKneeFlex = 18f,
                    lShoulderSwingFwd = 52f,
                    rShoulderSwingFwd = 18f
                )
            }

            FighterActionState.ATTACK_KICK_2 -> {
                // Spinning Heel Kick (Tiger) / Seismic Stomp Kick (Buffalo)
                val prog = (t / 0.52f).coerceIn(0f, 1f)
                val ext = attackEnvelope(t, startup = 0.14f, active = 0.16f, total = 0.52f)
                if (id == FighterId.SHADOW_TIGER) {
                    base.copy(
                        rootOffsetY = sin(prog * PI.toFloat()) * 0.18f,
                        extraYawDeg = smoothStep(prog) * 360f,
                        torsoLeanForward = -14f * ext,
                        lThighSwingFwd = 98f * ext,
                        lKneeFlex = 12f,
                        rThighSwingFwd = -16f,
                        rKneeFlex = 36f
                    )
                } else {
                    val lift = windupEnvelope(t, 0.16f)
                    base.copy(
                        rootOffsetY = -0.08f * ext,
                        torsoLeanForward = 20f * ext - 8f * lift,
                        rThighSwingFwd = 72f * lift + 58f * ext,
                        rKneeFlex = 68f * lift + 14f * ext,
                        lShoulderSwingFwd = 62f,
                        rShoulderSwingFwd = 62f
                    )
                }
            }

            FighterActionState.ATTACK_SPECIAL -> {
                val prog = (t / 0.72f).coerceIn(0f, 1f)
                val ext = attackEnvelope(t, startup = 0.18f, active = 0.30f, total = 0.72f)
                val windup = windupEnvelope(t, 0.18f)
                if (id == FighterId.SHADOW_TIGER) {
                    // Blazing Tornado Claw: airborne 720-degree fiery claw cyclone!
                    base.copy(
                        rootOffsetY = sin(prog * PI.toFloat()) * 0.28f,
                        extraYawDeg = smoothStep(prog) * 720f,
                        torsoLeanForward = 16f,
                        lShoulderSwingFwd = 88f * ext + 30f,
                        rShoulderSwingFwd = 92f * ext + 30f,
                        lShoulderOutward = 55f * ext + 15f,
                        rShoulderOutward = 55f * ext + 15f,
                        lElbowFlex = 32f,
                        rElbowFlex = 32f,
                        rThighSwingFwd = 65f * ext,
                        rKneeFlex = 42f
                    )
                } else {
                    // Iron Buffalo: Tectonic Horn Stampede (head lowered horns-first charge + slam!)
                    base.copy(
                        rootOffsetY = -0.10f * ext,
                        torsoLeanForward = -14f * windup + 46f * ext,
                        headPitchForward = -18f * windup + 28f * ext,
                        lShoulderSwingFwd = -22f * ext + 45f * windup,
                        rShoulderSwingFwd = -22f * ext + 45f * windup,
                        lShoulderOutward = 34f,
                        rShoulderOutward = 34f,
                        lThighSwingFwd = -32f * sin(t * 20f) * ext,
                        rThighSwingFwd = 32f * sin(t * 20f) * ext,
                        lKneeFlex = 35f,
                        rKneeFlex = 35f
                    )
                }
            }

            // --- DRAGON BOSS DISTINCT TELEGRAPHED ATTACK PATTERNS ---
            FighterActionState.BOSS_CLAW_STRIKE -> {
                // Pattern 1: Powerful Claw Swipe (0.62s clear high-arm coil telegraph -> diagonal talon cleave!)
                val windup = windupEnvelope(t, 0.62f)
                val ext = attackEnvelope(t, startup = 0.62f, active = 0.18f, total = 1.06f)
                base.copy(
                    torsoLeanForward = -24f * windup + 32f * ext,
                    torsoTwistYaw = -44f * windup + 48f * ext,
                    headPitchForward = -12f * windup + 10f * ext,
                    rShoulderSwingFwd = 148f * windup + 76f * ext,
                    rShoulderOutward = 46f * windup + 10f,
                    rElbowFlex = 36f * windup + 16f * ext,
                    lShoulderSwingFwd = 18f * (1f - windup),
                    wingSpreadDeg = 34f * windup + 18f * ext
                )
            }

            FighterActionState.BOSS_TAIL_SWEEP -> {
                // Pattern 4: Sweeping Spiked Tail Whip (0.68s sideways coil telegraph -> 360-degree whip!)
                val prog = (t / 1.20f).coerceIn(0f, 1f)
                val windup = windupEnvelope(t, 0.68f)
                val spin = if (t < 0.68f) -48f * windup else smoothStep(((t - 0.68f) / 0.36f).coerceIn(0f, 1f)) * 360f - 48f
                base.copy(
                    rootOffsetY = -0.08f,
                    extraYawDeg = spin,
                    torsoLeanForward = 22f,
                    tailYaw = -36f * windup + 45f * sin(prog * PI.toFloat() * 2f),
                    tailPitch = 28f * windup + 12f,
                    wingSpreadDeg = 36f
                )
            }

            FighterActionState.BOSS_GROUND_STOMP -> {
                // Pattern 2: Ground-Shattering Stomp (0.80s high single-leg stomp raise + wing flare -> seismic foot crush!)
                val windup = windupEnvelope(t, 0.80f)
                val ext = attackEnvelope(t, startup = 0.80f, active = 0.24f, total = 1.38f)
                base.copy(
                    rootOffsetY = 0.10f * windup - 0.16f * ext,
                    torsoLeanForward = -26f * windup + 34f * ext,
                    torsoRoll = 12f * windup - 6f * ext,
                    headPitchForward = -18f * windup + 22f * ext,
                    rThighSwingFwd = 98f * windup + 28f * ext,
                    rThighOutward = 18f * windup + 14f,
                    rKneeFlex = 68f * windup + 18f * ext,
                    lThighSwingFwd = -18f,
                    lKneeFlex = 24f + 18f * ext,
                    lShoulderSwingFwd = 118f * windup + 42f * ext,
                    rShoulderSwingFwd = 118f * windup + 42f * ext,
                    lShoulderOutward = 44f * windup + 24f,
                    rShoulderOutward = 44f * windup + 24f,
                    wingFlapDeg = 38f * windup - 26f * ext,
                    wingSpreadDeg = 54f * windup + 22f * ext
                )
            }

            FighterActionState.BOSS_FIRE_BREATH -> {
                // Pattern 3: Devastating Fire-Breath (0.85s deep molten chest inhale -> 0.56s sustained forward flame torrent!)
                val inhale = windupEnvelope(t, 0.85f)
                val exhale = attackEnvelope(t, startup = 0.85f, active = 0.56f, total = 1.72f)
                base.copy(
                    torsoLeanForward = -28f * inhale + 34f * exhale,
                    torsoBreathScale = 1f + 0.24f * inhale - 0.06f * exhale,
                    headPitchForward = -32f * inhale + 20f * exhale,
                    lShoulderSwingFwd = -22f * exhale + 22f * inhale,
                    rShoulderSwingFwd = -22f * exhale + 22f * inhale,
                    lShoulderOutward = 42f,
                    rShoulderOutward = 42f,
                    wingSpreadDeg = 56f * inhale + 38f * exhale,
                    wingFlapDeg = 20f * inhale - 12f * exhale
                )
            }

            FighterActionState.HIT_LIGHT -> {
                val recoil = sin((t / 0.28f).coerceIn(0f, 1f) * PI.toFloat())
                base.copy(
                    torsoLeanForward = base.torsoLeanForward - 22f * recoil,
                    headPitchForward = -24f * recoil,
                    torsoTwistYaw = -18f * recoil,
                    lShoulderSwingFwd = 16f,
                    rShoulderSwingFwd = 18f,
                    lElbowFlex = 62f,
                    rElbowFlex = 64f
                )
            }

            FighterActionState.HIT_HEAVY -> {
                val recoil = sin((t / 0.42f).coerceIn(0f, 1f) * PI.toFloat())
                base.copy(
                    rootOffsetY = base.rootOffsetY - 0.06f * recoil,
                    torsoLeanForward = -36f * recoil,
                    headPitchForward = -32f * recoil,
                    torsoTwistYaw = 28f * recoil,
                    lShoulderSwingFwd = -12f * recoil + 20f,
                    rShoulderSwingFwd = -14f * recoil + 20f,
                    lShoulderOutward = 38f * recoil + 14f,
                    rShoulderOutward = 38f * recoil + 14f
                )
            }

            FighterActionState.KNOCKDOWN -> {
                val fallProg = (t / 0.45f).coerceIn(0f, 1f)
                val airArc = sin(fallProg * PI.toFloat()) * 0.32f
                base.copy(
                    rootOffsetY = -0.68f * smoothStep(fallProg) + airArc,
                    pelvisLeanForward = -82f * smoothStep(fallProg),
                    torsoLeanForward = -10f,
                    lShoulderSwingFwd = 15f,
                    rShoulderSwingFwd = 15f,
                    lShoulderOutward = 42f,
                    rShoulderOutward = 42f,
                    lElbowFlex = 35f,
                    rElbowFlex = 35f,
                    lThighSwingFwd = 18f,
                    rThighSwingFwd = 10f,
                    lKneeFlex = 24f,
                    rKneeFlex = 18f
                )
            }

            FighterActionState.GET_UP -> {
                val upProg = smoothStep((t / 0.46f).coerceIn(0f, 1f))
                val inv = 1f - upProg
                base.copy(
                    rootOffsetY = -0.62f * inv,
                    pelvisLeanForward = -75f * inv,
                    torsoLeanForward = 24f * sin(upProg * PI.toFloat()) + base.torsoLeanForward * upProg,
                    lShoulderSwingFwd = 55f * inv + base.lShoulderSwingFwd * upProg,
                    rShoulderSwingFwd = 55f * inv + base.rShoulderSwingFwd * upProg
                )
            }

            FighterActionState.VICTORY -> {
                when (id) {
                    FighterId.SHADOW_TIGER -> {
                        // Agile spin into skyward champion claw uppercut!
                        val introProg = (t / 0.65f).coerceIn(0f, 1f)
                        val pulse = sin(t * 4.5f) * 4f
                        base.copy(
                            extraYawDeg = smoothStep(introProg) * 360f - 28f,
                            torsoLeanForward = -8f,
                            headPitchForward = -14f,
                            rShoulderSwingFwd = 152f + pulse,
                            rShoulderOutward = 16f,
                            rElbowFlex = 24f,
                            lShoulderSwingFwd = 38f,
                            lElbowFlex = 98f,
                            lThighSwingFwd = -18f,
                            rThighSwingFwd = 24f
                        )
                    }
                    FighterId.IRON_BUFFALO -> {
                        // Massive war-bellow with both armored arms raised skyward!
                        val beat = sin(t * 7.0f)
                        base.copy(
                            extraYawDeg = -25f,
                            torsoLeanForward = -12f + beat * 4f,
                            torsoBreathScale = 1.08f + beat * 0.04f,
                            headPitchForward = -18f,
                            lShoulderSwingFwd = 135f + beat * 10f,
                            rShoulderSwingFwd = 135f - beat * 10f,
                            lShoulderOutward = 42f,
                            rShoulderOutward = 42f,
                            lElbowFlex = 52f,
                            rElbowFlex = 52f
                        )
                    }
                    FighterId.DRAGON_BOSS -> {
                        val roar = sin(t * 5.0f)
                        base.copy(
                            torsoLeanForward = -16f,
                            headPitchForward = -22f,
                            wingSpreadDeg = 52f,
                            wingFlapDeg = 22f * roar,
                            lShoulderOutward = 44f,
                            rShoulderOutward = 44f
                        )
                    }
                }
            }

            FighterActionState.DEFEAT -> {
                val collapse = smoothStep((t / 0.55f).coerceIn(0f, 1f))
                base.copy(
                    rootOffsetY = -0.66f * collapse,
                    pelvisLeanForward = -78f * collapse,
                    torsoLeanForward = -12f * collapse,
                    headPitchForward = 18f * collapse,
                    lShoulderOutward = 45f * collapse,
                    rShoulderOutward = 45f * collapse,
                    lElbowFlex = 28f,
                    rElbowFlex = 28f,
                    lKneeFlex = 20f,
                    rKneeFlex = 16f
                )
            }

            FighterActionState.BOSS_INTRO -> {
                // Dragon descends from stadium rafters, lands heavily, spreads wings, and roars!
                val drop = (1f - (t / 0.65f).coerceIn(0f, 1f))
                val roarPhase = ((t - 0.65f) / 1.35f).coerceIn(0f, 1f)
                val roarArc = sin(roarPhase * PI.toFloat())
                base.copy(
                    rootOffsetY = drop * drop * 2.4f - (if (t in 0.65f..0.85f) 0.14f else 0f),
                    torsoLeanForward = 22f * roarArc - 10f * drop,
                    torsoBreathScale = 1f + 0.14f * roarArc,
                    headPitchForward = -24f * roarArc,
                    lShoulderSwingFwd = -16f * roarArc + 24f,
                    rShoulderSwingFwd = -16f * roarArc + 24f,
                    lShoulderOutward = 45f * roarArc + 20f,
                    rShoulderOutward = 45f * roarArc + 20f,
                    wingSpreadDeg = 56f * roarArc + 24f * drop,
                    wingFlapDeg = 32f * sin(t * 8f) * drop + 18f * roarArc
                )
            }
        }
    }

    private fun baseIdlePose(id: FighterId, time: Float): RigPose {
        val breathe = sin(time * 3.1f)
        val sway = cos(time * 1.8f)
        return when (id) {
            FighterId.SHADOW_TIGER -> RigPose(
                rootOffsetY = -0.05f + breathe * 0.015f,
                torsoLeanForward = 11f + breathe * 1.5f,
                torsoTwistYaw = -12f + sway * 2.5f,
                torsoBreathScale = 1f + breathe * 0.022f,
                headPitchForward = -4f,
                headYaw = 10f,
                lShoulderSwingFwd = 46f + breathe * 3f,
                lShoulderOutward = 14f,
                lElbowFlex = 102f,
                rShoulderSwingFwd = 34f - breathe * 3f,
                rShoulderOutward = 18f,
                rElbowFlex = 94f,
                lThighSwingFwd = -18f,
                lThighOutward = 8f,
                lKneeFlex = 24f,
                rThighSwingFwd = 24f,
                rThighOutward = 10f,
                rKneeFlex = 28f,
                tailPitch = sin(time * 2.6f) * 8f,
                tailYaw = cos(time * 2.1f) * 18f
            )

            FighterId.IRON_BUFFALO -> RigPose(
                rootOffsetY = -0.06f + breathe * 0.012f,
                torsoLeanForward = 15f + breathe * 1.8f,
                torsoTwistYaw = -8f + sway * 1.8f,
                torsoBreathScale = 1f + breathe * 0.030f,
                headPitchForward = 6f,
                headYaw = 6f,
                lShoulderSwingFwd = 36f + breathe * 2.5f,
                lShoulderOutward = 22f,
                lElbowFlex = 88f,
                rShoulderSwingFwd = 42f - breathe * 2.5f,
                rShoulderOutward = 24f,
                rElbowFlex = 92f,
                lThighSwingFwd = -16f,
                lThighOutward = 11f,
                lKneeFlex = 26f,
                rThighSwingFwd = 20f,
                rThighOutward = 12f,
                rKneeFlex = 28f,
                tailPitch = 0f,
                tailYaw = sin(time * 3.0f) * 10f
            )

            FighterId.DRAGON_BOSS -> RigPose(
                rootOffsetY = -0.05f + breathe * 0.022f,
                torsoLeanForward = 14f + breathe * 2f,
                torsoTwistYaw = -6f + sway * 3f,
                torsoBreathScale = 1f + breathe * 0.038f,
                headPitchForward = 4f - breathe * 3f,
                headYaw = 5f,
                lShoulderSwingFwd = 28f + breathe * 4f,
                lShoulderOutward = 24f,
                lElbowFlex = 72f,
                rShoulderSwingFwd = 32f - breathe * 4f,
                rShoulderOutward = 26f,
                rElbowFlex = 76f,
                lThighSwingFwd = -14f,
                lThighOutward = 12f,
                lKneeFlex = 26f,
                rThighSwingFwd = 18f,
                rThighOutward = 12f,
                rKneeFlex = 28f,
                tailPitch = sin(time * 2.2f) * 6f,
                tailYaw = cos(time * 1.9f) * 20f,
                wingFlapDeg = breathe * 9f,
                wingSpreadDeg = 14f + breathe * 4f
            )
        }
    }

    private fun windupEnvelope(t: Float, startup: Float): Float {
        if (t >= startup || startup <= 0.001f) return 0f
        return sin((t / startup).coerceIn(0f, 1f) * PI.toFloat())
    }

    private fun attackEnvelope(t: Float, startup: Float, active: Float, total: Float): Float {
        return when {
            t < startup * 0.65f -> 0f
            t < startup -> smoothStep((t - startup * 0.65f) / (startup * 0.35f))
            t <= startup + active -> 1f
            t < total -> 1f - smoothStep((t - (startup + active)) / (total - (startup + active)).coerceAtLeast(0.01f))
            else -> 0f
        }
    }

    private fun smoothStep(x: Float): Float {
        val c = x.coerceIn(0f, 1f)
        return c * c * (3f - 2f * c)
    }

    private fun lerp(a: Float, b: Float, f: Float): Float = a + (b - a) * f

    private fun lerpPose(a: RigPose, b: RigPose, f: Float): RigPose = RigPose(
        rootOffsetY = lerp(a.rootOffsetY, b.rootOffsetY, f),
        rootOffsetZ = lerp(a.rootOffsetZ, b.rootOffsetZ, f),
        extraYawDeg = lerp(a.extraYawDeg, b.extraYawDeg, f),
        pelvisLeanForward = lerp(a.pelvisLeanForward, b.pelvisLeanForward, f),
        pelvisRoll = lerp(a.pelvisRoll, b.pelvisRoll, f),
        torsoLeanForward = lerp(a.torsoLeanForward, b.torsoLeanForward, f),
        torsoTwistYaw = lerp(a.torsoTwistYaw, b.torsoTwistYaw, f),
        torsoRoll = lerp(a.torsoRoll, b.torsoRoll, f),
        torsoBreathScale = lerp(a.torsoBreathScale, b.torsoBreathScale, f),
        headPitchForward = lerp(a.headPitchForward, b.headPitchForward, f),
        headYaw = lerp(a.headYaw, b.headYaw, f),
        lShoulderSwingFwd = lerp(a.lShoulderSwingFwd, b.lShoulderSwingFwd, f),
        lShoulderOutward = lerp(a.lShoulderOutward, b.lShoulderOutward, f),
        lElbowFlex = lerp(a.lElbowFlex, b.lElbowFlex, f),
        rShoulderSwingFwd = lerp(a.rShoulderSwingFwd, b.rShoulderSwingFwd, f),
        rShoulderOutward = lerp(a.rShoulderOutward, b.rShoulderOutward, f),
        rElbowFlex = lerp(a.rElbowFlex, b.rElbowFlex, f),
        lThighSwingFwd = lerp(a.lThighSwingFwd, b.lThighSwingFwd, f),
        lThighOutward = lerp(a.lThighOutward, b.lThighOutward, f),
        lKneeFlex = lerp(a.lKneeFlex, b.lKneeFlex, f),
        lAnkleLevelComp = lerp(a.lAnkleLevelComp, b.lAnkleLevelComp, f),
        rThighSwingFwd = lerp(a.rThighSwingFwd, b.rThighSwingFwd, f),
        rThighOutward = lerp(a.rThighOutward, b.rThighOutward, f),
        rKneeFlex = lerp(a.rKneeFlex, b.rKneeFlex, f),
        rAnkleLevelComp = lerp(a.rAnkleLevelComp, b.rAnkleLevelComp, f),
        tailPitch = lerp(a.tailPitch, b.tailPitch, f),
        tailYaw = lerp(a.tailYaw, b.tailYaw, f),
        wingFlapDeg = lerp(a.wingFlapDeg, b.wingFlapDeg, f),
        wingSpreadDeg = lerp(a.wingSpreadDeg, b.wingSpreadDeg, f)
    )
}
