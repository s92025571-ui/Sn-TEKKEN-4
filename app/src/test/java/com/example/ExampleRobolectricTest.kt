package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.GamePreferencesRepository
import com.example.model.GraphicsQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w840dp-h420dp-land")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appNameAndLocalProgressionPersistence_workOffline() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("SN TEKKEN", context.getString(R.string.app_name))

        val repo = GamePreferencesRepository(context)
        assertEquals(1, repo.getUnlockedStage())
        repo.unlockStage(2)
        assertEquals(2, repo.getUnlockedStage())

        val updated = repo.loadSettings().copy(graphicsQuality = GraphicsQuality.MEDIUM, sfxEnabled = false)
        repo.saveSettings(updated)
        val reloaded = repo.loadSettings()
        assertEquals(GraphicsQuality.MEDIUM, reloaded.graphicsQuality)
        assertEquals(false, reloaded.sfxEnabled)
    }

    @Test
    fun criticalUserJourney_mainMenuRosterSettingsAndCombatControls() {
        composeTestRule.waitForIdle()

        // 1. Main Menu renders all required controls
        composeTestRule.onNodeWithTag("main_menu_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("play_campaign_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("fighter_roster_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_button").assertIsDisplayed()

        // 2. Open Fighter Roster -> verify strictly 2 playable fighters (Shadow Tiger & Iron Buffalo, NO Dragon Boss card)
        composeTestRule.onNodeWithTag("fighter_roster_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("fighter_roster_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("roster_card_shadow_tiger").assertIsDisplayed()
        composeTestRule.onNodeWithTag("roster_card_iron_buffalo").assertIsDisplayed()
        assertTrue(composeTestRule.onAllNodesWithTag("roster_card_dragon_boss").fetchSemanticsNodes().isEmpty())

        // Switch preview to Iron Buffalo and rotate 3D preview
        composeTestRule.onNodeWithTag("roster_card_iron_buffalo").performClick()
        composeTestRule.onNodeWithTag("btn_rotate_right").performClick()
        composeTestRule.onNodeWithTag("roster_back_button").performClick()
        composeTestRule.waitForIdle()

        // 3. Open Settings -> toggle graphics & return to Main Menu
        composeTestRule.onNodeWithTag("settings_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("graphics_option_medium").performClick()
        composeTestRule.onNodeWithTag("return_menu_button").performClick()
        composeTestRule.waitForIdle()

        // 4. Open Campaign Hub -> verify sequential Stage 1 -> Enter Arena -> verify Combat HUD & Mobile Controls
        composeTestRule.onNodeWithTag("play_campaign_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("campaign_select_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("campaign_stage_card_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_enter_arena").performClick()
        composeTestRule.waitForIdle()

        // 5. Verify Combat HUD & Redesigned Mobile Controls (Joystick + 4 Action Buttons)
        composeTestRule.onNodeWithTag("combat_screen_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_health_hud").assertIsDisplayed()
        composeTestRule.onNodeWithTag("opponent_health_hud").assertIsDisplayed()
        composeTestRule.onNodeWithTag("joystick_control").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_punch").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_kick").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_block").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_special").assertIsDisplayed()

        // Exercise combat action buttons
        composeTestRule.onNodeWithTag("btn_punch").performClick()
        composeTestRule.onNodeWithTag("btn_kick").performClick()
        composeTestRule.onNodeWithTag("btn_pause_combat").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pause_modal_card").assertIsDisplayed()
    }
}
