package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `read localized app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Bloco", appName)
  }

  @Test
  fun `start screen displays Bloco command bar and ellipsis reveals labels and menu`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("cmd_search").assertIsDisplayed()
    composeTestRule.onNodeWithTag("cmd_apps").assertIsDisplayed()
    composeTestRule.onNodeWithTag("cmd_edit").assertIsDisplayed()
    composeTestRule.onNodeWithTag("cmd_ellipsis").assertIsDisplayed()

    composeTestRule.onNodeWithTag("cmd_ellipsis").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Buscar").assertIsDisplayed()
    composeTestRule.onNodeWithText("Aplicativos").assertIsDisplayed()
    composeTestRule.onNodeWithText("Editar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("bar_menu_create_folder").assertIsDisplayed()
    composeTestRule.onNodeWithTag("bar_menu_quick_note").assertIsDisplayed()
    composeTestRule.onNodeWithTag("bar_menu_personalization").assertIsDisplayed()
  }

  @Test
  fun `apps screen shows Aplicativos title and search clear flow`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("cmd_apps").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("apps_title").assertIsDisplayed()
    composeTestRule.onNodeWithTag("apps_search_field").assertIsDisplayed()

    composeTestRule.onNodeWithTag("apps_search_field").performTextInput("zzzz_nao_existe")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Nenhum aplicativo encontrado").assertIsDisplayed()
    composeTestRule.onNodeWithTag("empty_clear_search_button").performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun `edit mode enters in place and commits with Concluir`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("cmd_edit").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("cmd_resize").assertIsDisplayed()
    composeTestRule.onNodeWithTag("cmd_unpin").assertIsDisplayed()
    composeTestRule.onNodeWithTag("cmd_done").assertIsDisplayed()

    composeTestRule.onNodeWithTag("cmd_done").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("cmd_apps").assertIsDisplayed()
  }

  @Test
  fun `create wide quick note, apply per-tile color, customize command bar and inspect personalization`() {
    composeTestRule.waitForIdle()

    // Open Nova nota via bottom bar ellipsis menu
    composeTestRule.onNodeWithTag("cmd_ellipsis").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("bar_menu_quick_note").performClick()
    composeTestRule.waitForIdle()

    // Author wide quick note "Prova de sexta"
    composeTestRule.onNodeWithTag("note_title_input").performTextInput("Prova de sexta")
    composeTestRule.onNodeWithTag("note_body_input").performTextInput("Estudar álgebra e física")
    composeTestRule.onNodeWithTag("note_size_wide").performClick()
    composeTestRule.onNodeWithTag("note_save_button").performClick()
    composeTestRule.waitUntil(timeoutMillis = 3_000) {
      composeTestRule.onAllNodesWithText("Prova de sexta", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    // Select any tile in edit mode and verify per-tile Violet preset works cleanly
    composeTestRule.onNodeWithTag("cmd_edit").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onAllNodesWithTag("tile_slot_clock", useUnmergedTree = true).onFirst().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("quick_color_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("tile_color_preset_violet").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("cmd_done").performClick()
    composeTestRule.waitUntil(timeoutMillis = 3_000) {
      composeTestRule.onAllNodesWithTag("cmd_apps").fetchSemanticsNodes().isNotEmpty()
    }

    // Assign "Nota rápida" to left bottom command bar slot
    composeTestRule.onNodeWithTag("cmd_ellipsis").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("bar_menu_left_slot_quick_note").performClick()
    composeTestRule.waitUntil(timeoutMillis = 3_000) {
      composeTestRule.onAllNodesWithTag("cmd_quick_note").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("cmd_quick_note").assertIsDisplayed()

    // Verify tapping left slot opens blank note editor in one tap
    composeTestRule.onNodeWithTag("cmd_quick_note").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("note_editor_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithText("Cancelar").performClick()
    composeTestRule.waitForIdle()

    // Open Personalização / Configurações via ellipsis menu, check Bloco logo, and open Aparência live preview
    composeTestRule.onNodeWithTag("cmd_ellipsis").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("bar_menu_personalization").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("bloco_logo_settings").assertIsDisplayed()
    composeTestRule.onNodeWithTag("settings_nav_appearance").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("accent_preview_row").assertIsDisplayed()
  }
}
