package com.example

import com.example.engine3d.CharacterMeshFactory
import com.example.engine3d.SkeletalAnimator
import com.example.game.CombatButtonInput
import com.example.game.CombatEngine
import com.example.model.CampaignStage
import com.example.model.FighterActionState
import com.example.model.FighterId
import com.example.model.MatchPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun playableRoster_containsStrictlyShadowTigerAndIronBuffalo() {
        val playable = FighterId.playableRoster
        assertEquals(2, playable.size)
        assertEquals(FighterId.SHADOW_TIGER, playable[0])
        assertEquals(FighterId.IRON_BUFFALO, playable[1])
        assertFalse(FighterId.DRAGON_BOSS.isPlayable)
    }

    @Test
    fun character3DMeshGeneration_buildsValidSculptedRigsForAllWarriors() {
        for (id in FighterId.entries) {
            val rig = CharacterMeshFactory.createRig(id, radialSegments = 16)
            assertTrue(rig.pelvis.indexCount > 60)
            assertTrue(rig.torso.indexCount > 120)
            assertTrue(rig.head.indexCount > 240)
            assertTrue(rig.upperArmLeft.indexCount > 60)
            assertTrue(rig.forearmAndHand.indexCount > 100)
            assertTrue(rig.thigh.indexCount > 60)
            assertTrue(rig.calf.indexCount > 60)
            assertTrue(rig.foot.indexCount > 60)
            assertNotNull(rig.tail)
            if (id == FighterId.DRAGON_BOSS) {
                assertNotNull(rig.wingLeft)
                assertNotNull(rig.wingRight)
            }
        }
    }

    @Test
    fun combatEngine_enforcesFacingPushboxesCombosBlockAndDodge() {
        var wonStage: CampaignStage? = null
        val engine = CombatEngine(onPlayerWonMatch = { wonStage = it })

        // Fight 1: Shadow Tiger vs Iron Buffalo
        engine.startMatch(CampaignStage.FIGHT_1)
        assertEquals(FighterId.SHADOW_TIGER, engine.snapshot.player.id)
        assertEquals(FighterId.IRON_BUFFALO, engine.snapshot.opponent.id)
        assertEquals(1f, engine.snapshot.player.facing, 0.001f)
        assertEquals(-1f, engine.snapshot.opponent.facing, 0.001f)

        // Advance past Round 1 Intro
        repeat(40) { engine.step(0.04f) }
        assertEquals(MatchPhase.FIGHTING, engine.snapshot.matchPhase)

        // Advance into melee range while guarding if needed, then verify pushbox separation
        engine.playerMoveAxis = 1.0f
        var safetySteps = 0
        while (kotlin.math.abs(engine.snapshot.opponent.x - engine.snapshot.player.x) > 1.35f && safetySteps < 90) {
            engine.step(0.025f)
            safetySteps++
        }
        engine.playerMoveAxis = 0f
        // Wait until player recovers to IDLE if grazed while approaching
        repeat(20) {
            if (engine.snapshot.player.action.isIncapacitated) engine.step(0.03f)
        }
        assertTrue(engine.snapshot.player.x < engine.snapshot.opponent.x)
        assertTrue(engine.snapshot.player.x >= -CombatEngine.ARENA_HALF_WIDTH)
        assertTrue(engine.snapshot.opponent.x <= CombatEngine.ARENA_HALF_WIDTH)

        // Trigger Punch -> Combo Punch 2 -> Combo Punch 3 and verify opponent takes damage
        val initialOppHp = engine.snapshot.opponent.health
        engine.queuePlayerButton(CombatButtonInput.PUNCH)
        repeat(4) { engine.step(0.03f) }
        assertEquals(FighterActionState.ATTACK_PUNCH_1, engine.snapshot.player.action)

        engine.queuePlayerButton(CombatButtonInput.PUNCH)
        repeat(6) { engine.step(0.03f) }
        engine.queuePlayerButton(CombatButtonInput.PUNCH)
        repeat(16) { engine.step(0.03f) }
        assertTrue(engine.snapshot.opponent.health < initialOppHp)

        // Ensure recovery completes, then verify Dodge Back invulnerability state
        repeat(20) { engine.step(0.03f) }
        engine.queuePlayerButton(CombatButtonInput.DODGE_BACK)
        engine.step(0.02f)
        assertTrue(engine.snapshot.player.action.isInvulnerable)

        // Verify Fight 2 swaps player to Iron Buffalo vs Shadow Tiger
        engine.startMatch(CampaignStage.FIGHT_2)
        assertEquals(FighterId.IRON_BUFFALO, engine.snapshot.player.id)
        assertEquals(FighterId.SHADOW_TIGER, engine.snapshot.opponent.id)

        // Verify Fight 3 introduces Dragon Boss with cinematic entrance
        engine.startMatch(CampaignStage.FIGHT_3_BOSS, FighterId.SHADOW_TIGER)
        assertEquals(FighterId.DRAGON_BOSS, engine.snapshot.opponent.id)
        assertEquals(MatchPhase.BOSS_ENTRANCE, engine.snapshot.matchPhase)
        repeat(60) { engine.step(0.04f) }
        // Transitions from Boss Entrance -> Round Intro -> Fighting
        repeat(45) { engine.step(0.04f) }
        assertEquals(MatchPhase.FIGHTING, engine.snapshot.matchPhase)

        // Verify skeletal poses evaluate cleanly without NaNs
        val pose = SkeletalAnimator.evaluateBlendedPose(engine.snapshot.opponent, 1.5f)
        assertFalse(pose.torsoLeanForward.isNaN())
        assertNullOrValid(wonStage)
    }

    @Test
    fun dragonBoss_executesDistinctTelegraphedAttacksWithBlockAndDodgeCounterplay() {
        val engine = CombatEngine()

        // 1. Verify all 3+ Dragon Boss patterns have generous early-reactable telegraph startups (>= 0.60s)
        val clawSpec = engine.getAttackSpec(FighterId.DRAGON_BOSS, FighterActionState.BOSS_CLAW_STRIKE)
        val stompSpec = engine.getAttackSpec(FighterId.DRAGON_BOSS, FighterActionState.BOSS_GROUND_STOMP)
        val breathSpec = engine.getAttackSpec(FighterId.DRAGON_BOSS, FighterActionState.BOSS_FIRE_BREATH)
        val tailSpec = engine.getAttackSpec(FighterId.DRAGON_BOSS, FighterActionState.BOSS_TAIL_SWEEP)

        assertTrue(clawSpec.startupSec >= 0.60f)
        assertTrue(stompSpec.startupSec >= 0.75f && stompSpec.guardBreak)
        assertTrue(breathSpec.startupSec >= 0.80f && breathSpec.reach > 3.5f)
        assertTrue(tailSpec.startupSec >= 0.60f)

        // 2. Start Fight 3 Boss and step into active combat
        engine.startMatch(CampaignStage.FIGHT_3_BOSS, FighterId.SHADOW_TIGER)
        var guardSteps = 0
        while (engine.snapshot.matchPhase != MatchPhase.FIGHTING && guardSteps < 150) {
            engine.step(0.04f)
            guardSteps++
        }
        assertEquals(MatchPhase.FIGHTING, engine.snapshot.matchPhase)

        // Move into close-mid range (<= 2.2f) so Dragon Boss cycles through Claw Swipe -> Ground Stomp -> Fire-Breath
        engine.playerMoveAxis = 1.0f
        var approachSteps = 0
        while (kotlin.math.abs(engine.snapshot.opponent.x - engine.snapshot.player.x) > 2.15f && approachSteps < 60) {
            engine.step(0.025f)
            approachSteps++
        }
        engine.playerMoveAxis = 0f

        val observedBossPatterns = mutableSetOf<FighterActionState>()
        val observedTelegraphWarnings = mutableSetOf<String>()

        // Simulate through sequential boss attacks and react to their telegraphs!
        repeat(340) {
            val snap = engine.snapshot
            val boss = snap.opponent
            if (boss.action.isAttacking) {
                observedBossPatterns.add(boss.action)
            }
            if (snap.bossTelegraphWarning.isNotEmpty()) {
                observedTelegraphWarnings.add(snap.bossTelegraphWarning)
                assertTrue(snap.bossTelegraphCounterHint.isNotEmpty())
                assertTrue(snap.bossTelegraphProgress in 0f..1f)

                // React to early visual telegraph!
                if (snap.bossTelegraphUnblockable) {
                    // Ground-Shattering Stomp: react with Dodge Back when telegraph reaches 72%
                    engine.playerHoldingBlock = false
                    engine.playerMoveAxis = 0f
                    if (snap.bossTelegraphProgress >= 0.72f && snap.player.dodgeCooldown <= 0f && !snap.player.action.isInvulnerable) {
                        engine.queuePlayerButton(CombatButtonInput.DODGE_BACK)
                    }
                } else {
                    // Claw Swipe / Fire-Breath: hold Block to guard against attack
                    engine.playerMoveAxis = 0f
                    engine.playerHoldingBlock = true
                }
            } else {
                engine.playerHoldingBlock = false
                // Re-close distance inside 1.95m between boss attacks so all melee/mid patterns cycle
                val gap = kotlin.math.abs(snap.opponent.x - snap.player.x)
                engine.playerMoveAxis = if (!boss.action.isAttacking && gap > 1.95f) 1.0f else 0f
            }

            engine.step(0.025f)

            // Verify skeletal poses for all boss telegraph & active frames have no NaNs
            val bossPose = SkeletalAnimator.evaluateBlendedPose(engine.snapshot.opponent, engine.snapshot.elapsedArenaTime)
            assertFalse(bossPose.headPitchForward.isNaN())
            assertFalse(bossPose.wingSpreadDeg.isNaN())
        }

        // Confirm all 3 requested Dragon Boss patterns (Claw Swipe, Ground-Shattering Stomp, Fire-Breath) executed & telegraphed!
        assertTrue("Expected BOSS_CLAW_STRIKE in $observedBossPatterns", observedBossPatterns.contains(FighterActionState.BOSS_CLAW_STRIKE))
        assertTrue("Expected BOSS_GROUND_STOMP in $observedBossPatterns", observedBossPatterns.contains(FighterActionState.BOSS_GROUND_STOMP))
        assertTrue("Expected BOSS_FIRE_BREATH in $observedBossPatterns", observedBossPatterns.contains(FighterActionState.BOSS_FIRE_BREATH))

        assertTrue(observedTelegraphWarnings.contains("DRACONIC CLAW SWIPE"))
        assertTrue(observedTelegraphWarnings.contains("GROUND-SHATTERING STOMP"))
        assertTrue(observedTelegraphWarnings.contains("CATACLYSM FIRE-BREATH"))

        // Confirm reacting to telegraphs kept the player alive with healthy HP (> 65% remaining after 3+ boss attacks!)
        assertTrue(
            "Player should mitigate/evade telegraphed boss attacks via block & dodge (HP=${engine.snapshot.player.healthFraction})",
            engine.snapshot.player.healthFraction > 0.65f
        )
    }

    private fun assertNullOrValid(stage: CampaignStage?) {
        if (stage != null) {
            assertTrue(stage.stageNumber in 1..3)
        }
    }
}

