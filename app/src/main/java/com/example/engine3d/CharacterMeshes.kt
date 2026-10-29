package com.example.engine3d

import com.example.model.FighterId

/**
 * Holds compiled anatomical rig sub-meshes for a fully articulated character.
 * Every limb segment is modeled around its proximal joint origin (0, 0, 0):
 * - Torso/Neck/Head extend +Y, with facial muzzles/eyes/chests facing +Z.
 * - Arms/Forearms/Thighs/Calves extend -Y from their joint socket, with rounded overlap domes.
 * - Hands/Paws/Claws attach at wrist (0, 0, 0) extending -Y/+Z.
 * - Feet/Paws/Hooves attach at ankle (0, 0, 0) extending forward along +Z with sole at -ankleHeight.
 */
class CharacterRigMeshSet(
    val pelvis: CompiledMesh,
    val torso: CompiledMesh,
    val head: CompiledMesh,
    val upperArmLeft: CompiledMesh,
    val upperArmRight: CompiledMesh,
    val forearmAndHand: CompiledMesh,
    val thigh: CompiledMesh,
    val calf: CompiledMesh,
    val foot: CompiledMesh,
    val tail: CompiledMesh?,
    val wingLeft: CompiledMesh?,
    val wingRight: CompiledMesh?,
    // Rig joint offsets in meters (anatomical proportions)
    val hipHeight: Float,
    val hipHalfWidth: Float,
    val spineLength: Float,
    val neckOffsetY: Float,
    val shoulderOffsetY: Float,
    val shoulderHalfWidth: Float,
    val upperArmLength: Float,
    val forearmLength: Float,
    val thighLength: Float,
    val calfLength: Float,
    val ankleHeight: Float
)

object CharacterMeshFactory {

    fun createRig(fighterId: FighterId, radialSegments: Int): CharacterRigMeshSet {
        return when (fighterId) {
            FighterId.SHADOW_TIGER -> buildShadowTigerRig(radialSegments)
            FighterId.IRON_BUFFALO -> buildIronBuffaloRig(radialSegments)
            FighterId.DRAGON_BOSS -> buildDragonBossRig(radialSegments)
        }
    }

    private fun buildShadowTigerRig(seg: Int): CharacterRigMeshSet {
        // 1. Pelvis & Martial Belt Sash
        val pelvis = OrganicMeshBuilder(seg).apply {
            addLoftedTube(
                listOf(
                    LoftStation(cy = -0.10f, rx = 0.17f, rzFront = 0.13f, rzBack = 0.14f, vCoord = 0f, materialId = MaterialIds.GI_CRIMSON, ao = 0.8f),
                    LoftStation(cy = 0.00f, rx = 0.20f, rzFront = 0.15f, rzBack = 0.16f, vCoord = 0.5f, materialId = MaterialIds.GI_CRIMSON),
                    LoftStation(cy = 0.09f, rx = 0.21f, rzFront = 0.16f, rzBack = 0.15f, vCoord = 1f, materialId = MaterialIds.BELT_GOLD)
                )
            )
            // Front hanging martial arts obi belt sash (+Z front)
            addSweptCurve(
                listOf(
                    floatArrayOf(-0.04f, 0.06f, 0.16f, 0.035f, 0.015f, MaterialIds.BELT_GOLD),
                    floatArrayOf(-0.06f, -0.08f, 0.19f, 0.038f, 0.014f, MaterialIds.BELT_GOLD),
                    floatArrayOf(-0.07f, -0.22f, 0.20f, 0.032f, 0.012f, MaterialIds.BELT_GOLD)
                ),
                segments = 8
            )
        }.build()

        // 2. Muscular V-Tapered Tiger Torso in Sleeveless Crimson Martial Gi + Fur Chest Cleavage
        val torso = OrganicMeshBuilder(seg).apply {
            addLoftedTube(
                listOf(
                    LoftStation(cy = -0.03f, rx = 0.19f, rzFront = 0.14f, rzBack = 0.14f, vCoord = 0f, materialId = MaterialIds.BELT_GOLD, ao = 0.85f),
                    LoftStation(cy = 0.12f, rx = 0.22f, rzFront = 0.16f, rzBack = 0.15f, muscleHarmonic2 = 0.05f, vCoord = 0.25f, materialId = MaterialIds.GI_CRIMSON),
                    LoftStation(cy = 0.28f, rx = 0.27f, rzFront = 0.20f, rzBack = 0.17f, muscleHarmonic2 = 0.08f, vCoord = 0.6f, materialId = MaterialIds.GI_CRIMSON),
                    LoftStation(cy = 0.42f, rx = 0.30f, rzFront = 0.21f, rzBack = 0.18f, muscleHarmonic2 = 0.06f, vCoord = 0.85f, materialId = MaterialIds.GI_CRIMSON),
                    LoftStation(cy = 0.50f, cz = 0.01f, rx = 0.15f, rzFront = 0.13f, rzBack = 0.13f, vCoord = 1f, materialId = MaterialIds.TIGER_FUR)
                )
            )
            // Exposed muscular tiger fur chest V-neck at +Z
            addLoftedTube(
                listOf(
                    LoftStation(cy = 0.24f, cz = 0.14f, rx = 0.05f, rzFront = 0.07f, rzBack = 0.02f, vCoord = 0.3f, materialId = MaterialIds.TIGER_CREAM),
                    LoftStation(cy = 0.38f, cz = 0.14f, rx = 0.11f, rzFront = 0.085f, rzBack = 0.03f, vCoord = 0.7f, materialId = MaterialIds.TIGER_FUR),
                    LoftStation(cy = 0.48f, cz = 0.08f, rx = 0.12f, rzFront = 0.08f, rzBack = 0.04f, vCoord = 1f, materialId = MaterialIds.TIGER_FUR)
                ),
                segments = 12
            )
            // Golden Gi Collar Lapels
            for (side in floatArrayOf(-1f, 1f)) {
                addSweptCurve(
                    listOf(
                        floatArrayOf(0f, 0.18f, 0.19f, 0.022f, 0.016f, MaterialIds.BELT_GOLD),
                        floatArrayOf(side * 0.09f, 0.34f, 0.20f, 0.024f, 0.016f, MaterialIds.BELT_GOLD),
                        floatArrayOf(side * 0.15f, 0.48f, 0.08f, 0.024f, 0.016f, MaterialIds.BELT_GOLD)
                    ),
                    segments = 8
                )
            }
        }.build()

        // 3. Detailed Tiger Head (Realistic feline skull, cheek ruffs, forward +Z muzzle, nose, jaw, fangs, amber eyes, ears)
        val head = OrganicMeshBuilder(seg).apply {
            // Neck & Cranium
            addLoftedTube(
                listOf(
                    LoftStation(cy = -0.04f, cz = -0.01f, rx = 0.10f, rzFront = 0.10f, rzBack = 0.10f, vCoord = 0f, materialId = MaterialIds.TIGER_FUR, ao = 0.82f),
                    LoftStation(cy = 0.06f, cz = 0.01f, rx = 0.15f, rzFront = 0.14f, rzBack = 0.13f, vCoord = 0.3f, materialId = MaterialIds.TIGER_FUR),
                    LoftStation(cy = 0.13f, cz = 0.02f, rx = 0.175f, rzFront = 0.15f, rzBack = 0.14f, muscleHarmonic2 = 0.07f, vCoord = 0.55f, materialId = MaterialIds.TIGER_FUR),
                    LoftStation(cy = 0.22f, cz = 0.01f, rx = 0.145f, rzFront = 0.14f, rzBack = 0.13f, vCoord = 0.8f, materialId = MaterialIds.TIGER_FUR),
                    LoftStation(cy = 0.28f, cz = -0.01f, rx = 0.09f, rzFront = 0.09f, rzBack = 0.09f, vCoord = 1f, materialId = MaterialIds.TIGER_FUR)
                )
            )
            // Iconic Feline Cheek Ruffs (±X)
            for (side in floatArrayOf(-1f, 1f)) {
                addSweptCurve(
                    listOf(
                        floatArrayOf(side * 0.11f, 0.14f, 0.04f, 0.055f, 0.055f, MaterialIds.TIGER_CREAM),
                        floatArrayOf(side * 0.19f, 0.10f, 0.03f, 0.048f, 0.052f, MaterialIds.TIGER_CREAM),
                        floatArrayOf(side * 0.22f, 0.06f, 0.01f, 0.022f, 0.028f, MaterialIds.TIGER_CREAM)
                    ),
                    segments = 10
                )
            }
            // Defined Tiger Upper Muzzle & Snout Bridge (+Z forward!)
            addSweptCurve(
                listOf(
                    floatArrayOf(0f, 0.16f, 0.08f, 0.095f, 0.075f, MaterialIds.TIGER_FUR),
                    floatArrayOf(0f, 0.13f, 0.17f, 0.082f, 0.062f, MaterialIds.TIGER_FUR),
                    floatArrayOf(0f, 0.11f, 0.24f, 0.068f, 0.050f, MaterialIds.TIGER_CREAM)
                ),
                segments = 12
            )
            // Nose Pad at tip of Muzzle (+Z)
            addSweptCurve(
                listOf(
                    floatArrayOf(0f, 0.135f, 0.235f, 0.034f, 0.024f, MaterialIds.NOSE_DARK),
                    floatArrayOf(0f, 0.130f, 0.268f, 0.026f, 0.018f, MaterialIds.NOSE_DARK)
                ),
                segments = 10
            )
            // Strong Tiger Lower Jaw & Chin (+Z)
            addSweptCurve(
                listOf(
                    floatArrayOf(0f, 0.065f, 0.07f, 0.078f, 0.048f, MaterialIds.TIGER_CREAM),
                    floatArrayOf(0f, 0.055f, 0.16f, 0.065f, 0.040f, MaterialIds.TIGER_CREAM),
                    floatArrayOf(0f, 0.058f, 0.225f, 0.050f, 0.032f, MaterialIds.TIGER_CREAM)
                ),
                segments = 10
            )
            // Upper Canine Fangs
            for (side in floatArrayOf(-1f, 1f)) {
                addSweptCurve(
                    listOf(
                        floatArrayOf(side * 0.042f, 0.095f, 0.215f, 0.011f, 0.011f, MaterialIds.FANG_CLAW),
                        floatArrayOf(side * 0.040f, 0.062f, 0.220f, 0.004f, 0.004f, MaterialIds.FANG_CLAW)
                    ),
                    segments = 6
                )
            }
            // Expressive Fierce Glowing Amber Eyes & Brow Ridges (+Z)
            for (side in floatArrayOf(-1f, 1f)) {
                addSweptCurve(
                    listOf(
                        floatArrayOf(side * 0.042f, 0.175f, 0.145f, 0.018f, 0.014f, MaterialIds.EYE_GLOW),
                        floatArrayOf(side * 0.082f, 0.185f, 0.132f, 0.015f, 0.012f, MaterialIds.EYE_GLOW)
                    ),
                    segments = 8
                )
                // Fierce Brow Ridge
                addSweptCurve(
                    listOf(
                        floatArrayOf(side * 0.025f, 0.192f, 0.155f, 0.018f, 0.014f, MaterialIds.TIGER_FUR),
                        floatArrayOf(side * 0.100f, 0.212f, 0.132f, 0.016f, 0.013f, MaterialIds.TIGER_FUR)
                    ),
                    segments = 8
                )
                // Rounded Tiger Ears on Crown
                addSweptCurve(
                    listOf(
                        floatArrayOf(side * 0.105f, 0.245f, -0.01f, 0.038f, 0.022f, MaterialIds.TIGER_FUR),
                        floatArrayOf(side * 0.135f, 0.305f, -0.02f, 0.032f, 0.018f, MaterialIds.TIGER_FUR),
                        floatArrayOf(side * 0.142f, 0.332f, -0.02f, 0.016f, 0.010f, MaterialIds.TIGER_CREAM)
                    ),
                    segments = 10
                )
            }
        }.build()

        // 4. Muscular Deltoid & Bicep Upper Arm (with golden arm ring)
        fun buildTigerUpperArm(): CompiledMesh = OrganicMeshBuilder(seg).apply {
            addLoftedTube(
                listOf(
                    LoftStation(cy = 0.05f, rx = 0.095f, rzFront = 0.095f, rzBack = 0.095f, vCoord = 0f, materialId = MaterialIds.TIGER_FUR),
                    LoftStation(cy = -0.05f, rx = 0.108f, rzFront = 0.105f, rzBack = 0.098f, muscleHarmonic2 = 0.06f, vCoord = 0.25f, materialId = MaterialIds.TIGER_FUR),
                    LoftStation(cy = -0.14f, rx = 0.096f, rzFront = 0.098f, rzBack = 0.088f, vCoord = 0.52f, materialId = MaterialIds.BELT_GOLD),
                    LoftStation(cy = -0.22f, rx = 0.088f, rzFront = 0.094f, rzBack = 0.082f, muscleHarmonic2 = 0.08f, vCoord = 0.78f, materialId = MaterialIds.TIGER_FUR),
                    LoftStation(cy = -0.31f, rx = 0.072f, rzFront = 0.074f, rzBack = 0.072f, vCoord = 1f, materialId = MaterialIds.TIGER_FUR)
                )
            )
        }.build()

        // 5. Forearm with Martial Wraps + Connected Clawed Tiger Paw-Fist
        val forearmAndHand = OrganicMeshBuilder(seg).apply {
            addLoftedTube(
                listOf(
                    LoftStation(cy = 0.04f, rx = 0.074f, rzFront = 0.074f, rzBack = 0.074f, vCoord = 0f, materialId = MaterialIds.TIGER_FUR),
                    LoftStation(cy = -0.07f, rx = 0.084f, rzFront = 0.082f, rzBack = 0.078f, muscleHarmonic2 = 0.05f, vCoord = 0.3f, materialId = MaterialIds.TIGER_FUR),
                    LoftStation(cy = -0.15f, rx = 0.072f, rzFront = 0.070f, rzBack = 0.068f, vCoord = 0.58f, materialId = MaterialIds.CLOTH_WRAPS),
                    LoftStation(cy = -0.25f, rx = 0.062f, rzFront = 0.060f, rzBack = 0.058f, vCoord = 0.80f, materialId = MaterialIds.CLOTH_WRAPS),
                    // Connected Tiger Paw Fist at end of forearm
                    LoftStation(cy = -0.31f, cz = 0.015f, rx = 0.078f, rzFront = 0.082f, rzBack = 0.068f, vCoord = 0.92f, materialId = MaterialIds.TIGER_FUR),
                    LoftStation(cy = -0.36f, cz = 0.020f, rx = 0.068f, rzFront = 0.072f, rzBack = 0.058f, vCoord = 1f, materialId = MaterialIds.TIGER_FUR)
                )
            )
            // Tiger Claws on knuckles (+Z forward)
            for (fingerX in floatArrayOf(-0.042f, -0.014f, 0.014f, 0.042f)) {
                addSweptCurve(
                    listOf(
                        floatArrayOf(fingerX, -0.355f, 0.065f, 0.010f, 0.010f, MaterialIds.FANG_CLAW),
                        floatArrayOf(fingerX, -0.388f, 0.082f, 0.004f, 0.004f, MaterialIds.FANG_CLAW)
                    ),
                    segments = 6
                )
            }
        }.build()

        // 6. Muscular Martial Trousers Thigh
        val thigh = OrganicMeshBuilder(seg).apply {
            addLoftedTube(
                listOf(
                    LoftStation(cy = 0.05f, rx = 0.115f, rzFront = 0.115f, rzBack = 0.115f, vCoord = 0f, materialId = MaterialIds.GI_CRIMSON),
                    LoftStation(cy = -0.12f, rx = 0.135f, rzFront = 0.138f, rzBack = 0.128f, muscleHarmonic2 = 0.05f, vCoord = 0.35f, materialId = MaterialIds.GI_CRIMSON),
                    LoftStation(cy = -0.28f, rx = 0.122f, rzFront = 0.120f, rzBack = 0.115f, vCoord = 0.72f, materialId = MaterialIds.GI_CRIMSON),
                    LoftStation(cy = -0.40f, rx = 0.092f, rzFront = 0.094f, rzBack = 0.090f, vCoord = 1f, materialId = MaterialIds.GI_CRIMSON)
                )
            )
        }.build()

        // 7. Defined Calf with Martial Shin Wraps
        val calf = OrganicMeshBuilder(seg).apply {
            addLoftedTube(
                listOf(
                    LoftStation(cy = 0.04f, rx = 0.090f, rzFront = 0.090f, rzBack = 0.090f, vCoord = 0f, materialId = MaterialIds.GI_CRIMSON),
                    LoftStation(cy = -0.12f, rx = 0.096f, rzFront = 0.088f, rzBack = 0.105f, muscleHarmonic2 = 0.06f, vCoord = 0.35f, materialId = MaterialIds.CLOTH_WRAPS),
                    LoftStation(cy = -0.26f, rx = 0.072f, rzFront = 0.072f, rzBack = 0.076f, vCoord = 0.72f, materialId = MaterialIds.CLOTH_WRAPS),
                    LoftStation(cy = -0.38f, rx = 0.064f, rzFront = 0.066f, rzBack = 0.066f, vCoord = 1f, materialId = MaterialIds.TIGER_FUR)
                )
            )
        }.build()

        // 8. Forward-Pointing (+Z) Striped Tiger Paw Foot with Claws
        val foot = OrganicMeshBuilder(seg).apply {
            addSweptCurve(
                listOf(
                    floatArrayOf(0f, -0.02f, -0.05f, 0.062f, 0.052f, MaterialIds.TIGER_FUR),
                    floatArrayOf(0f, -0.045f, 0.04f, 0.074f, 0.046f, MaterialIds.TIGER_FUR),
                    floatArrayOf(0f, -0.052f, 0.14f, 0.082f, 0.038f, MaterialIds.TIGER_FUR),
                    floatArrayOf(0f, -0.056f, 0.19f, 0.065f, 0.028f, MaterialIds.TIGER_CREAM)
                ),
                segments = 12
            )
            for (toeX in floatArrayOf(-0.045f, -0.015f, 0.015f, 0.045f)) {
                addSweptCurve(
                    listOf(
                        floatArrayOf(toeX, -0.052f, 0.185f, 0.011f, 0.010f, MaterialIds.FANG_CLAW),
                        floatArrayOf(toeX, -0.064f, 0.222f, 0.004f, 0.004f, MaterialIds.FANG_CLAW)
                    ),
                    segments = 6
                )
            }
        }.build()

        // 9. Expressive Striped Tiger Tail (-Z behind pelvis)
        val tail = OrganicMeshBuilder(10).apply {
            addSweptCurve(
                listOf(
                    floatArrayOf(0f, 0.02f, -0.12f, 0.042f, 0.042f, MaterialIds.TIGER_FUR),
                    floatArrayOf(0f, -0.05f, -0.32f, 0.038f, 0.038f, MaterialIds.TIGER_FUR),
                    floatArrayOf(0.03f, 0.04f, -0.52f, 0.034f, 0.034f, MaterialIds.TIGER_FUR),
                    floatArrayOf(0.05f, 0.20f, -0.66f, 0.030f, 0.030f, MaterialIds.TIGER_FUR),
                    floatArrayOf(0.02f, 0.34f, -0.72f, 0.024f, 0.024f, MaterialIds.NOSE_DARK)
                ),
                segments = 10
            )
        }.build()

        return CharacterRigMeshSet(
            pelvis = pelvis,
            torso = torso,
            head = head,
            upperArmLeft = buildTigerUpperArm(),
            upperArmRight = buildTigerUpperArm(),
            forearmAndHand = forearmAndHand,
            thigh = thigh,
            calf = calf,
            foot = foot,
            tail = tail,
            wingLeft = null,
            wingRight = null,
            hipHeight = 0.86f,
            hipHalfWidth = 0.115f,
            spineLength = 0.50f,
            neckOffsetY = 0.49f,
            shoulderOffsetY = 0.43f,
            shoulderHalfWidth = 0.285f,
            upperArmLength = 0.30f,
            forearmLength = 0.29f,
            thighLength = 0.39f,
            calfLength = 0.38f,
            ankleHeight = 0.08f
        )
    }

    private fun buildIronBuffaloRig(seg: Int): CharacterRigMeshSet {
        // 1. Heavy Armored Battle Pelvis & Championship Iron Belt
        val pelvis = OrganicMeshBuilder(seg).apply {
            addLoftedTube(
                listOf(
                    LoftStation(cy = -0.12f, rx = 0.23f, rzFront = 0.18f, rzBack = 0.19f, vCoord = 0f, materialId = MaterialIds.LEATHER_STRAP, ao = 0.8f),
                    LoftStation(cy = 0.00f, rx = 0.26f, rzFront = 0.20f, rzBack = 0.21f, vCoord = 0.5f, materialId = MaterialIds.LEATHER_STRAP),
                    LoftStation(cy = 0.11f, rx = 0.27f, rzFront = 0.22f, rzBack = 0.20f, vCoord = 1f, materialId = MaterialIds.IRON_ARMOR)
                )
            )
        }.build()

        // 2. Massive Barrel Chest, Trapezius Buffalo Hump & Crossed Leather/Iron Harness
        val torso = OrganicMeshBuilder(seg).apply {
            addLoftedTube(
                listOf(
                    LoftStation(cy = -0.03f, rx = 0.25f, rzFront = 0.19f, rzBack = 0.19f, vCoord = 0f, materialId = MaterialIds.IRON_ARMOR, ao = 0.85f),
                    LoftStation(cy = 0.14f, rx = 0.29f, rzFront = 0.23f, rzBack = 0.22f, muscleHarmonic2 = 0.07f, vCoord = 0.28f, materialId = MaterialIds.BUFFALO_FUR),
                    LoftStation(cy = 0.32f, rx = 0.36f, rzFront = 0.28f, rzBack = 0.26f, muscleHarmonic2 = 0.10f, vCoord = 0.62f, materialId = MaterialIds.BUFFALO_FUR),
                    // Powerful Buffalo Shoulder Hump (-Z) & Thick Neck Base (+Z)
                    LoftStation(cy = 0.48f, cz = -0.02f, rx = 0.38f, rzFront = 0.27f, rzBack = 0.30f, muscleHarmonic2 = 0.08f, vCoord = 0.86f, materialId = MaterialIds.BUFFALO_FUR),
                    LoftStation(cy = 0.58f, cz = 0.04f, rx = 0.21f, rzFront = 0.20f, rzBack = 0.22f, vCoord = 1f, materialId = MaterialIds.BUFFALO_FUR)
                )
            )
            // Crossed Leather Chest Straps & Iron Medallion (+Z)
            for (side in floatArrayOf(-1f, 1f)) {
                addSweptCurve(
                    listOf(
                        floatArrayOf(side * 0.22f, 0.02f, 0.19f, 0.032f, 0.020f, MaterialIds.LEATHER_STRAP),
                        floatArrayOf(0f, 0.28f, 0.285f, 0.038f, 0.022f, MaterialIds.IRON_ARMOR),
                        floatArrayOf(-side * 0.25f, 0.50f, 0.16f, 0.034f, 0.020f, MaterialIds.LEATHER_STRAP)
                    ),
                    segments = 8
                )
            }
        }.build()

        // 3. Recognizable Buffalo Head: Broad Muzzle, Strong Jaw, Horn Boss & Sweeping Curved Ridged Horns
        val head = OrganicMeshBuilder(seg).apply {
            // Thick Muscular Bovine Neck & Skull
            addLoftedTube(
                listOf(
                    LoftStation(cy = -0.05f, cz = -0.02f, rx = 0.15f, rzFront = 0.15f, rzBack = 0.16f, vCoord = 0f, materialId = MaterialIds.BUFFALO_FUR, ao = 0.82f),
                    LoftStation(cy = 0.08f, cz = 0.03f, rx = 0.18f, rzFront = 0.17f, rzBack = 0.16f, vCoord = 0.4f, materialId = MaterialIds.BUFFALO_FUR),
                    LoftStation(cy = 0.20f, cz = 0.04f, rx = 0.19f, rzFront = 0.17f, rzBack = 0.15f, vCoord = 0.78f, materialId = MaterialIds.BUFFALO_FUR),
                    // Heavy Horn Boss Crown
                    LoftStation(cy = 0.28f, cz = 0.02f, rx = 0.17f, rzFront = 0.14f, rzBack = 0.13f, vCoord = 1f, materialId = MaterialIds.BUFFALO_HORN)
                )
            )
            // Broad Buffalo Muzzle & Wet Nose Pad (+Z forward!)
            addSweptCurve(
                listOf(
                    floatArrayOf(0f, 0.16f, 0.09f, 0.125f, 0.098f, MaterialIds.BUFFALO_FUR),
                    floatArrayOf(0f, 0.12f, 0.21f, 0.115f, 0.086f, MaterialIds.BUFFALO_FUR),
                    floatArrayOf(0f, 0.10f, 0.30f, 0.105f, 0.072f, MaterialIds.NOSE_DARK)
                ),
                segments = 14
            )
            // Strong Bovine Lower Jaw & Beard Tuft (+Z)
            addSweptCurve(
                listOf(
                    floatArrayOf(0f, 0.06f, 0.09f, 0.098f, 0.058f, MaterialIds.BUFFALO_FUR),
                    floatArrayOf(0f, 0.04f, 0.23f, 0.084f, 0.048f, MaterialIds.BUFFALO_FUR),
                    floatArrayOf(0f, -0.02f, 0.25f, 0.040f, 0.038f, MaterialIds.BUFFALO_FUR)
                ),
                segments = 10
            )
            // Expressive Glowing Eyes, Heavy Brow & Sweeping Curved Buffalo Horns!
            for (side in floatArrayOf(-1f, 1f)) {
                addSweptCurve(
                    listOf(
                        floatArrayOf(side * 0.095f, 0.185f, 0.145f, 0.019f, 0.015f, MaterialIds.EYE_GLOW),
                        floatArrayOf(side * 0.128f, 0.190f, 0.125f, 0.016f, 0.013f, MaterialIds.EYE_GLOW)
                    ),
                    segments = 8
                )
                // Side Bovine Ears beneath Horns
                addSweptCurve(
                    listOf(
                        floatArrayOf(side * 0.15f, 0.17f, 0.01f, 0.032f, 0.020f, MaterialIds.BUFFALO_FUR),
                        floatArrayOf(side * 0.25f, 0.14f, -0.02f, 0.028f, 0.014f, MaterialIds.BUFFALO_FUR)
                    ),
                    segments = 8
                )
                // Iconic Curved Buffalo Horns: thick boss -> sweep outward (±X), curve forward (+Z) and up (+Y)!
                addSweptCurve(
                    listOf(
                        floatArrayOf(side * 0.06f, 0.26f, 0.03f, 0.068f, 0.056f, MaterialIds.BUFFALO_HORN),
                        floatArrayOf(side * 0.19f, 0.24f, 0.02f, 0.060f, 0.050f, MaterialIds.BUFFALO_HORN),
                        floatArrayOf(side * 0.31f, 0.26f, 0.06f, 0.048f, 0.042f, MaterialIds.BUFFALO_HORN),
                        floatArrayOf(side * 0.37f, 0.34f, 0.14f, 0.034f, 0.032f, MaterialIds.BUFFALO_HORN),
                        floatArrayOf(side * 0.33f, 0.43f, 0.21f, 0.018f, 0.018f, MaterialIds.BUFFALO_HORN)
                    ),
                    segments = 12
                )
            }
        }.build()

        // 4. Spiked Gunmetal Pauldron + Massive Muscular Upper Arm
        fun buildBuffaloUpperArm(sideSign: Float): CompiledMesh = OrganicMeshBuilder(seg).apply {
            // Muscular upper arm
            addLoftedTube(
                listOf(
                    LoftStation(cy = 0.06f, rx = 0.125f, rzFront = 0.125f, rzBack = 0.125f, vCoord = 0f, materialId = MaterialIds.BUFFALO_FUR),
                    LoftStation(cy = -0.08f, rx = 0.142f, rzFront = 0.138f, rzBack = 0.132f, muscleHarmonic2 = 0.08f, vCoord = 0.32f, materialId = MaterialIds.BUFFALO_FUR),
                    LoftStation(cy = -0.21f, rx = 0.122f, rzFront = 0.126f, rzBack = 0.115f, muscleHarmonic2 = 0.09f, vCoord = 0.68f, materialId = MaterialIds.BUFFALO_FUR),
                    LoftStation(cy = -0.33f, rx = 0.098f, rzFront = 0.098f, rzBack = 0.095f, vCoord = 1f, materialId = MaterialIds.BUFFALO_FUR)
                )
            )
            // Armored Steel Shoulder Pauldron + Spikes
            addLoftedTube(
                listOf(
                    LoftStation(cx = sideSign * 0.02f, cy = -0.06f, rx = 0.155f, rzFront = 0.152f, rzBack = 0.152f, vCoord = 0f, materialId = MaterialIds.IRON_ARMOR),
                    LoftStation(cx = sideSign * 0.02f, cy = 0.05f, rx = 0.162f, rzFront = 0.158f, rzBack = 0.158f, vCoord = 0.6f, materialId = MaterialIds.IRON_ARMOR),
                    LoftStation(cx = 0f, cy = 0.11f, rx = 0.110f, rzFront = 0.115f, rzBack = 0.115f, vCoord = 1f, materialId = MaterialIds.IRON_ARMOR)
                ),
                segments = 12
            )
            addSweptCurve(
                listOf(
                    floatArrayOf(sideSign * 0.13f, 0.05f, 0f, 0.026f, 0.026f, MaterialIds.IRON_ARMOR),
                    floatArrayOf(sideSign * 0.22f, 0.13f, 0f, 0.005f, 0.005f, MaterialIds.IRON_ARMOR)
                ),
                segments = 6
            )
        }.build()

        // 5. Heavy Armored Iron Gauntlet Forearm + Massive Clenched Fist
        val forearmAndHand = OrganicMeshBuilder(seg).apply {
            addLoftedTube(
                listOf(
                    LoftStation(cy = 0.05f, rx = 0.098f, rzFront = 0.098f, rzBack = 0.098f, vCoord = 0f, materialId = MaterialIds.BUFFALO_FUR),
                    LoftStation(cy = -0.08f, rx = 0.118f, rzFront = 0.115f, rzBack = 0.112f, vCoord = 0.30f, materialId = MaterialIds.IRON_ARMOR),
                    LoftStation(cy = -0.24f, rx = 0.096f, rzFront = 0.096f, rzBack = 0.094f, vCoord = 0.72f, materialId = MaterialIds.IRON_ARMOR),
                    // Heavy Brawler Fist directly connected at wrist
                    LoftStation(cy = -0.31f, cz = 0.02f, rx = 0.108f, rzFront = 0.112f, rzBack = 0.092f, vCoord = 0.90f, materialId = MaterialIds.IRON_ARMOR),
                    LoftStation(cy = -0.38f, cz = 0.025f, rx = 0.096f, rzFront = 0.102f, rzBack = 0.082f, vCoord = 1f, materialId = MaterialIds.BUFFALO_FUR)
                )
            )
        }.build()

        // 6. Massive Thigh with Armored Tasset
        val thigh = OrganicMeshBuilder(seg).apply {
            addLoftedTube(
                listOf(
                    LoftStation(cy = 0.05f, rx = 0.150f, rzFront = 0.148f, rzBack = 0.148f, vCoord = 0f, materialId = MaterialIds.LEATHER_STRAP),
                    LoftStation(cy = -0.14f, rx = 0.168f, rzFront = 0.165f, rzBack = 0.158f, muscleHarmonic2 = 0.06f, vCoord = 0.38f, materialId = MaterialIds.LEATHER_STRAP),
                    LoftStation(cy = -0.30f, rx = 0.142f, rzFront = 0.140f, rzBack = 0.135f, vCoord = 0.75f, materialId = MaterialIds.BUFFALO_FUR),
                    LoftStation(cy = -0.41f, rx = 0.115f, rzFront = 0.118f, rzBack = 0.112f, vCoord = 1f, materialId = MaterialIds.IRON_ARMOR)
                )
            )
        }.build()

        // 7. Heavy Steel-Greaved Calf
        val calf = OrganicMeshBuilder(seg).apply {
            addLoftedTube(
                listOf(
                    LoftStation(cy = 0.05f, rx = 0.115f, rzFront = 0.118f, rzBack = 0.112f, vCoord = 0f, materialId = MaterialIds.IRON_ARMOR),
                    LoftStation(cy = -0.14f, rx = 0.125f, rzFront = 0.122f, rzBack = 0.132f, vCoord = 0.38f, materialId = MaterialIds.IRON_ARMOR),
                    LoftStation(cy = -0.28f, rx = 0.098f, rzFront = 0.100f, rzBack = 0.102f, vCoord = 0.76f, materialId = MaterialIds.IRON_ARMOR),
                    LoftStation(cy = -0.39f, rx = 0.088f, rzFront = 0.092f, rzBack = 0.088f, vCoord = 1f, materialId = MaterialIds.BUFFALO_FUR)
                )
            )
        }.build()

        // 8. Heavy Iron-Shod Cloven Buffalo Hoof (+Z forward!)
        val foot = OrganicMeshBuilder(seg).apply {
            addSweptCurve(
                listOf(
                    floatArrayOf(0f, -0.02f, -0.06f, 0.084f, 0.065f, MaterialIds.BUFFALO_FUR),
                    floatArrayOf(0f, -0.05f, 0.05f, 0.098f, 0.058f, MaterialIds.IRON_ARMOR),
                    floatArrayOf(0f, -0.065f, 0.16f, 0.108f, 0.050f, MaterialIds.IRON_ARMOR),
                    floatArrayOf(0f, -0.072f, 0.22f, 0.092f, 0.038f, MaterialIds.BUFFALO_HORN)
                ),
                segments = 12
            )
        }.build()

        // 9. Short Tufted Buffalo Tail (-Z)
        val tail = OrganicMeshBuilder(8).apply {
            addSweptCurve(
                listOf(
                    floatArrayOf(0f, 0.02f, -0.18f, 0.032f, 0.032f, MaterialIds.BUFFALO_FUR),
                    floatArrayOf(0f, -0.14f, -0.28f, 0.026f, 0.026f, MaterialIds.BUFFALO_FUR),
                    floatArrayOf(0f, -0.28f, -0.31f, 0.044f, 0.044f, MaterialIds.BUFFALO_FUR)
                ),
                segments = 8
            )
        }.build()

        return CharacterRigMeshSet(
            pelvis = pelvis,
            torso = torso,
            head = head,
            upperArmLeft = buildBuffaloUpperArm(1f),
            upperArmRight = buildBuffaloUpperArm(-1f),
            forearmAndHand = forearmAndHand,
            thigh = thigh,
            calf = calf,
            foot = foot,
            tail = tail,
            wingLeft = null,
            wingRight = null,
            hipHeight = 0.89f,
            hipHalfWidth = 0.145f,
            spineLength = 0.56f,
            neckOffsetY = 0.55f,
            shoulderOffsetY = 0.48f,
            shoulderHalfWidth = 0.355f,
            upperArmLength = 0.32f,
            forearmLength = 0.31f,
            thighLength = 0.40f,
            calfLength = 0.39f,
            ankleHeight = 0.09f
        )
    }

    private fun buildDragonBossRig(seg: Int): CharacterRigMeshSet {
        // Colossal 2.35m Inferno Dragon Boss!
        val pelvis = OrganicMeshBuilder(seg).apply {
            addLoftedTube(
                listOf(
                    LoftStation(cy = -0.14f, rx = 0.26f, rzFront = 0.21f, rzBack = 0.24f, vCoord = 0f, materialId = MaterialIds.DRAGON_SCALES, ao = 0.8f),
                    LoftStation(cy = 0.02f, rx = 0.29f, rzFront = 0.23f, rzBack = 0.26f, vCoord = 0.6f, materialId = MaterialIds.DRAGON_SCALES),
                    LoftStation(cy = 0.14f, rx = 0.28f, rzFront = 0.22f, rzBack = 0.23f, vCoord = 1f, materialId = MaterialIds.DRAGON_MAGMA)
                )
            )
        }.build()

        val torso = OrganicMeshBuilder(seg).apply {
            addLoftedTube(
                listOf(
                    LoftStation(cy = -0.04f, rx = 0.27f, rzFront = 0.22f, rzBack = 0.23f, vCoord = 0f, materialId = MaterialIds.DRAGON_SCALES),
                    LoftStation(cy = 0.18f, rx = 0.33f, rzFront = 0.27f, rzBack = 0.26f, muscleHarmonic2 = 0.08f, vCoord = 0.3f, materialId = MaterialIds.DRAGON_SCALES),
                    LoftStation(cy = 0.42f, rx = 0.42f, rzFront = 0.33f, rzBack = 0.30f, muscleHarmonic2 = 0.10f, vCoord = 0.65f, materialId = MaterialIds.DRAGON_SCALES),
                    LoftStation(cy = 0.60f, cz = 0.03f, rx = 0.36f, rzFront = 0.28f, rzBack = 0.28f, vCoord = 0.86f, materialId = MaterialIds.DRAGON_SCALES),
                    LoftStation(cy = 0.72f, cz = 0.08f, rx = 0.20f, rzFront = 0.20f, rzBack = 0.20f, vCoord = 1f, materialId = MaterialIds.DRAGON_SCALES)
                )
            )
            // Glowing Molten Magma Chest Core (+Z)
            addLoftedTube(
                listOf(
                    LoftStation(cy = 0.08f, cz = 0.16f, rx = 0.13f, rzFront = 0.12f, rzBack = 0.04f, vCoord = 0.2f, materialId = MaterialIds.DRAGON_MAGMA),
                    LoftStation(cy = 0.38f, cz = 0.22f, rx = 0.19f, rzFront = 0.14f, rzBack = 0.04f, vCoord = 0.6f, materialId = MaterialIds.DRAGON_MAGMA),
                    LoftStation(cy = 0.62f, cz = 0.18f, rx = 0.13f, rzFront = 0.12f, rzBack = 0.04f, vCoord = 1f, materialId = MaterialIds.DRAGON_MAGMA)
                ),
                segments = 12
            )
            // Dorsal Back Spines (-Z)
            for (spineY in floatArrayOf(0.16f, 0.34f, 0.52f, 0.66f)) {
                addSweptCurve(
                    listOf(
                        floatArrayOf(0f, spineY, -0.26f, 0.035f, 0.028f, MaterialIds.BUFFALO_HORN),
                        floatArrayOf(0f, spineY + 0.09f, -0.42f, 0.006f, 0.006f, MaterialIds.FANG_CLAW)
                    ),
                    segments = 6
                )
            }
        }.build()

        // Detailed Dragon Head: Armored Snout, Hinged Fiery Jaw, Fangs, 4 Swept Horns!
        val head = OrganicMeshBuilder(seg).apply {
            // Sinuous Armored Dragon Neck & Cranium
            addSweptCurve(
                listOf(
                    floatArrayOf(0f, -0.06f, -0.02f, 0.17f, 0.17f, MaterialIds.DRAGON_SCALES),
                    floatArrayOf(0f, 0.12f, 0.06f, 0.18f, 0.17f, MaterialIds.DRAGON_SCALES),
                    floatArrayOf(0f, 0.26f, 0.12f, 0.20f, 0.16f, MaterialIds.DRAGON_SCALES)
                ),
                segments = 14
            )
            // Long Armored Dragon Upper Snout (+Z)
            addSweptCurve(
                listOf(
                    floatArrayOf(0f, 0.25f, 0.12f, 0.16f, 0.11f, MaterialIds.DRAGON_SCALES),
                    floatArrayOf(0f, 0.23f, 0.30f, 0.12f, 0.085f, MaterialIds.DRAGON_SCALES),
                    floatArrayOf(0f, 0.22f, 0.46f, 0.09f, 0.065f, MaterialIds.DRAGON_SCALES)
                ),
                segments = 12
            )
            // Roaring Molten Lower Jaw (+Z)
            addSweptCurve(
                listOf(
                    floatArrayOf(0f, 0.14f, 0.12f, 0.13f, 0.065f, MaterialIds.DRAGON_MAGMA),
                    floatArrayOf(0f, 0.10f, 0.28f, 0.10f, 0.052f, MaterialIds.DRAGON_MAGMA),
                    floatArrayOf(0f, 0.09f, 0.42f, 0.07f, 0.038f, MaterialIds.DRAGON_SCALES)
                ),
                segments = 10
            )
            // Fierce Dragon Fangs, Eyes & 4 Crown Horns
            for (side in floatArrayOf(-1f, 1f)) {
                for (zPos in floatArrayOf(0.28f, 0.36f, 0.43f)) {
                    addSweptCurve(
                        listOf(
                            floatArrayOf(side * 0.065f, 0.19f, zPos, 0.014f, 0.014f, MaterialIds.FANG_CLAW),
                            floatArrayOf(side * 0.060f, 0.135f, zPos + 0.01f, 0.004f, 0.004f, MaterialIds.FANG_CLAW)
                        ),
                        segments = 6
                    )
                }
                // Glowing Golden Dragon Eyes
                addSweptCurve(
                    listOf(
                        floatArrayOf(side * 0.095f, 0.28f, 0.22f, 0.025f, 0.018f, MaterialIds.EYE_GLOW),
                        floatArrayOf(side * 0.135f, 0.29f, 0.19f, 0.020f, 0.015f, MaterialIds.EYE_GLOW)
                    ),
                    segments = 8
                )
                // Primary Swept-Back Crown Horns
                addSweptCurve(
                    listOf(
                        floatArrayOf(side * 0.10f, 0.31f, 0.10f, 0.055f, 0.050f, MaterialIds.BUFFALO_HORN),
                        floatArrayOf(side * 0.18f, 0.42f, -0.06f, 0.042f, 0.038f, MaterialIds.BUFFALO_HORN),
                        floatArrayOf(side * 0.24f, 0.56f, -0.24f, 0.026f, 0.024f, MaterialIds.BUFFALO_HORN),
                        floatArrayOf(side * 0.26f, 0.68f, -0.38f, 0.008f, 0.008f, MaterialIds.FANG_CLAW)
                    ),
                    segments = 10
                )
                // Secondary Cheek Spikes
                addSweptCurve(
                    listOf(
                        floatArrayOf(side * 0.15f, 0.20f, 0.08f, 0.032f, 0.026f, MaterialIds.BUFFALO_HORN),
                        floatArrayOf(side * 0.26f, 0.22f, -0.06f, 0.007f, 0.007f, MaterialIds.FANG_CLAW)
                    ),
                    segments = 6
                )
            }
        }.build()

        fun buildDragonUpperArm(): CompiledMesh = OrganicMeshBuilder(seg).apply {
            addLoftedTube(
                listOf(
                    LoftStation(cy = 0.06f, rx = 0.14f, rzFront = 0.14f, rzBack = 0.14f, vCoord = 0f, materialId = MaterialIds.DRAGON_SCALES),
                    LoftStation(cy = -0.14f, rx = 0.155f, rzFront = 0.15f, rzBack = 0.14f, muscleHarmonic2 = 0.08f, vCoord = 0.45f, materialId = MaterialIds.DRAGON_SCALES),
                    LoftStation(cy = -0.38f, rx = 0.115f, rzFront = 0.115f, rzBack = 0.11f, vCoord = 1f, materialId = MaterialIds.DRAGON_SCALES)
                )
            )
        }.build()

        val forearmAndHand = OrganicMeshBuilder(seg).apply {
            addLoftedTube(
                listOf(
                    LoftStation(cy = 0.05f, rx = 0.115f, rzFront = 0.115f, rzBack = 0.115f, vCoord = 0f, materialId = MaterialIds.DRAGON_SCALES),
                    LoftStation(cy = -0.18f, rx = 0.122f, rzFront = 0.120f, rzBack = 0.115f, vCoord = 0.55f, materialId = MaterialIds.DRAGON_SCALES),
                    LoftStation(cy = -0.34f, cz = 0.02f, rx = 0.110f, rzFront = 0.115f, rzBack = 0.095f, vCoord = 0.88f, materialId = MaterialIds.DRAGON_MAGMA),
                    LoftStation(cy = -0.42f, cz = 0.04f, rx = 0.095f, rzFront = 0.100f, rzBack = 0.080f, vCoord = 1f, materialId = MaterialIds.DRAGON_SCALES)
                )
            )
            // Lethal Dragon Talons
            for (clawX in floatArrayOf(-0.055f, 0f, 0.055f)) {
                addSweptCurve(
                    listOf(
                        floatArrayOf(clawX, -0.41f, 0.08f, 0.018f, 0.018f, MaterialIds.FANG_CLAW),
                        floatArrayOf(clawX, -0.49f, 0.13f, 0.005f, 0.005f, MaterialIds.FANG_CLAW)
                    ),
                    segments = 6
                )
            }
        }.build()

        val thigh = OrganicMeshBuilder(seg).apply {
            addLoftedTube(
                listOf(
                    LoftStation(cy = 0.06f, rx = 0.175f, rzFront = 0.175f, rzBack = 0.175f, vCoord = 0f, materialId = MaterialIds.DRAGON_SCALES),
                    LoftStation(cy = -0.20f, rx = 0.195f, rzFront = 0.190f, rzBack = 0.180f, muscleHarmonic2 = 0.08f, vCoord = 0.48f, materialId = MaterialIds.DRAGON_SCALES),
                    LoftStation(cy = -0.48f, rx = 0.135f, rzFront = 0.135f, rzBack = 0.130f, vCoord = 1f, materialId = MaterialIds.DRAGON_SCALES)
                )
            )
        }.build()

        val calf = OrganicMeshBuilder(seg).apply {
            addLoftedTube(
                listOf(
                    LoftStation(cy = 0.05f, rx = 0.135f, rzFront = 0.135f, rzBack = 0.135f, vCoord = 0f, materialId = MaterialIds.DRAGON_SCALES),
                    LoftStation(cy = -0.20f, rx = 0.142f, rzFront = 0.132f, rzBack = 0.150f, vCoord = 0.45f, materialId = MaterialIds.DRAGON_SCALES),
                    LoftStation(cy = -0.46f, rx = 0.105f, rzFront = 0.105f, rzBack = 0.105f, vCoord = 1f, materialId = MaterialIds.DRAGON_SCALES)
                )
            )
        }.build()

        val foot = OrganicMeshBuilder(seg).apply {
            addSweptCurve(
                listOf(
                    floatArrayOf(0f, -0.02f, -0.08f, 0.10f, 0.07f, MaterialIds.DRAGON_SCALES),
                    floatArrayOf(0f, -0.06f, 0.08f, 0.12f, 0.06f, MaterialIds.DRAGON_SCALES),
                    floatArrayOf(0f, -0.08f, 0.24f, 0.13f, 0.05f, MaterialIds.DRAGON_SCALES)
                ),
                segments = 12
            )
            for (toeX in floatArrayOf(-0.065f, 0f, 0.065f)) {
                addSweptCurve(
                    listOf(
                        floatArrayOf(toeX, -0.075f, 0.23f, 0.020f, 0.018f, MaterialIds.FANG_CLAW),
                        floatArrayOf(toeX, -0.095f, 0.32f, 0.005f, 0.005f, MaterialIds.FANG_CLAW)
                    ),
                    segments = 6
                )
            }
        }.build()

        // Powerful Heavy Spiked Dragon Tail (-Z)
        val tail = OrganicMeshBuilder(12).apply {
            addSweptCurve(
                listOf(
                    floatArrayOf(0f, 0.02f, -0.18f, 0.135f, 0.125f, MaterialIds.DRAGON_SCALES),
                    floatArrayOf(0f, -0.14f, -0.52f, 0.110f, 0.100f, MaterialIds.DRAGON_SCALES),
                    floatArrayOf(0f, -0.22f, -0.88f, 0.082f, 0.078f, MaterialIds.DRAGON_SCALES),
                    floatArrayOf(0.05f, -0.12f, -1.22f, 0.058f, 0.055f, MaterialIds.DRAGON_MAGMA),
                    floatArrayOf(0.08f, 0.06f, -1.48f, 0.072f, 0.042f, MaterialIds.BUFFALO_HORN)
                ),
                segments = 12
            )
        }.build()

        // Articulated Dragon Wings (Left = +X, Right = -X)
        fun buildWing(side: Float): CompiledMesh = OrganicMeshBuilder(8).apply {
            val pShoulder = floatArrayOf(0f, 0f, 0f)
            val pApex = floatArrayOf(side * 0.62f, 0.48f, -0.24f)
            val pTip = floatArrayOf(side * 1.18f, 0.24f, -0.52f)
            val pMidTrailing = floatArrayOf(side * 0.72f, -0.26f, -0.44f)
            val pBaseTrailing = floatArrayOf(side * 0.16f, -0.32f, -0.18f)

            // Wing bone spars
            addSweptCurve(
                listOf(
                    floatArrayOf(0f, 0f, 0f, 0.045f, 0.045f, MaterialIds.DRAGON_SCALES),
                    floatArrayOf(pApex[0], pApex[1], pApex[2], 0.036f, 0.036f, MaterialIds.DRAGON_SCALES),
                    floatArrayOf(pTip[0], pTip[1], pTip[2], 0.016f, 0.016f, MaterialIds.BUFFALO_HORN)
                ),
                segments = 8
            )
            addSweptCurve(
                listOf(
                    floatArrayOf(pApex[0], pApex[1], pApex[2], 0.024f, 0.024f, MaterialIds.BUFFALO_HORN),
                    floatArrayOf(pMidTrailing[0], pMidTrailing[1], pMidTrailing[2], 0.010f, 0.010f, MaterialIds.FANG_CLAW)
                ),
                segments = 6
            )
            // Wing leathery crimson membranes
            addDoubleSidedQuad(pShoulder, pApex, pMidTrailing, pBaseTrailing, MaterialIds.DRAGON_WING)
            addDoubleSidedQuad(pApex, pTip, pMidTrailing, pApex, MaterialIds.DRAGON_WING)
        }.build()

        return CharacterRigMeshSet(
            pelvis = pelvis,
            torso = torso,
            head = head,
            upperArmLeft = buildDragonUpperArm(),
            upperArmRight = buildDragonUpperArm(),
            forearmAndHand = forearmAndHand,
            thigh = thigh,
            calf = calf,
            foot = foot,
            tail = tail,
            wingLeft = buildWing(1f),
            wingRight = buildWing(-1f),
            hipHeight = 1.04f,
            hipHalfWidth = 0.175f,
            spineLength = 0.70f,
            neckOffsetY = 0.69f,
            shoulderOffsetY = 0.58f,
            shoulderHalfWidth = 0.42f,
            upperArmLength = 0.37f,
            forearmLength = 0.36f,
            thighLength = 0.47f,
            calfLength = 0.46f,
            ankleHeight = 0.11f
        )
    }
}
