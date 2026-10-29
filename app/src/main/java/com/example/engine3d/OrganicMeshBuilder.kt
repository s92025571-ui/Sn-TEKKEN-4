package com.example.engine3d

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object MaterialIds {
    const val TIGER_FUR = 1f
    const val TIGER_CREAM = 2f
    const val NOSE_DARK = 3f
    const val EYE_GLOW = 4f
    const val FANG_CLAW = 5f
    const val GI_CRIMSON = 6f
    const val BELT_GOLD = 7f
    const val CLOTH_WRAPS = 8f
    const val BUFFALO_FUR = 9f
    const val BUFFALO_HORN = 10f
    const val IRON_ARMOR = 11f
    const val LEATHER_STRAP = 12f
    const val DRAGON_SCALES = 13f
    const val DRAGON_MAGMA = 14f
    const val DRAGON_WING = 15f
    const val ARENA_FLOOR = 16f
    const val ARENA_PILLAR = 17f
    const val CONTACT_SHADOW = 18f
    const val PARTICLE_FX = 19f
    const val TELEGRAPH_ZONE = 20f
    const val FIRE_BEAM = 21f
}

/**
 * Interleaved vertex layout (10 floats per vertex):
 * [posX, posY, posZ, normX, normY, normZ, u, v, materialId, aoFactor]
 */
class CompiledMesh(
    val vertexBuffer: FloatBuffer,
    val indexBuffer: ShortBuffer,
    val indexCount: Int
) {
    companion object {
        const val FLOATS_PER_VERTEX = 10
        const val STRIDE_BYTES = FLOATS_PER_VERTEX * 4
    }
}

data class LoftStation(
    val cx: Float = 0f,
    val cy: Float,
    val cz: Float = 0f,
    val rx: Float,
    val rzFront: Float,
    val rzBack: Float = rzFront,
    val muscleHarmonic2: Float = 0f,
    val vCoord: Float = 0f,
    val materialId: Float,
    val ao: Float = 1f
)

class OrganicMeshBuilder(private val defaultSegments: Int = 16) {

    private val pos = ArrayList<Float>(4096)
    private val norm = ArrayList<Float>(4096)
    private val uv = ArrayList<Float>(2048)
    private val mat = ArrayList<Float>(1024)
    private val aoList = ArrayList<Float>(1024)
    private val indices = ArrayList<Short>(6144)

    val vertexCount: Int get() = mat.size

    fun addVertex(
        x: Float,
        y: Float,
        z: Float,
        u: Float,
        v: Float,
        materialId: Float,
        ao: Float = 1f
    ): Int {
        val idx = mat.size
        pos.add(x)
        pos.add(y)
        pos.add(z)
        norm.add(0f)
        norm.add(0f)
        norm.add(0f)
        uv.add(u)
        uv.add(v)
        mat.add(materialId)
        aoList.add(ao)
        return idx
    }

    fun addTriangle(i0: Int, i1: Int, i2: Int) {
        indices.add(i0.toShort())
        indices.add(i1.toShort())
        indices.add(i2.toShort())
    }

    /**
     * Lofts an organic muscular body part or limb across anatomical stations.
     * Supports asymmetric front (+Z) vs back (-Z) radii, muscle definition harmonics,
     * and closed rounded dome caps at both ends for seamless joint connections.
     */
    fun addLoftedTube(
        stations: List<LoftStation>,
        segments: Int = defaultSegments,
        capStart: Boolean = true,
        capEnd: Boolean = true
    ) {
        if (stations.size < 2) return
        val segs = segments.coerceAtLeast(8)
        val ringIndices = Array(stations.size) { IntArray(segs) }

        for (sIdx in stations.indices) {
            val st = stations[sIdx]
            for (r in 0 until segs) {
                val theta = (2.0 * PI * r.toDouble()) / segs.toDouble()
                val sinT = sin(theta).toFloat()
                val cosT = cos(theta).toFloat()
                val rz = if (cosT >= 0f) st.rzFront else st.rzBack
                val muscleMod = 1f + st.muscleHarmonic2 * cos(2.0 * theta).toFloat()
                val vx = st.cx + sinT * st.rx * muscleMod
                val vz = st.cz + cosT * rz * muscleMod
                val vy = st.cy
                val u = r.toFloat() / segs.toFloat()
                ringIndices[sIdx][r] = addVertex(vx, vy, vz, u, st.vCoord, st.materialId, st.ao)
            }
        }

        // Stitch adjacent rings with outward-facing counter-clockwise winding
        for (sIdx in 0 until stations.size - 1) {
            val rA = ringIndices[sIdx]
            val rB = ringIndices[sIdx + 1]
            // Determine axial direction to ensure outward normals
            val dy = stations[sIdx + 1].cy - stations[sIdx].cy
            val dz = stations[sIdx + 1].cz - stations[sIdx].cz
            val flip = (dy < 0f) || (kotlin.math.abs(dy) < 1e-4f && dz < 0f)

            for (r in 0 until segs) {
                val rNext = (r + 1) % segs
                val a0 = rA[r]
                val a1 = rA[rNext]
                val b0 = rB[r]
                val b1 = rB[rNext]
                if (!flip) {
                    addTriangle(a0, a1, b1)
                    addTriangle(a0, b1, b0)
                } else {
                    addTriangle(a0, b1, a1)
                    addTriangle(a0, b0, b1)
                }
            }
        }

        if (capStart) {
            val first = stations.first()
            val second = stations[1]
            val dirY = (first.cy - second.cy).coerceIn(-0.04f, 0.04f)
            val dirZ = (first.cz - second.cz).coerceIn(-0.04f, 0.04f)
            val centerIdx = addVertex(
                first.cx,
                first.cy + dirY * 0.35f,
                first.cz + dirZ * 0.35f,
                0.5f,
                first.vCoord,
                first.materialId,
                first.ao
            )
            val dy = second.cy - first.cy
            val dz = second.cz - first.cz
            val flip = (dy < 0f) || (kotlin.math.abs(dy) < 1e-4f && dz < 0f)
            val ring = ringIndices.first()
            for (r in 0 until segs) {
                val rNext = (r + 1) % segs
                if (!flip) {
                    addTriangle(centerIdx, ring[rNext], ring[r])
                } else {
                    addTriangle(centerIdx, ring[r], ring[rNext])
                }
            }
        }

        if (capEnd) {
            val last = stations.last()
            val prev = stations[stations.size - 2]
            val dirY = (last.cy - prev.cy).coerceIn(-0.04f, 0.04f)
            val dirZ = (last.cz - prev.cz).coerceIn(-0.04f, 0.04f)
            val centerIdx = addVertex(
                last.cx,
                last.cy + dirY * 0.35f,
                last.cz + dirZ * 0.35f,
                0.5f,
                last.vCoord,
                last.materialId,
                last.ao
            )
            val dy = last.cy - prev.cy
            val dz = last.cz - prev.cz
            val flip = (dy < 0f) || (kotlin.math.abs(dy) < 1e-4f && dz < 0f)
            val ring = ringIndices.last()
            for (r in 0 until segs) {
                val rNext = (r + 1) % segs
                if (!flip) {
                    addTriangle(centerIdx, ring[r], ring[rNext])
                } else {
                    addTriangle(centerIdx, ring[rNext], ring[r])
                }
            }
        }
    }

    /**
     * Sweeps a 3D spine path (for curved horns, tails, claws, ears, and brow ridges)
     * using parallel-transport local frames so curved horns & tails never twist or flatten.
     */
    fun addSweptCurve(
        centers: List<FloatArray>, // each [x, y, z, radiusX, radiusY, materialId]
        segments: Int = 12,
        capTip: Boolean = true
    ) {
        if (centers.size < 2) return
        val segs = segments.coerceAtLeast(6)
        val rings = Array(centers.size) { IntArray(segs) }

        for (i in centers.indices) {
            val c = centers[i]
            val prev = centers[(i - 1).coerceAtLeast(0)]
            val next = centers[(i + 1).coerceAtMost(centers.lastIndex)]
            var tx = next[0] - prev[0]
            var ty = next[1] - prev[1]
            var tz = next[2] - prev[2]
            val tLen = sqrt(tx * tx + ty * ty + tz * tz).coerceAtLeast(1e-5f)
            tx /= tLen; ty /= tLen; tz /= tLen

            // Stable up reference
            var ux = 0f; var uy = 1f; var uz = 0f
            if (kotlin.math.abs(ty) > 0.92f) {
                ux = 0f; uy = 0f; uz = 1f
            }
            // Binormal B = T x U
            var bx = ty * uz - tz * uy
            var by = tz * ux - tx * uz
            var bz = tx * uy - ty * ux
            val bLen = sqrt(bx * bx + by * by + bz * bz).coerceAtLeast(1e-5f)
            bx /= bLen; by /= bLen; bz /= bLen

            // Normal N = B x T
            val nx = by * tz - bz * ty
            val ny = bz * tx - bx * tz
            val nz = bx * ty - by * tx

            val rx = c[3]
            val ry = c[4]
            val matId = c[5]
            val vProg = i.toFloat() / (centers.size - 1).toFloat()

            for (r in 0 until segs) {
                val theta = (2.0 * PI * r.toDouble()) / segs.toDouble()
                val ct = cos(theta).toFloat()
                val st = sin(theta).toFloat()
                val px = c[0] + bx * (ct * rx) + nx * (st * ry)
                val py = c[1] + by * (ct * rx) + ny * (st * ry)
                val pz = c[2] + bz * (ct * rx) + nz * (st * ry)
                rings[i][r] = addVertex(px, py, pz, r.toFloat() / segs, vProg, matId, 1f)
            }
        }

        for (i in 0 until centers.size - 1) {
            val rA = rings[i]
            val rB = rings[i + 1]
            for (r in 0 until segs) {
                val rNext = (r + 1) % segs
                addTriangle(rA[r], rB[r], rB[rNext])
                addTriangle(rA[r], rB[rNext], rA[rNext])
            }
        }

        if (capTip) {
            val last = centers.last()
            val prev = centers[centers.size - 2]
            val dx = (last[0] - prev[0]) * 0.45f
            val dy = (last[1] - prev[1]) * 0.45f
            val dz = (last[2] - prev[2]) * 0.45f
            val tipIdx = addVertex(
                last[0] + dx,
                last[1] + dy,
                last[2] + dz,
                0.5f,
                1f,
                last[5],
                1f
            )
            val ring = rings.last()
            for (r in 0 until segs) {
                val rNext = (r + 1) % segs
                addTriangle(ring[r], tipIdx, ring[rNext])
            }
        }
    }

    /**
     * Adds a double-sided sculpted membrane quad strip (used for dragon wing membranes & gi sash).
     */
    fun addDoubleSidedQuad(
        p0: FloatArray,
        p1: FloatArray,
        p2: FloatArray,
        p3: FloatArray,
        materialId: Float,
        ao: Float = 0.92f
    ) {
        val f0 = addVertex(p0[0], p0[1], p0[2], 0f, 0f, materialId, ao)
        val f1 = addVertex(p1[0], p1[1], p1[2], 1f, 0f, materialId, ao)
        val f2 = addVertex(p2[0], p2[1], p2[2], 1f, 1f, materialId, ao)
        val f3 = addVertex(p3[0], p3[1], p3[2], 0f, 1f, materialId, ao)
        addTriangle(f0, f1, f2)
        addTriangle(f0, f2, f3)

        val b0 = addVertex(p0[0], p0[1], p0[2], 0f, 0f, materialId, ao)
        val b1 = addVertex(p1[0], p1[1], p1[2], 1f, 0f, materialId, ao)
        val b2 = addVertex(p2[0], p2[1], p2[2], 1f, 1f, materialId, ao)
        val b3 = addVertex(p3[0], p3[1], p3[2], 0f, 1f, materialId, ao)
        addTriangle(b0, b2, b1)
        addTriangle(b0, b3, b2)
    }

    /**
     * Computes area-weighted smooth per-vertex normals across all triangles and packs buffers.
     */
    fun build(): CompiledMesh {
        val vCount = mat.size
        for (i in 0 until norm.size) {
            norm[i] = 0f
        }

        var t = 0
        while (t + 2 < indices.size) {
            val i0 = indices[t].toInt() and 0xFFFF
            val i1 = indices[t + 1].toInt() and 0xFFFF
            val i2 = indices[t + 2].toInt() and 0xFFFF

            val x0 = pos[i0 * 3]; val y0 = pos[i0 * 3 + 1]; val z0 = pos[i0 * 3 + 2]
            val x1 = pos[i1 * 3]; val y1 = pos[i1 * 3 + 1]; val z1 = pos[i1 * 3 + 2]
            val x2 = pos[i2 * 3]; val y2 = pos[i2 * 3 + 1]; val z2 = pos[i2 * 3 + 2]

            val ux = x1 - x0; val uy = y1 - y0; val uz = z1 - z0
            val vx = x2 - x0; val vy = y2 - y0; val vz = z2 - z0

            val nx = uy * vz - uz * vy
            val ny = uz * vx - ux * vz
            val nz = ux * vy - uy * vx

            norm[i0 * 3] += nx; norm[i0 * 3 + 1] += ny; norm[i0 * 3 + 2] += nz
            norm[i1 * 3] += nx; norm[i1 * 3 + 1] += ny; norm[i1 * 3 + 2] += nz
            norm[i2 * 3] += nx; norm[i2 * 3 + 1] += ny; norm[i2 * 3 + 2] += nz
            t += 3
        }

        val packed = FloatArray(vCount * CompiledMesh.FLOATS_PER_VERTEX)
        for (i in 0 until vCount) {
            var nx = norm[i * 3]
            var ny = norm[i * 3 + 1]
            var nz = norm[i * 3 + 2]
            val len = sqrt(nx * nx + ny * ny + nz * nz)
            if (len > 1e-6f) {
                nx /= len; ny /= len; nz /= len
            } else {
                nx = 0f; ny = 1f; nz = 0f
            }
            val base = i * CompiledMesh.FLOATS_PER_VERTEX
            packed[base] = pos[i * 3]
            packed[base + 1] = pos[i * 3 + 1]
            packed[base + 2] = pos[i * 3 + 2]
            packed[base + 3] = nx
            packed[base + 4] = ny
            packed[base + 5] = nz
            packed[base + 6] = uv[i * 2]
            packed[base + 7] = uv[i * 2 + 1]
            packed[base + 8] = mat[i]
            packed[base + 9] = aoList[i]
        }

        val shortArr = ShortArray(indices.size) { indices[it] }

        val vb = ByteBuffer.allocateDirect(packed.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(packed)
                position(0)
            }

        val ib = ByteBuffer.allocateDirect(shortArr.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply {
                put(shortArr)
                position(0)
            }

        return CompiledMesh(vb, ib, shortArr.size)
    }
}
