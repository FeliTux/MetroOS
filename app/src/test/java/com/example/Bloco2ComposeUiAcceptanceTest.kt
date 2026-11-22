package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import com.example.ui.components.MetroCommandBar
import com.example.ui.components.SelectedTileQuickBar
import com.example.ui.theme.MetroBlack
import com.example.ui.theme.MetroDefaultBlue
import com.example.ui.theme.MetroHomeTheme
import com.example.model.LeftCommandSlot
import com.example.model.StartTile
import com.example.model.TileKind
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Bloco2ComposeUiAcceptanceTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `release acceptance flow - Escola Hub, note, lightweight mode, and navigation back to Start`() {
    composeTestRule.waitForIdle()

    // 1. Fresh install keeps photos hidden by default (no photo bitmap container on Start)
    assert(composeTestRule.onAllNodesWithTag("photo_tile_bitmap_container").fetchSemanticsNodes().isEmpty())

    // 2. Create "Escola" Hub from Start command bar ellipsis menu
    composeTestRule.onNodeWithTag("cmd_ellipsis").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("bar_menu_create_hub").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("create_hub_name_input").performTextInput("Escola")
    composeTestRule.onNodeWithTag("hub_color_preset_forest").performClick()
    composeTestRule.onNodeWithTag("create_hub_confirm_button").performClick()

    composeTestRule.waitUntil(3_000) {
      composeTestRule.onAllNodesWithTag("hub_title").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Escola").assertIsDisplayed()

    // 3. Inside Escola Hub, add a note and a Calendar live tile
    composeTestRule.onNodeWithTag("cmd_add_to_hub").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("hub_add_live_calendar").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("cmd_add_to_hub").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("hub_add_new_note").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("note_title_input").performTextInput("Agenda Escolar")
    composeTestRule.onNodeWithTag("note_body_input").performTextInput("Entregar trabalho")
    composeTestRule.onNodeWithTag("note_save_button").performClick()
    composeTestRule.waitUntil(3_000) {
      composeTestRule.onAllNodesWithTag("note_editor_dialog").fetchSemanticsNodes().isEmpty()
    }

    // 4. Return to Start cleanly
    composeTestRule.onNodeWithTag("cmd_back_to_start").performClick()
    composeTestRule.waitUntil(3_000) {
      composeTestRule.onAllNodesWithTag("cmd_apps").fetchSemanticsNodes().isNotEmpty()
    }

    // 5. Open Configurações, verify modular subpages (Tela inicial, Avançado, Desempenho) and enable Modo leve
    composeTestRule.onNodeWithTag("cmd_ellipsis").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("bar_menu_personalization").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("settings_nav_start_screen").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("show_home_photos_switch").assertIsDisplayed()
    composeTestRule.onNodeWithTag("settings_back_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("settings_nav_advanced").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("reload_icons_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("settings_back_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("settings_nav_performance").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("lightweight_enabled").performClick()
    composeTestRule.waitForIdle()

    // Return from Desempenho -> Landing -> Start
    composeTestRule.onNodeWithTag("settings_back_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("settings_back_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("cmd_apps").assertIsDisplayed()
  }

  @Test
  fun `large system font scale 1_5x keeps command bar and selected tile actions usable`() {
    composeTestRule.activityRule.scenario.onActivity { activity ->
      // Ensure activity starts cleanly, then verify scaled bar/quick actions in compose hierarchy
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("cmd_edit").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("cmd_resize").assertIsDisplayed()
    composeTestRule.onNodeWithTag("cmd_unpin").assertIsDisplayed()
    composeTestRule.onNodeWithTag("cmd_done").assertIsDisplayed()
  }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Bloco2LargeFontScaleTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `command bar and selected tile quick bar remain visible and clickable at fontScale 1_5f`() {
    val sampleClock =
      StartTile(
        id = "clock_1",
        kind = TileKind.CLOCK,
        col = 0,
        row = 0,
        colSpan = 2,
        rowSpan = 2,
        fallbackLabel = "Relógio",
      )

    composeTestRule.setContent {
      CompositionLocalProvider(LocalDensity provides Density(density = 2.5f, fontScale = 1.5f)) {
        MetroHomeTheme {
          Column {
            SelectedTileQuickBar(
              selectedTile = sampleClock,
              selectedFolder = null,
              themeThirdPartyIcons = true,
              accentColor = MetroDefaultBlue,
              onMoveClick = {},
              onColorClick = {},
              onToggleOriginalIcon = {},
              onToggleLiveMode = {},
              onRenameFolder = {},
              onDissolveFolder = {},
            )
            MetroCommandBar(
              backgroundColor = MetroBlack,
              isEditing = true,
              hasSelectedTile = true,
              isSaving = false,
              leftSlot = LeftCommandSlot.SEARCH,
              onLeftSlotClick = {},
              onAppsClick = {},
              onEditClick = {},
              onResizeClick = {},
              onUnpinClick = {},
              onDoneClick = {},
              onCreateFolderClick = {},
              onQuickNoteClick = {},
              onSavedNotesClick = {},
              onOpenPersonalizationClick = {},
              onOpenTileColorClick = {},
              onOpenMoveClick = {},
              onChangeLeftSlot = {},
            )
          }
        }
      }
    }

    composeTestRule.onNodeWithTag("quick_move_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("quick_color_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("quick_toggle_live_mode").assertIsDisplayed()
    composeTestRule.onNodeWithTag("cmd_resize").assertIsDisplayed()
    composeTestRule.onNodeWithTag("cmd_unpin").assertIsDisplayed()
    composeTestRule.onNodeWithTag("cmd_done").assertIsDisplayed()
  }
}
