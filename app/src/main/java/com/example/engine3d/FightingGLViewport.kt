package com.example.engine3d

import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.CombatSnapshot
import com.example.model.FighterId
import com.example.model.GraphicsQuality
import com.example.model.ParticleEvent
import com.example.ui.theme.ArenaObsidian

class ArenaGLSurfaceView(
    context: Context,
    val arenaRenderer: ArenaRenderer
) : GLSurfaceView(context) {
    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(arenaRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}

@Composable
fun FightingGLViewport(
    sceneMode: ViewportSceneMode,
    combatSnapshot: CombatSnapshot,
    rosterFighter: FighterId,
    rosterYawDeg: Float,
    graphicsQuality: GraphicsQuality,
    cameraSensitivity: Float,
    particleBatchToken: Long,
    drainParticleBatch: () -> List<ParticleEvent>,
    modifier: Modifier = Modifier
) {
    // Headless JVM / Robolectric guard so unit tests run cleanly without hardware EGL
    val isRobolectric = remember {
        Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
    }

    if (isRobolectric) {
        Box(
            modifier = modifier
                .background(ArenaObsidian)
                .testTag("arena_3d_viewport")
        )
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val renderer = remember { ArenaRenderer() }

    LaunchedEffect(particleBatchToken) {
        val batch = drainParticleBatch()
        if (batch.isNotEmpty()) {
            renderer.enqueueParticles(batch)
        }
    }

    AndroidView(
        factory = { ctx ->
            ArenaGLSurfaceView(ctx, renderer)
        },
        update = { view ->
            view.arenaRenderer.sceneMode = sceneMode
            view.arenaRenderer.combatSnapshot = combatSnapshot
            view.arenaRenderer.rosterFighter = rosterFighter
            view.arenaRenderer.rosterYawDeg = rosterYawDeg
            view.arenaRenderer.graphicsQuality = graphicsQuality
            view.arenaRenderer.cameraSensitivity = cameraSensitivity
        },
        modifier = modifier.testTag("arena_3d_viewport")
    )

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            // GLSurfaceView manages internal thread lifecycle automatically with window attachment;
            // keeping observer hook clean and lightweight.
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_RESUME) {
                // Handled safely by GLSurfaceView surface callbacks
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
