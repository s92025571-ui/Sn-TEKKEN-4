package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.GameSettings
import com.example.model.GraphicsQuality

class GamePreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadSettings(): GameSettings {
        val qualityName = prefs.getString(KEY_GRAPHICS, GraphicsQuality.HIGH.name)
            ?: GraphicsQuality.HIGH.name
        val quality = GraphicsQuality.entries.firstOrNull { it.name == qualityName }
            ?: GraphicsQuality.HIGH

        return GameSettings(
            sfxEnabled = prefs.getBoolean(KEY_SFX, true),
            musicEnabled = prefs.getBoolean(KEY_MUSIC, true),
            graphicsQuality = quality,
            cameraSensitivity = prefs.getFloat(KEY_CAMERA_SENS, 1.0f).coerceIn(0.6f, 1.4f),
            controlScale = prefs.getFloat(KEY_CONTROL_SCALE, 1.0f).coerceIn(0.8f, 1.25f),
            devUnlockAllStages = prefs.getBoolean(KEY_DEV_UNLOCK, false)
        )
    }

    fun saveSettings(settings: GameSettings) {
        prefs.edit()
            .putBoolean(KEY_SFX, settings.sfxEnabled)
            .putBoolean(KEY_MUSIC, settings.musicEnabled)
            .putString(KEY_GRAPHICS, settings.graphicsQuality.name)
            .putFloat(KEY_CAMERA_SENS, settings.cameraSensitivity)
            .putFloat(KEY_CONTROL_SCALE, settings.controlScale)
            .putBoolean(KEY_DEV_UNLOCK, settings.devUnlockAllStages)
            .apply()
    }

    fun getUnlockedStage(): Int = prefs.getInt(KEY_UNLOCKED_STAGE, 1).coerceIn(1, 3)

    fun unlockStage(stageNumber: Int) {
        val current = getUnlockedStage()
        val next = maxOf(current, stageNumber.coerceIn(1, 3))
        prefs.edit().putInt(KEY_UNLOCKED_STAGE, next).apply()
    }

    fun isCampaignCompleted(): Boolean = prefs.getBoolean(KEY_CAMPAIGN_COMPLETED, false)

    fun setCampaignCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_CAMPAIGN_COMPLETED, completed).apply()
    }

    companion object {
        private const val PREFS_NAME = "sn_tekken_animal_warriors_prefs"
        private const val KEY_SFX = "sfx_enabled"
        private const val KEY_MUSIC = "music_enabled"
        private const val KEY_GRAPHICS = "graphics_quality"
        private const val KEY_CAMERA_SENS = "camera_sensitivity"
        private const val KEY_CONTROL_SCALE = "control_scale"
        private const val KEY_DEV_UNLOCK = "dev_unlock_all"
        private const val KEY_UNLOCKED_STAGE = "unlocked_stage"
        private const val KEY_CAMPAIGN_COMPLETED = "campaign_completed"
    }
}
