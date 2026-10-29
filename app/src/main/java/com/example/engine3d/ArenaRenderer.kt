package com.example.engine3d

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.model.CombatSnapshot
import com.example.model.FighterActionState
import com.example.model.FighterId
import com.example.model.FighterRuntimeState
import com.example.model.GraphicsQuality
import com.example.model.MatchPhase
import com.example.model.ParticleEvent
import com.example.model.ParticleKind
import java.util.concurrent.ConcurrentLinkedQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class ViewportSceneMode {
    MAIN_MENU_SHOWCASE,
    ROSTER_PREVIEW,
    COMBAT_ARENA
}

private class LiveParticle(
    var active: Boolean = false,
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var vz: Float = 0f,
    var r: Float = 1f,
    var g: Float = 0.6f,
    var b: Float = 0.2f,
    var size: Float = 0.08f,
    var age: Float = 0f,
    var maxLife: Float = 0.5f
)

class ArenaRenderer : GLSurfaceView.Renderer {

    @Volatile var sceneMode: ViewportSceneMode = ViewportSceneMode.MAIN_MENU_SHOWCASE
    @Volatile var combatSnapshot: CombatSnapshot = CombatSnapshot()
    @Volatile var rosterFighter: FighterId = FighterId.SHADOW_TIGER
    @Volatile var rosterYawDeg: Float = 18f
    @Volatile var graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH
    @Volatile var cameraSensitivity: Float = 1.0f

    private val pendingParticleEvents = ConcurrentLinkedQueue<ParticleEvent>()
    private val particles = Array(140) { LiveParticle() }
    private val rng = Random(9917)

    private var programId = 0
    private var uMVPMatrixLoc = 0
    private var uModelMatrixLoc = 0
    private var uCameraPosLoc = 0
    private var uTimeLoc = 0
    private var uTintFlashLoc = 0
    private var uFlashAmountLoc = 0
    private var uAlphaLoc = 0
    private var uFloorGlowOnLoc = 0
    private var uBossPhase2Loc = 0

    private var aPositionLoc = 0
    private var aNormalLoc = 0
    private var aUVLoc = 0
    private var aMaterialIdLoc = 0
    private var aAOLoc = 0

    private var builtRadialSegments = -1
    private val rigCache = HashMap<FighterId, CharacterRigMeshSet>()
    private var arenaFloorMesh: CompiledMesh? = null
    private var arenaBackdropMesh: CompiledMesh? = null
    private var contactShadowMesh: CompiledMesh? = null
    private var particleQuadMesh: CompiledMesh? = null
    private var telegraphDiscMesh: CompiledMesh? = null
    private var fireBeamConeMesh: CompiledMesh? = null

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Reusable matrix scratch buffers (render thread only)
    private val mRoot = FloatArray(16)
    private val mPelvis = FloatArray(16)
    private val mTorso = FloatArray(16)
    private val mTorsoDraw = FloatArray(16)
    private val mHead = FloatArray(16)
    private val mLimbA = FloatArray(16)
    private val mLimbB = FloatArray(16)
    private val mLimbC = FloatArray(16)
    private val mScratch = FloatArray(16)

    private var lastFrameNs = 0L
    private var renderTimeSec = 0f
    private var smoothCamX = 0f
    private var smoothCamY = 1.95f
    private var smoothCamZ = 6.2f
    private var smoothLookX = 0f
    private var smoothLookY = 1.15f
    private var emberSpawnTimer = 0f

    fun enqueueParticles(events: List<ParticleEvent>) {
        for (ev in events) {
            pendingParticleEvents.offer(ev)
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.035f, 0.042f, 0.070f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)

        programId = ArenaShaders.createProgram()
        uMVPMatrixLoc = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
        uModelMatrixLoc = GLES20.glGetUniformLocation(programId, "uModelMatrix")
        uCameraPosLoc = GLES20.glGetUniformLocation(programId, "uCameraPos")
        uTimeLoc = GLES20.glGetUniformLocation(programId, "uTime")
        uTintFlashLoc = GLES20.glGetUniformLocation(programId, "uTintFlash")
        uFlashAmountLoc = GLES20.glGetUniformLocation(programId, "uFlashAmount")
        uAlphaLoc = GLES20.glGetUniformLocation(programId, "uAlpha")
        uFloorGlowOnLoc = GLES20.glGetUniformLocation(programId, "uFloorGlowOn")
        uBossPhase2Loc = GLES20.glGetUniformLocation(programId, "uBossPhase2")

        aPositionLoc = GLES20.glGetAttribLocation(programId, "aPosition")
        aNormalLoc = GLES20.glGetAttribLocation(programId, "aNormal")
        aUVLoc = GLES20.glGetAttribLocation(programId, "aUV")
        aMaterialIdLoc = GLES20.glGetAttribLocation(programId, "aMaterialId")
        aAOLoc = GLES20.glGetAttribLocation(programId, "aAO")

        builtRadialSegments = -1
        rebuildMeshesIfNeeded()
        lastFrameNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        val safeW = width.coerceAtLeast(1)
        val safeH = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, safeW, safeH)
        val aspect = safeW.toFloat() / safeH.toFloat()
        val fovY = if (aspect < 1.1f) 52f else 40f
        Matrix.perspectiveM(projMatrix, 0, fovY, aspect, 0.25f, 38f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastFrameNs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastFrameNs = now
        renderTimeSec += dt

        rebuildMeshesIfNeeded()
        drainAndSimulateParticles(dt)

        val snap = combatSnapshot
        val isBossPhase2 = if (sceneMode == ViewportSceneMode.COMBAT_ARENA &&
            snap.opponent.id == FighterId.DRAGON_BOSS &&
            snap.opponent.bossPhase >= 2
        ) 1f else 0f

        if (isBossPhase2 > 0.5f) {
            GLES20.glClearColor(0.085f, 0.028f, 0.024f, 1.0f)
        } else {
            GLES20.glClearColor(0.035f, 0.042f, 0.070f, 1.0f)
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        GLES20.glUseProgram(programId)
        GLES20.glUniform1f(uTimeLoc, renderTimeSec)
        GLES20.glUniform1f(
            uFloorGlowOnLoc,
            if (graphicsQuality.enableFloorGridGlow) 1f else 0f
        )
        GLES20.glUniform1f(uBossPhase2Loc, isBossPhase2)

        updateCamera(dt, snap)
        Matrix.setLookAtM(
            viewMatrix, 0,
            smoothCamX, smoothCamY, smoothCamZ,
            smoothLookX, smoothLookY, 0f,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)
        GLES20.glUniform3f(uCameraPosLoc, smoothCamX, smoothCamY, smoothCamZ)

        // 1. Opaque Pass: Underground Combat Stadium Floor & Pillars
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDepthMask(true)
        Matrix.setIdentityM(mScratch, 0)
        drawMesh(arenaFloorMesh, mScratch, 0f, 0f, 0f, 0f, 1f)
        drawMesh(arenaBackdropMesh, mScratch, 0f, 0f, 0f, 0f, 1f)

        // 2. Opaque Pass: Rigged 3D Animal Warriors
        when (sceneMode) {
            ViewportSceneMode.MAIN_MENU_SHOWCASE -> {
                val tigerState = FighterRuntimeState(
                    id = FighterId.SHADOW_TIGER,
                    x = -1.25f,
                    facing = 1f,
                    stateTime = renderTimeSec
                )
                val buffaloState = FighterRuntimeState(
                    id = FighterId.IRON_BUFFALO,
                    x = 1.25f,
                    facing = -1f,
                    stateTime = renderTimeSec + 0.8f
                )
                renderCharacter(tigerState, customYaw = 62f)
                renderCharacter(buffaloState, customYaw = -62f)
                renderContactShadows(
                    listOf(
                        Triple(-1.25f, 0.25f, 0.78f),
                        Triple(1.25f, -0.15f, 0.92f)
                    )
                )
            }

            ViewportSceneMode.ROSTER_PREVIEW -> {
                val previewFighter = if (rosterFighter.isPlayable) rosterFighter else FighterId.SHADOW_TIGER
                val state = FighterRuntimeState(
                    id = previewFighter,
                    x = 0.65f,
                    facing = 1f,
                    stateTime = renderTimeSec
                )
                renderCharacter(state, customYaw = rosterYawDeg)
                renderContactShadows(listOf(Triple(0.65f, 0f, 0.88f)))
            }

            ViewportSceneMode.COMBAT_ARENA -> {
                renderCharacter(snap.player, customYaw = null)
                renderCharacter(snap.opponent, customYaw = null)
                val pScale = if (snap.player.id == FighterId.IRON_BUFFALO) 0.92f else 0.78f
                val oScale = when (snap.opponent.id) {
                    FighterId.DRAGON_BOSS -> 1.32f
                    FighterId.IRON_BUFFALO -> 0.92f
                    FighterId.SHADOW_TIGER -> 0.78f
                }
                renderContactShadows(
                    listOf(
                        Triple(snap.player.x, 0f, pScale),
                        Triple(snap.opponent.x, 0f, oScale)
                    )
                )
                renderDragonBoss3DTelegraphsAndEffects(snap)
            }
        }

        // 3. Transparent / Additive Combat Particles Pass
        renderParticles()
    }

    private fun updateCamera(dt: Float, snap: CombatSnapshot) {
        val lerpRate = (dt * 7.5f * cameraSensitivity).coerceIn(0.05f, 0.65f)
        when (sceneMode) {
            ViewportSceneMode.MAIN_MENU_SHOWCASE -> {
                val orbitX = sin(renderTimeSec * 0.42f) * 0.75f - 0.55f
                val targetZ = 5.45f + cos(renderTimeSec * 0.35f) * 0.25f
                smoothCamX += (orbitX - smoothCamX) * lerpRate
                smoothCamY += (1.72f - smoothCamY) * lerpRate
                smoothCamZ += (targetZ - smoothCamZ) * lerpRate
                smoothLookX += (-0.35f - smoothLookX) * lerpRate
                smoothLookY += (1.15f - smoothLookY) * lerpRate
            }

            ViewportSceneMode.ROSTER_PREVIEW -> {
                smoothCamX += (0.15f - smoothCamX) * lerpRate
                smoothCamY += (1.48f - smoothCamY) * lerpRate
                smoothCamZ += (3.85f - smoothCamZ) * lerpRate
                smoothLookX += (0.15f - smoothLookX) * lerpRate
                smoothLookY += (1.06f - smoothLookY) * lerpRate
            }

            ViewportSceneMode.COMBAT_ARENA -> {
                when (snap.matchPhase) {
                    MatchPhase.BOSS_ENTRANCE -> {
                        // Dramatic low-angle camera sweep onto the descending Dragon Boss
                        val targetX = snap.opponent.x - 0.8f
                        smoothCamX += (targetX - smoothCamX) * lerpRate
                        smoothCamY += (1.55f - smoothCamY) * lerpRate
                        smoothCamZ += (4.95f - smoothCamZ) * lerpRate
                        smoothLookX += (snap.opponent.x - smoothLookX) * lerpRate
                        smoothLookY += (1.52f - smoothLookY) * lerpRate
                    }
                    MatchPhase.MATCH_VICTORY, MatchPhase.MATCH_DEFEAT -> {
                        val winner = if (snap.matchPhase == MatchPhase.MATCH_VICTORY) snap.player else snap.opponent
                        smoothCamX += (winner.x * 0.85f - smoothCamX) * lerpRate
                        smoothCamY += (1.52f - smoothCamY) * lerpRate
                        smoothCamZ += (4.25f - smoothCamZ) * lerpRate
                        smoothLookX += (winner.x - smoothLookX) * lerpRate
                        smoothLookY += (1.18f - smoothLookY) * lerpRate
                    }
                    else -> {
                        val midX = (snap.player.x + snap.opponent.x) * 0.5f
                        val separation = kotlin.math.abs(snap.player.x - snap.opponent.x)
                        val bossExtraZ = if (snap.opponent.id == FighterId.DRAGON_BOSS) 0.72f else 0f
                        val desiredZ = (4.65f + separation * 0.52f + bossExtraZ).coerceIn(4.8f, 7.8f)
                        val desiredY = if (snap.opponent.id == FighterId.DRAGON_BOSS) 2.02f else 1.82f
                        val shakeX = if (snap.cameraShakeIntensity > 0.01f) {
                            (rng.nextFloat() * 2f - 1f) * snap.cameraShakeIntensity * 0.22f
                        } else 0f
                        val shakeY = if (snap.cameraShakeIntensity > 0.01f) {
                            (rng.nextFloat() * 2f - 1f) * snap.cameraShakeIntensity * 0.18f
                        } else 0f

                        smoothCamX += (midX - smoothCamX) * lerpRate + shakeX
                        smoothCamY += (desiredY - smoothCamY) * lerpRate + shakeY
                        smoothCamZ += (desiredZ - smoothCamZ) * lerpRate
                        smoothLookX += (midX - smoothLookX) * lerpRate
                        smoothLookY += ((if (snap.opponent.id == FighterId.DRAGON_BOSS) 1.28f else 1.10f) - smoothLookY) * lerpRate
                    }
                }
            }
        }
    }

    private fun renderCharacter(fighter: FighterRuntimeState, customYaw: Float?) {
        val rig = rigCache[fighter.id] ?: return
        val pose = SkeletalAnimator.evaluateBlendedPose(fighter, renderTimeSec)

        // Determine hit flash or Boss Telegraph warning glow
        var flashR = 1f; var flashG = 1f; var flashB = 1f; var flashAmt = 0f
        if ((fighter.action == FighterActionState.HIT_LIGHT || fighter.action == FighterActionState.HIT_HEAVY) &&
            fighter.stateTime < 0.12f
        ) {
            flashR = 1f; flashG = 0.92f; flashB = 0.80f
            flashAmt = (1f - fighter.stateTime / 0.12f) * 0.65f
        } else if (fighter.id == FighterId.DRAGON_BOSS && fighter.action.isAttacking) {
            // Color-coded Dragon Boss Telegraph Aura across the full early startup window!
            val startupWindow = when (fighter.action) {
                FighterActionState.BOSS_CLAW_STRIKE -> 0.62f
                FighterActionState.BOSS_TAIL_SWEEP -> 0.68f
                FighterActionState.BOSS_GROUND_STOMP -> 0.80f
                FighterActionState.BOSS_FIRE_BREATH -> 0.85f
                else -> 0.45f
            }
            if (fighter.stateTime <= startupWindow) {
                val pulse = 0.45f + 0.55f * kotlin.math.abs(sin(fighter.stateTime * 22f))
                if (fighter.action == FighterActionState.BOSS_GROUND_STOMP) {
                    // Intense Crimson-Gold Unblockable Warning Glow
                    flashR = 1.0f; flashG = 0.16f; flashB = 0.12f
                    flashAmt = pulse * 0.68f
                } else if (fighter.action == FighterActionState.BOSS_FIRE_BREATH) {
                    // Molten Hellfire Charge Glow
                    flashR = 1.0f; flashG = 0.48f; flashB = 0.06f
                    flashAmt = pulse * 0.64f
                } else {
                    // Bright Amber-Gold Blockable Strike Telegraph
                    flashR = 1.0f; flashG = 0.68f; flashB = 0.12f
                    flashAmt = pulse * 0.56f
                }
            }
        } else if (fighter.action == FighterActionState.ATTACK_SPECIAL) {
            if (fighter.id == FighterId.SHADOW_TIGER) {
                flashR = 1f; flashG = 0.55f; flashB = 0.12f; flashAmt = 0.32f
            } else if (fighter.id == FighterId.IRON_BUFFALO) {
                flashR = 0.28f; flashG = 0.78f; flashB = 1.0f; flashAmt = 0.30f
            }
        }

        val zOffset = if (sceneMode == ViewportSceneMode.MAIN_MENU_SHOWCASE) {
            if (fighter.id == FighterId.SHADOW_TIGER) 0.25f else -0.15f
        } else 0f

        val baseYaw = customYaw ?: if (fighter.facing >= 0f) 80f else -80f

        Matrix.setIdentityM(mRoot, 0)
        Matrix.translateM(mRoot, 0, fighter.x, 0f, zOffset + pose.rootOffsetZ)
        Matrix.rotateM(mRoot, 0, baseYaw + pose.extraYawDeg, 0f, 1f, 0f)

        // Pelvis
        System.arraycopy(mRoot, 0, mPelvis, 0, 16)
        Matrix.translateM(mPelvis, 0, 0f, rig.hipHeight + pose.rootOffsetY, 0f)
        Matrix.rotateM(mPelvis, 0, pose.pelvisLeanForward, 1f, 0f, 0f)
        Matrix.rotateM(mPelvis, 0, pose.pelvisRoll, 0f, 0f, 1f)
        drawMesh(rig.pelvis, mPelvis, flashR, flashG, flashB, flashAmt, 1f)

        // Tail (-Z)
        if (rig.tail != null) {
            System.arraycopy(mPelvis, 0, mLimbA, 0, 16)
            Matrix.rotateM(mLimbA, 0, pose.tailYaw, 0f, 1f, 0f)
            Matrix.rotateM(mLimbA, 0, pose.tailPitch, 1f, 0f, 0f)
            drawMesh(rig.tail, mLimbA, flashR, flashG, flashB, flashAmt, 1f)
        }

        // Torso (+Y)
        System.arraycopy(mPelvis, 0, mTorso, 0, 16)
        Matrix.translateM(mTorso, 0, 0f, 0.04f, 0f)
        Matrix.rotateM(mTorso, 0, pose.torsoTwistYaw, 0f, 1f, 0f)
        Matrix.rotateM(mTorso, 0, pose.torsoLeanForward, 1f, 0f, 0f)
        Matrix.rotateM(mTorso, 0, pose.torsoRoll, 0f, 0f, 1f)

        System.arraycopy(mTorso, 0, mTorsoDraw, 0, 16)
        Matrix.scaleM(mTorsoDraw, 0, pose.torsoBreathScale, 1f, pose.torsoBreathScale)
        drawMesh(rig.torso, mTorsoDraw, flashR, flashG, flashB, flashAmt, 1f)

        // Head (+Y, Muzzle +Z)
        System.arraycopy(mTorso, 0, mHead, 0, 16)
        Matrix.translateM(mHead, 0, 0f, rig.neckOffsetY, 0.02f)
        Matrix.rotateM(mHead, 0, pose.headYaw, 0f, 1f, 0f)
        Matrix.rotateM(mHead, 0, pose.headPitchForward, 1f, 0f, 0f)
        drawMesh(rig.head, mHead, flashR, flashG, flashB, flashAmt, 1f)

        // Dragon Wings (if Boss)
        if (rig.wingLeft != null && rig.wingRight != null) {
            System.arraycopy(mTorso, 0, mLimbA, 0, 16)
            Matrix.translateM(mLimbA, 0, 0.14f, rig.shoulderOffsetY * 0.86f, -0.18f)
            Matrix.rotateM(mLimbA, 0, -pose.wingSpreadDeg, 0f, 1f, 0f)
            Matrix.rotateM(mLimbA, 0, pose.wingFlapDeg, 0f, 0f, 1f)
            drawMesh(rig.wingLeft, mLimbA, flashR, flashG, flashB, flashAmt, 1f)

            System.arraycopy(mTorso, 0, mLimbA, 0, 16)
            Matrix.translateM(mLimbA, 0, -0.14f, rig.shoulderOffsetY * 0.86f, -0.18f)
            Matrix.rotateM(mLimbA, 0, pose.wingSpreadDeg, 0f, 1f, 0f)
            Matrix.rotateM(mLimbA, 0, -pose.wingFlapDeg, 0f, 0f, 1f)
            drawMesh(rig.wingRight, mLimbA, flashR, flashG, flashB, flashAmt, 1f)
        }

        // Left Arm (+X side)
        renderArm(
            rig = rig,
            side = 1f,
            shoulderSwingFwd = pose.lShoulderSwingFwd,
            shoulderOutward = pose.lShoulderOutward,
            elbowFlex = pose.lElbowFlex,
            upperArmMesh = rig.upperArmLeft,
            flashR = flashR, flashG = flashG, flashB = flashB, flashAmt = flashAmt
        )

        // Right Arm (-X side)
        renderArm(
            rig = rig,
            side = -1f,
            shoulderSwingFwd = pose.rShoulderSwingFwd,
            shoulderOutward = pose.rShoulderOutward,
            elbowFlex = pose.rElbowFlex,
            upperArmMesh = rig.upperArmRight,
            flashR = flashR, flashG = flashG, flashB = flashB, flashAmt = flashAmt
        )

        // Left Leg (+X side)
        renderLeg(
            rig = rig,
            side = 1f,
            pelvisLean = pose.pelvisLeanForward,
            thighSwingFwd = pose.lThighSwingFwd,
            thighOutward = pose.lThighOutward,
            kneeFlex = pose.lKneeFlex,
            flashR = flashR, flashG = flashG, flashB = flashB, flashAmt = flashAmt
        )

        // Right Leg (-X side)
        renderLeg(
            rig = rig,
            side = -1f,
            pelvisLean = pose.pelvisLeanForward,
            thighSwingFwd = pose.rThighSwingFwd,
            thighOutward = pose.rThighOutward,
            kneeFlex = pose.rKneeFlex,
            flashR = flashR, flashG = flashG, flashB = flashB, flashAmt = flashAmt
        )
    }

    private fun renderArm(
        rig: CharacterRigMeshSet,
        side: Float,
        shoulderSwingFwd: Float,
        shoulderOutward: Float,
        elbowFlex: Float,
        upperArmMesh: CompiledMesh,
        flashR: Float,
        flashG: Float,
        flashB: Float,
        flashAmt: Float
    ) {
        System.arraycopy(mTorso, 0, mLimbA, 0, 16)
        Matrix.translateM(mLimbA, 0, side * rig.shoulderHalfWidth, rig.shoulderOffsetY, 0f)
        Matrix.rotateM(mLimbA, 0, side * shoulderOutward, 0f, 0f, 1f)
        Matrix.rotateM(mLimbA, 0, -shoulderSwingFwd, 1f, 0f, 0f)
        drawMesh(upperArmMesh, mLimbA, flashR, flashG, flashB, flashAmt, 1f)

        System.arraycopy(mLimbA, 0, mLimbB, 0, 16)
        Matrix.translateM(mLimbB, 0, 0f, -rig.upperArmLength, 0f)
        Matrix.rotateM(mLimbB, 0, -side * 14f, 0f, 0f, 1f)
        Matrix.rotateM(mLimbB, 0, -elbowFlex, 1f, 0f, 0f)
        drawMesh(rig.forearmAndHand, mLimbB, flashR, flashG, flashB, flashAmt, 1f)
    }

    private fun renderLeg(
        rig: CharacterRigMeshSet,
        side: Float,
        pelvisLean: Float,
        thighSwingFwd: Float,
        thighOutward: Float,
        kneeFlex: Float,
        flashR: Float,
        flashG: Float,
        flashB: Float,
        flashAmt: Float
    ) {
        System.arraycopy(mPelvis, 0, mLimbA, 0, 16)
        Matrix.translateM(mLimbA, 0, side * rig.hipHalfWidth, 0f, 0f)
        Matrix.rotateM(mLimbA, 0, side * thighOutward, 0f, 0f, 1f)
        Matrix.rotateM(mLimbA, 0, -thighSwingFwd, 1f, 0f, 0f)
        drawMesh(rig.thigh, mLimbA, flashR, flashG, flashB, flashAmt, 1f)

        System.arraycopy(mLimbA, 0, mLimbB, 0, 16)
        Matrix.translateM(mLimbB, 0, 0f, -rig.thighLength, 0f)
        Matrix.rotateM(mLimbB, 0, kneeFlex, 1f, 0f, 0f)
        drawMesh(rig.calf, mLimbB, flashR, flashG, flashB, flashAmt, 1f)

        System.arraycopy(mLimbB, 0, mLimbC, 0, 16)
        Matrix.translateM(mLimbC, 0, 0f, -rig.calfLength, 0f)
        // Keep foot sole naturally aligned with ground unless knocked down or kicking high
        if (kotlin.math.abs(pelvisLean) < 35f && thighSwingFwd < 45f) {
            val compPitch = -(-thighSwingFwd + kneeFlex) * 0.85f
            Matrix.rotateM(mLimbC, 0, compPitch, 1f, 0f, 0f)
            Matrix.rotateM(mLimbC, 0, -side * thighOutward * 0.8f, 0f, 0f, 1f)
        }
        drawMesh(rig.foot, mLimbC, flashR, flashG, flashB, flashAmt, 1f)
    }

    private fun renderContactShadows(shadows: List<Triple<Float, Float, Float>>) {
        val mesh = contactShadowMesh ?: return
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glDepthMask(false)

        for ((x, z, scale) in shadows) {
            Matrix.setIdentityM(mScratch, 0)
            Matrix.translateM(mScratch, 0, x, 0.012f, z)
            Matrix.scaleM(mScratch, 0, scale, 1f, scale * 0.72f)
            drawMesh(mesh, mScratch, 0f, 0f, 0f, 0f, 0.65f)
        }

        GLES20.glDepthMask(true)
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    private fun renderDragonBoss3DTelegraphsAndEffects(snap: CombatSnapshot) {
        val boss = snap.opponent
        if (boss.id != FighterId.DRAGON_BOSS) return

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glDepthMask(false)

        // A. Early 3D Arena Floor Telegraph Danger Zone (shows exact attack reach & charge progress!)
        if (snap.bossTelegraphWarning.isNotEmpty() && snap.bossTelegraphReach > 0.1f) {
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)
            val reach = snap.bossTelegraphReach
            val prog = snap.bossTelegraphProgress.coerceIn(0.05f, 1f)
            val centerX = when (boss.action) {
                FighterActionState.BOSS_GROUND_STOMP,
                FighterActionState.BOSS_TAIL_SWEEP -> boss.x + boss.facing * (reach * 0.35f)
                else -> boss.x + boss.facing * (reach * 0.50f)
            }
            val scaleX = when (boss.action) {
                FighterActionState.BOSS_FIRE_BREATH -> reach * 0.85f
                else -> reach * 0.72f
            }
            val scaleZ = when (boss.action) {
                FighterActionState.BOSS_FIRE_BREATH -> 0.85f
                FighterActionState.BOSS_GROUND_STOMP -> 1.85f
                else -> 1.45f
            }
            val (r, g, b) = if (snap.bossTelegraphUnblockable) {
                Triple(1.0f, 0.16f, 0.12f) // Unblockable Crimson Danger Zone
            } else if (boss.action == FighterActionState.BOSS_FIRE_BREATH) {
                Triple(1.0f, 0.42f, 0.08f) // Hellfire Beam Lane
            } else {
                Triple(1.0f, 0.68f, 0.12f) // Blockable Amber Strike Zone
            }

            Matrix.setIdentityM(mScratch, 0)
            Matrix.translateM(mScratch, 0, centerX, 0.022f, 0f)
            Matrix.scaleM(mScratch, 0, scaleX, 1f, scaleZ)
            drawMesh(telegraphDiscMesh, mScratch, r, g, b, prog, 0.85f)
        }

        // B. Active 3D Volumetric Dragon Fire-Breath Jet Cone (stateTime 0.85s..1.41s)
        if (boss.action == FighterActionState.BOSS_FIRE_BREATH && boss.stateTime in 0.83f..1.42f) {
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)
            val mouthX = boss.x + boss.facing * 0.65f
            val jetExtend = ((boss.stateTime - 0.83f) / 0.14f).coerceIn(0.25f, 1f) * 3.25f
            val flicker = 0.88f + 0.18f * sin(renderTimeSec * 42f)
            Matrix.setIdentityM(mScratch, 0)
            Matrix.translateM(mScratch, 0, mouthX, 1.56f, 0f)
            // Orient cone (+Y axis in local mesh) horizontally along boss.facing (+X or -X) and slightly downward toward arena floor
            val angleZ = if (boss.facing >= 0f) -102f else 102f
            Matrix.rotateM(mScratch, 0, angleZ, 0f, 0f, 1f)
            Matrix.scaleM(mScratch, 0, flicker, jetExtend, flicker)
            drawMesh(fireBeamConeMesh, mScratch, 1.0f, 0.34f, 0.06f, 1f, 0.82f)
        }

        // C. Active 3D Ground-Shattering Stomp Expanding Seismic Shockwave Ring (stateTime 0.80s..1.12f)
        if (boss.action == FighterActionState.BOSS_GROUND_STOMP && boss.stateTime in 0.79f..1.14f) {
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)
            val waveProg = ((boss.stateTime - 0.79f) / 0.35f).coerceIn(0f, 1f)
            val radiusScale = 0.6f + waveProg * 2.8f
            val stompX = boss.x + boss.facing * 0.55f
            Matrix.setIdentityM(mScratch, 0)
            Matrix.translateM(mScratch, 0, stompX, 0.030f, 0f)
            Matrix.scaleM(mScratch, 0, radiusScale, 1f, radiusScale * 0.85f)
            drawMesh(telegraphDiscMesh, mScratch, 1.0f, 0.32f, 0.08f, 1.0f, (1f - waveProg) * 0.95f)
        }

        GLES20.glDepthMask(true)
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    private fun renderParticles() {
        val mesh = particleQuadMesh ?: return
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)
        GLES20.glDepthMask(false)

        for (p in particles) {
            if (!p.active) continue
            val lifeRatio = (1f - p.age / p.maxLife).coerceIn(0f, 1f)
            val s = p.size * (0.45f + 0.65f * lifeRatio)
            Matrix.setIdentityM(mScratch, 0)
            Matrix.translateM(mScratch, 0, p.x, p.y, p.z)
            Matrix.scaleM(mScratch, 0, s, s, s)
            drawMesh(mesh, mScratch, p.r, p.g, p.b, 1f, lifeRatio * 0.90f)
        }

        GLES20.glDepthMask(true)
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    private fun drawMesh(
        mesh: CompiledMesh?,
        modelMatrix: FloatArray,
        flashR: Float,
        flashG: Float,
        flashB: Float,
        flashAmt: Float,
        alpha: Float
    ) {
        if (mesh == null) return
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelMatrixLoc, 1, false, modelMatrix, 0)
        GLES20.glUniform3f(uTintFlashLoc, flashR, flashG, flashB)
        GLES20.glUniform1f(uFlashAmountLoc, flashAmt)
        GLES20.glUniform1f(uAlphaLoc, alpha)

        val vb = mesh.vertexBuffer
        val stride = CompiledMesh.STRIDE_BYTES

        vb.position(0)
        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, stride, vb)

        vb.position(3)
        GLES20.glEnableVertexAttribArray(aNormalLoc)
        GLES20.glVertexAttribPointer(aNormalLoc, 3, GLES20.GL_FLOAT, false, stride, vb)

        vb.position(6)
        GLES20.glEnableVertexAttribArray(aUVLoc)
        GLES20.glVertexAttribPointer(aUVLoc, 2, GLES20.GL_FLOAT, false, stride, vb)

        vb.position(8)
        GLES20.glEnableVertexAttribArray(aMaterialIdLoc)
        GLES20.glVertexAttribPointer(aMaterialIdLoc, 1, GLES20.GL_FLOAT, false, stride, vb)

        vb.position(9)
        GLES20.glEnableVertexAttribArray(aAOLoc)
        GLES20.glVertexAttribPointer(aAOLoc, 1, GLES20.GL_FLOAT, false, stride, vb)

        mesh.indexBuffer.position(0)
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            mesh.indexCount,
            GLES20.GL_UNSIGNED_SHORT,
            mesh.indexBuffer
        )
    }

    private fun drainAndSimulateParticles(dt: Float) {
        val maxActive = graphicsQuality.maxParticles.coerceAtMost(particles.size)

        // Subtle atmospheric underground stadium embers
        emberSpawnTimer += dt
        if (emberSpawnTimer >= 0.14f) {
            emberSpawnTimer = 0f
            spawnSingleParticle(
                kind = ParticleKind.ARENA_EMBER,
                wx = (rng.nextFloat() * 2f - 1f) * 6.5f,
                wy = 0.08f,
                wz = (rng.nextFloat() * 2f - 1f) * 4.0f - 1.2f,
                dirX = 0f,
                maxActive = maxActive
            )
        }

        // Dragon Fire-Breath Telegraph Inhale Embers & Active Flame Torrent!
        val snap = combatSnapshot
        if (sceneMode == ViewportSceneMode.COMBAT_ARENA &&
            snap.opponent.id == FighterId.DRAGON_BOSS &&
            snap.opponent.action == FighterActionState.BOSS_FIRE_BREATH
        ) {
            val mouthX = snap.opponent.x + snap.opponent.facing * 0.70f
            if (snap.opponent.stateTime in 0.05f..0.82f) {
                // Early Telegraph: Embers get sucked INWARD toward the Dragon's charging jaws!
                spawnSingleParticle(
                    kind = ParticleKind.DRAGON_INHALE_EMBER,
                    wx = mouthX + snap.opponent.facing * (0.95f + rng.nextFloat() * 0.65f),
                    wy = 1.45f + (rng.nextFloat() - 0.5f) * 0.75f,
                    wz = (rng.nextFloat() - 0.5f) * 0.75f,
                    dirX = -snap.opponent.facing,
                    maxActive = maxActive
                )
            } else if (snap.opponent.stateTime in 0.83f..1.41f) {
                // Active Devastating Hellfire Torrent!
                repeat(4) {
                    spawnSingleParticle(
                        kind = ParticleKind.DRAGON_FIRE,
                        wx = mouthX,
                        wy = 1.62f + (rng.nextFloat() - 0.5f) * 0.18f,
                        wz = (rng.nextFloat() - 0.5f) * 0.20f,
                        dirX = snap.opponent.facing,
                        maxActive = maxActive
                    )
                }
            }
        }

        var ev = pendingParticleEvents.poll()
        while (ev != null) {
            val scaledCount = when (graphicsQuality) {
                GraphicsQuality.LOW -> (ev.count / 2).coerceAtLeast(4)
                GraphicsQuality.MEDIUM -> (ev.count * 3 / 4).coerceAtLeast(6)
                GraphicsQuality.HIGH -> ev.count
            }
            repeat(scaledCount) {
                spawnSingleParticle(ev.kind, ev.worldX, ev.worldY, ev.worldZ, ev.dirX, maxActive)
            }
            ev = pendingParticleEvents.poll()
        }

        for (i in 0 until particles.size) {
            val p = particles[i]
            if (!p.active) continue
            p.age += dt
            if (p.age >= p.maxLife) {
                p.active = false
                continue
            }
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.z += p.vz * dt
            p.vy -= 3.2f * dt
            if (p.y < 0.03f) {
                p.y = 0.03f
                p.vy = kotlin.math.abs(p.vy) * 0.35f
            }
        }
    }

    private fun spawnSingleParticle(
        kind: ParticleKind,
        wx: Float,
        wy: Float,
        wz: Float,
        dirX: Float,
        maxActive: Int
    ) {
        val slot = particles.take(maxActive).firstOrNull { !it.active } ?: return
        slot.active = true
        slot.age = 0f
        slot.x = wx
        slot.y = wy
        slot.z = wz

        val rx = rng.nextFloat() * 2f - 1f
        val ry = rng.nextFloat() * 2f - 1f
        val rz = rng.nextFloat() * 2f - 1f

        when (kind) {
            ParticleKind.HIT_SPARK -> {
                slot.vx = dirX * 2.8f + rx * 2.4f
                slot.vy = 1.2f + rng.nextFloat() * 2.6f
                slot.vz = rz * 2.2f
                slot.r = 1.0f; slot.g = 0.76f; slot.b = 0.22f
                slot.size = 0.075f
                slot.maxLife = 0.28f + rng.nextFloat() * 0.16f
            }
            ParticleKind.BLOCK_SPARK -> {
                slot.vx = rx * 2.2f
                slot.vy = 0.8f + rng.nextFloat() * 2.0f
                slot.vz = rz * 2.0f
                slot.r = 0.32f; slot.g = 0.85f; slot.b = 1.0f
                slot.size = 0.068f
                slot.maxLife = 0.24f + rng.nextFloat() * 0.12f
            }
            ParticleKind.DUST_PUFF -> {
                slot.vx = dirX * 1.1f + rx * 1.3f
                slot.vy = 0.45f + rng.nextFloat() * 0.85f
                slot.vz = rz * 1.1f
                slot.r = 0.58f; slot.g = 0.56f; slot.b = 0.54f
                slot.size = 0.12f
                slot.maxLife = 0.42f + rng.nextFloat() * 0.18f
            }
            ParticleKind.TIGER_FLAME -> {
                slot.vx = dirX * 3.2f + rx * 1.8f
                slot.vy = 1.5f + rng.nextFloat() * 2.2f
                slot.vz = rz * 1.8f
                slot.r = 1.0f; slot.g = 0.45f; slot.b = 0.08f
                slot.size = 0.11f
                slot.maxLife = 0.38f + rng.nextFloat() * 0.16f
            }
            ParticleKind.BUFFALO_SHOCKWAVE -> {
                slot.vx = rx * 3.8f
                slot.vy = 0.9f + rng.nextFloat() * 1.6f
                slot.vz = rz * 3.2f
                slot.r = 0.35f; slot.g = 0.82f; slot.b = 1.0f
                slot.size = 0.13f
                slot.maxLife = 0.44f
            }
            ParticleKind.DRAGON_FIRE -> {
                slot.vx = dirX * (6.2f + rng.nextFloat() * 2.5f)
                slot.vy = -0.75f + ry * 0.85f
                slot.vz = rz * 0.75f
                slot.r = 1.0f
                slot.g = 0.34f + rng.nextFloat() * 0.38f
                slot.b = 0.06f
                slot.size = 0.20f
                slot.maxLife = 0.44f
            }
            ParticleKind.DRAGON_INHALE_EMBER -> {
                slot.vx = dirX * (2.8f + rng.nextFloat() * 1.4f)
                slot.vy = 0.5f + ry * 0.9f
                slot.vz = -slot.z * 2.5f
                slot.r = 1.0f; slot.g = 0.65f; slot.b = 0.14f
                slot.size = 0.085f
                slot.maxLife = 0.28f
            }
            ParticleKind.STOMP_SEISMIC -> {
                slot.vx = rx * 4.8f
                slot.vy = 1.8f + rng.nextFloat() * 2.4f
                slot.vz = rz * 3.8f
                slot.r = 1.0f; slot.g = 0.28f; slot.b = 0.08f
                slot.size = 0.16f
                slot.maxLife = 0.50f
            }
            ParticleKind.ARENA_EMBER -> {
                slot.vx = rx * 0.25f
                slot.vy = 1.6f + rng.nextFloat() * 0.8f
                slot.vz = rz * 0.25f
                slot.r = 1.0f; slot.g = 0.52f; slot.b = 0.15f
                slot.size = 0.038f
                slot.maxLife = 1.6f
            }
        }
    }

    private fun rebuildMeshesIfNeeded() {
        val targetSegs = graphicsQuality.radialSegments
        if (builtRadialSegments == targetSegs && arenaFloorMesh != null) return
        builtRadialSegments = targetSegs

        rigCache[FighterId.SHADOW_TIGER] = CharacterMeshFactory.createRig(FighterId.SHADOW_TIGER, targetSegs)
        rigCache[FighterId.IRON_BUFFALO] = CharacterMeshFactory.createRig(FighterId.IRON_BUFFALO, targetSegs)
        rigCache[FighterId.DRAGON_BOSS] = CharacterMeshFactory.createRig(FighterId.DRAGON_BOSS, targetSegs)

        arenaFloorMesh = buildArenaFloorMesh()
        arenaBackdropMesh = buildArenaBackdropPillars()
        contactShadowMesh = buildContactShadowMesh()
        particleQuadMesh = buildParticleDiamondMesh()
        telegraphDiscMesh = buildTelegraphDiscMesh()
        fireBeamConeMesh = buildFireBeamConeMesh()
    }

    private fun buildArenaFloorMesh(): CompiledMesh = OrganicMeshBuilder(24).apply {
        // Futuristic Octagonal Underground Combat Platform + Beveled Rim
        addLoftedTube(
            listOf(
                LoftStation(cy = -0.45f, rx = 7.4f, rzFront = 6.2f, rzBack = 6.2f, vCoord = 0f, materialId = MaterialIds.ARENA_PILLAR),
                LoftStation(cy = -0.02f, rx = 7.0f, rzFront = 5.8f, rzBack = 5.8f, vCoord = 0.4f, materialId = MaterialIds.ARENA_PILLAR),
                LoftStation(cy = 0.00f, rx = 6.8f, rzFront = 5.6f, rzBack = 5.6f, vCoord = 1.0f, materialId = MaterialIds.ARENA_FLOOR)
            ),
            segments = 16,
            capStart = false,
            capEnd = true
        )
    }.build()

    private fun buildArenaBackdropPillars(): CompiledMesh = OrganicMeshBuilder(10).apply {
        // Futuristic underground stadium pillars & overhead neon light trusses behind combat line (-Z)
        val pillarPositions = listOf(
            floatArrayOf(-5.6f, -3.8f),
            floatArrayOf(-2.9f, -4.8f),
            floatArrayOf(0.0f, -5.2f),
            floatArrayOf(2.9f, -4.8f),
            floatArrayOf(5.6f, -3.8f)
        )
        for (p in pillarPositions) {
            val px = p[0]
            val pz = p[1]
            addLoftedTube(
                listOf(
                    LoftStation(cx = px, cy = 0f, cz = pz, rx = 0.36f, rzFront = 0.36f, vCoord = 0f, materialId = MaterialIds.ARENA_PILLAR),
                    LoftStation(cx = px, cy = 2.6f, cz = pz - 0.15f, rx = 0.30f, rzFront = 0.30f, vCoord = 0.5f, materialId = MaterialIds.ARENA_PILLAR),
                    LoftStation(cx = px * 0.88f, cy = 4.8f, cz = pz + 0.55f, rx = 0.22f, rzFront = 0.22f, vCoord = 1f, materialId = MaterialIds.ARENA_PILLAR)
                ),
                segments = 8
            )
        }
        // Overhead Stadium Arch Beam
        addSweptCurve(
            listOf(
                floatArrayOf(-5.2f, 4.4f, -3.4f, 0.16f, 0.16f, MaterialIds.ARENA_PILLAR),
                floatArrayOf(-2.6f, 4.9f, -4.2f, 0.16f, 0.16f, MaterialIds.ARENA_PILLAR),
                floatArrayOf(0.0f, 5.1f, -4.5f, 0.18f, 0.18f, MaterialIds.ARENA_PILLAR),
                floatArrayOf(2.6f, 4.9f, -4.2f, 0.16f, 0.16f, MaterialIds.ARENA_PILLAR),
                floatArrayOf(5.2f, 4.4f, -3.4f, 0.16f, 0.16f, MaterialIds.ARENA_PILLAR)
            ),
            segments = 8
        )
    }.build()

    private fun buildContactShadowMesh(): CompiledMesh = OrganicMeshBuilder(16).apply {
        val segs = 16
        val center = addVertex(0f, 0f, 0f, 0.5f, 0.5f, MaterialIds.CONTACT_SHADOW, 1f)
        val ring = IntArray(segs)
        for (i in 0 until segs) {
            val a = (2.0 * PI * i) / segs
            val x = sin(a).toFloat() * 0.65f
            val z = cos(a).toFloat() * 0.65f
            val u = 0.5f + sin(a).toFloat() * 0.5f
            val v = 0.5f + cos(a).toFloat() * 0.5f
            ring[i] = addVertex(x, 0f, z, u, v, MaterialIds.CONTACT_SHADOW, 1f)
        }
        for (i in 0 until segs) {
            addTriangle(center, ring[i], ring[(i + 1) % segs])
        }
    }.build()

    private fun buildParticleDiamondMesh(): CompiledMesh = OrganicMeshBuilder(6).apply {
        addLoftedTube(
            listOf(
                LoftStation(cy = -0.8f, rx = 0.05f, rzFront = 0.05f, vCoord = 0f, materialId = MaterialIds.PARTICLE_FX),
                LoftStation(cy = 0.0f, rx = 0.85f, rzFront = 0.85f, vCoord = 0.5f, materialId = MaterialIds.PARTICLE_FX),
                LoftStation(cy = 0.8f, rx = 0.05f, rzFront = 0.05f, vCoord = 1f, materialId = MaterialIds.PARTICLE_FX)
            ),
            segments = 6
        )
    }.build()

    private fun buildTelegraphDiscMesh(): CompiledMesh = OrganicMeshBuilder(24).apply {
        val segs = 24
        val center = addVertex(0f, 0f, 0f, 0.5f, 0.5f, MaterialIds.TELEGRAPH_ZONE, 1f)
        val ring = IntArray(segs)
        for (i in 0 until segs) {
            val a = (2.0 * PI * i) / segs
            val x = sin(a).toFloat() * 1.0f
            val z = cos(a).toFloat() * 1.0f
            val u = 0.5f + sin(a).toFloat() * 0.5f
            val v = 0.5f + cos(a).toFloat() * 0.5f
            ring[i] = addVertex(x, 0f, z, u, v, MaterialIds.TELEGRAPH_ZONE, 1f)
        }
        for (i in 0 until segs) {
            addTriangle(center, ring[i], ring[(i + 1) % segs])
        }
    }.build()

    private fun buildFireBeamConeMesh(): CompiledMesh = OrganicMeshBuilder(14).apply {
        addLoftedTube(
            listOf(
                LoftStation(cy = 0.0f, rx = 0.14f, rzFront = 0.14f, vCoord = 0.0f, materialId = MaterialIds.FIRE_BEAM),
                LoftStation(cy = 0.35f, rx = 0.28f, rzFront = 0.28f, vCoord = 0.35f, materialId = MaterialIds.FIRE_BEAM),
                LoftStation(cy = 0.75f, rx = 0.44f, rzFront = 0.44f, vCoord = 0.75f, materialId = MaterialIds.FIRE_BEAM),
                LoftStation(cy = 1.00f, rx = 0.54f, rzFront = 0.54f, vCoord = 1.0f, materialId = MaterialIds.FIRE_BEAM)
            ),
            segments = 14,
            capStart = true,
            capEnd = true
        )
    }.build()
}
