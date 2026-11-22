package com.example

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.example.data.AndroidAppIconLoader
import com.example.data.AtomicJsonLayoutRepository
import com.example.data.PhotoStorageManager
import com.example.data.PreferencesRepository
import com.example.data.metroPreferencesDataStore
import com.example.layout.MetroGridGeometry
import com.example.model.AppTargetKey
import com.example.model.AppThemeMode
import com.example.model.LightweightMode
import com.example.model.MetroAccent
import com.example.model.MetroColorUtils
import com.example.model.MetroFolder
import com.example.model.MetroHub
import com.example.model.ProjectAboutConfig
import com.example.model.QuickNote
import com.example.model.SettingsSection
import com.example.model.StartLayoutState
import com.example.model.StartTile
import com.example.model.TileKind
import com.example.ui.components.NoteEditorDialog
import com.example.ui.components.VisualAlertBanner
import com.example.ui.screens.SettingsAboutPage
import com.example.ui.screens.SettingsAccessibilityPage
import com.example.ui.theme.MetroBlack
import com.example.ui.theme.MetroDefaultBlue
import com.example.ui.theme.MetroHomeTheme
import com.example.ui.theme.MetroLightBackground
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Bloco3PersistenceAndBenchmarkTests {

  @get:Rule val tempFolder = TemporaryFolder()

  @Test
  fun `criterion A - recovery backup restores Start layout when primary file is corrupted`() = runTest {
    val layoutFile = File(tempFolder.root, "start_layout.json")
    val repo = AtomicJsonLayoutRepository(layoutFile)

    val original =
      StartLayoutState(
        schemaVersion = 2,
        tiles =
          listOf(
            StartTile("clock_1", TileKind.CLOCK, 0, 0, 2, 2, fallbackLabel = "Relógio"),
            StartTile("folder_1", TileKind.FOLDER, 2, 0, 2, 2, fallbackLabel = "Estudos", folderId = "f_study"),
          ),
        folders =
          listOf(
            MetroFolder(
              id = "f_study",
              name = "Estudos",
              appKeys = listOf(AppTargetKey("com.app.a", "com.app.a.Main", 0L)),
            )
          ),
        notes = listOf(QuickNote("n1", "Lembrete", "Prova amanhã")),
        hubs = listOf(MetroHub("h1", "Escola")),
      )

    repo.saveLayout(original).getOrThrow()
    val recoveryFile = File(tempFolder.root, "start_layout.json.recovery")
    assertTrue(recoveryFile.exists())

    // Simulate interrupted write / primary corruption
    layoutFile.writeText("{corrupted_json_truncated")

    val recovered = AtomicJsonLayoutRepository(layoutFile).loadLayout()
    assertTrue(recovered.initialized)
    assertEquals(2, recovered.tiles.size)
    assertEquals(1, recovered.folders.size)
    assertEquals("Estudos", recovered.folders.first().name)
    assertEquals(1, recovered.notes.size)
    assertEquals(1, recovered.hubs.size)
  }

  @Test
  fun `criterion B - profile avatar crops cleanly to 256px separate from Start photos and persists profile`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = PhotoStorageManager(context)
    val prefsRepo = PreferencesRepository(context)

    // Create a large test source image (1200x800) in cache
    val sourceFile = File(context.cacheDir, "portrait_source.jpg")
    val sourceBitmap = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888).apply {
      eraseColor(android.graphics.Color.CYAN)
    }
    sourceFile.outputStream().use { sourceBitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
    sourceBitmap.recycle()

    val sourceUri = Uri.fromFile(sourceFile)
    val cropResult =
      storage.importCroppedProfileAvatar(
        uri = sourceUri,
        normCenterX = 0.5f,
        normCenterY = 0.35f,
        zoom = 1.4f,
        targetPx = PhotoStorageManager.PROFILE_AVATAR_PX,
      )
    val (avatarPath, avatarRev) = cropResult.getOrThrow()
    val avatarFile = File(avatarPath)
    assertTrue(avatarFile.exists())
    assertTrue(avatarFile.parentFile?.name == "profile")

    val decodedAvatar = storage.loadProfileAvatar(avatarPath, PhotoStorageManager.PROFILE_AVATAR_PX)
    assertNotNull(decodedAvatar)
    assertTrue(decodedAvatar!!.width <= PhotoStorageManager.PROFILE_AVATAR_PX)
    assertTrue(decodedAvatar.height <= PhotoStorageManager.PROFILE_AVATAR_PX)

    // Save profile in PreferencesRepository & verify Start photos remain separate and OFF
    prefsRepo.setProfileName("Felipe")
    prefsRepo.setProfileAge(27)
    prefsRepo.setProfileAvatar(avatarPath, avatarRev)

    val loadedPrefs = prefsRepo.preferencesFlow.first()
    assertEquals("Felipe", loadedPrefs.profile.name)
    assertEquals(27, loadedPrefs.profile.age)
    assertEquals(avatarPath, loadedPrefs.profile.avatarPath)
    assertFalse(loadedPrefs.profile.showOnStart)
    assertFalse(loadedPrefs.showHomePhotos)
    assertTrue(loadedPrefs.selectedPhotoPaths.isEmpty())

    // Clear age and remove avatar file cleanly
    prefsRepo.setProfileAge(null)
    storage.deleteProfileAvatar(avatarPath)
    prefsRepo.setProfileAvatar(null, System.currentTimeMillis())

    val afterRemoval = prefsRepo.preferencesFlow.first()
    assertEquals("Felipe", afterRemoval.profile.name)
    assertNull(afterRemoval.profile.age)
    assertNull(afterRemoval.profile.avatarPath)
    assertFalse(avatarFile.exists())
  }

  @Test
  fun `criterion C - color contrast picks legible ink on Light, Dark, and bright custom accents`() {
    assertEquals(Color.White, MetroColorUtils.legibleForeground(MetroBlack))
    assertEquals(Color(0xFF111111), MetroColorUtils.legibleForeground(MetroLightBackground))
    assertEquals(Color(0xFF111111), MetroColorUtils.legibleForeground(Color(0xFFFFE600)))
    assertEquals(Color.White, MetroColorUtils.legibleForeground(MetroAccent.VIOLET.color))
    assertTrue(MetroColorUtils.isLightColor(MetroLightBackground))
    assertFalse(MetroColorUtils.isLightColor(MetroBlack))
  }

  @Test
  fun `criterion F - Modo leve AUTO detects 512 MB low-RAM and allows manual overrides`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.metroPreferencesDataStore.edit { it.clear() }

    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    Shadows.shadowOf(am).setIsLowRamDevice(true)

    val lowRamRepo = PreferencesRepository(context)
    val autoLowRam = lowRamRepo.preferencesFlow.first()
    assertTrue(autoLowRam.isLowRamDevice)
    assertEquals(LightweightMode.AUTO, autoLowRam.lightweightMode)
    assertTrue(autoLowRam.lightMode)

    lowRamRepo.setLightweightMode(LightweightMode.DISABLED)
    assertFalse(lowRamRepo.preferencesFlow.first().lightMode)

    lowRamRepo.setLightweightMode(LightweightMode.ENABLED)
    assertTrue(lowRamRepo.preferencesFlow.first().lightMode)

    // Standard RAM device with AUTO stays full-featured
    Shadows.shadowOf(am).setIsLowRamDevice(false)
    val standardRepo = PreferencesRepository(context)
    standardRepo.setLightweightMode(LightweightMode.AUTO)
    val autoStandard = standardRepo.preferencesFlow.first()
    assertFalse(autoStandard.isLowRamDevice)
    assertFalse(autoStandard.lightMode)
  }

  @Test
  fun `benchmark - Bloco 2_0 lightweight 512 MB budget vs Standard Moto G14 workload`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val layoutFile = File(tempFolder.root, "bench_bloco3.json")
    val repo = AtomicJsonLayoutRepository(layoutFile)

    val tiles = buildList {
      add(StartTile("b_clock", TileKind.CLOCK, 0, 0, 2, 2, fallbackLabel = "Relógio"))
      add(StartTile("b_bat", TileKind.BATTERY, 2, 0, 2, 2, fallbackLabel = "Bateria"))
      add(StartTile("b_photo", TileKind.PHOTO, 4, 0, 2, 2, fallbackLabel = "Fotos"))
      for (i in 0 until 57) {
        add(
          StartTile(
            id = "tile_$i",
            kind = if (i % 12 == 0) TileKind.FOLDER else TileKind.APP_SHORTCUT,
            col = (i % 3) * 2,
            row = 2 + (i / 3) * 2,
            colSpan = 2,
            rowSpan = 2,
            target = if (i % 12 != 0) AppTargetKey("com.pkg.$i", "com.pkg.$i.Main", 0L) else null,
            folderId = if (i % 12 == 0) "folder_$i" else null,
            fallbackLabel = "App $i",
          )
        )
      }
    }
    val folders = (0 until 57 step 12).map { i ->
      MetroFolder("folder_$i", "Pasta $i", listOf(AppTargetKey("com.pkg.$i", "com.pkg.$i.Main", 0L)))
    }
    repo.saveLayout(
      StartLayoutState(
        tiles = tiles,
        folders = folders,
        hubs = listOf(MetroHub("h_1", "Escola"), MetroHub("h_2", "Trabalho")),
      )
    ).getOrThrow()

    val startColdNs = System.nanoTime()
    val loaded = repo.loadLayout()
    val coldMs = (System.nanoTime() - startColdNs) / 1_000_000.0

    val warmStartNs = System.nanoTime()
    repeat(20) {
      val st = repo.loadLayout()
      val bands = MetroGridGeometry.partitionIntoBands(st.tiles)
      val unitPx = MetroGridGeometry.computeUnitHeightPx(1080, 12)
      bands.forEach { b -> b.tiles.forEach { MetroGridGeometry.computeTileBounds(1080, 12, unitPx, it, b.startRow) } }
    }
    val warmGridMs = ((System.nanoTime() - warmStartNs) / 1_000_000.0) / 20.0

    // Measure 256x256 bounded profile crop + trim under 512 MB Mode
    val storage = PhotoStorageManager(context)
    val srcFile = File(context.cacheDir, "bench_avatar.jpg")
    val srcBmp = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888)
    srcFile.outputStream().use { srcBmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
    srcBmp.recycle()

    val loader = AndroidAppIconLoader(context)
    val runtime = Runtime.getRuntime()
    val beforeMem = runtime.totalMemory() - runtime.freeMemory()

    val cropStartNs = System.nanoTime()
    val (avatarPath, _) = storage.importCroppedProfileAvatar(Uri.fromFile(srcFile)).getOrThrow()
    val cropMs = (System.nanoTime() - cropStartNs) / 1_000_000.0

    loader.setLightweightMode(true)
    loader.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)
    val afterTrimMem = runtime.totalMemory() - runtime.freeMemory()
    val avatarBytes = File(avatarPath).length()
    storage.deleteProfileAvatar(avatarPath)

    println(
      "[BLOCO_BENCHMARK_BLOCO3] tiles=${loaded.tiles.size} coldLoadMs=${"%.2f".format(coldMs)} " +
        "warmGridMs=${"%.2f".format(warmGridMs)} avatarCropMs=${"%.2f".format(cropMs)} " +
        "avatarFileBytes=$avatarBytes memDeltaBytes=${afterTrimMem - beforeMem}"
    )

    assertEquals(60, loaded.tiles.size)
    assertTrue(avatarBytes in 1..100_000)
    assertTrue(warmGridMs < 250.0)
  }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Bloco3UiAndAccessibilityAcceptanceTests {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `criterion B and D and G - profile privacy on Start, accessible tile actions, and About page`() {
    composeTestRule.waitForIdle()

    // Profile hidden on Start by default
    assertTrue(composeTestRule.onAllNodesWithTag("start_profile_header").fetchSemanticsNodes().isEmpty())

    // Open Configurações -> Meu perfil
    composeTestRule.onNodeWithTag("cmd_ellipsis").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("bar_menu_personalization").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("settings_nav_profile").performClick()
    composeTestRule.waitForIdle()

    // Set Name and Age
    composeTestRule.onNodeWithTag("profile_edit_name_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("profile_name_input").performTextInput("Maria Silva")
    composeTestRule.onNodeWithTag("profile_name_confirm").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("profile_header_name").assertTextContains("Maria Silva")

    composeTestRule.onNodeWithTag("profile_edit_age_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("profile_age_input").performTextInput("32")
    composeTestRule.onNodeWithTag("profile_age_confirm").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("profile_header_age").assertTextContains("32 anos")

    // Enable "Mostrar meu perfil no início"
    composeTestRule.onNodeWithTag("profile_show_on_start_switch").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Navigate using horizontal tab strip to Sobre o projeto (Criterion G)
    composeTestRule.onNodeWithTag("settings_tab_about").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("about_code_icon").assertIsDisplayed()
    composeTestRule.onNodeWithTag("about_app_name").assertTextContains("Bloco")
    composeTestRule.onNodeWithTag("about_version").assertTextContains("Versão 2.0")
    composeTestRule.onNodeWithTag("about_creator").assertTextContains("Criado por Felipe Oliveira")
    composeTestRule.onNodeWithTag("about_statement").assertTextContains(ProjectAboutConfig.FREE_USE_STATEMENT_PT)
    assertNull(ProjectAboutConfig.repositoryUrl)
    assertTrue(composeTestRule.onAllNodesWithTag("about_repo_button").fetchSemanticsNodes().isEmpty())
    assertTrue(composeTestRule.onAllNodesWithTag("about_license_card").fetchSemanticsNodes().isEmpty())

    // Navigate to Acessibilidade and verify 3 groups + Android system shortcuts (Criterion D)
    composeTestRule.onNodeWithTag("settings_tab_accessibility").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("a11y_group_vision").assertIsDisplayed()
    composeTestRule.onNodeWithTag("a11y_group_hearing").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("a11y_group_motion").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("a11y_open_screen_reader_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    val a11yIntent = Shadows.shadowOf(composeTestRule.activity).nextStartedActivity
    assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, a11yIntent?.action)

    composeTestRule.onNodeWithTag("a11y_open_captions_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    val captionIntent = Shadows.shadowOf(composeTestRule.activity).nextStartedActivity
    assertEquals(Settings.ACTION_CAPTIONING_SETTINGS, captionIntent?.action)

    // Back to Landing -> Back to Start
    composeTestRule.onNodeWithTag("settings_back_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("settings_back_button").performClick()
    composeTestRule.waitForIdle()

    // Start shows profile header with name, and NEVER shows age ("32" absent on Start)
    composeTestRule.onNodeWithTag("start_profile_header").assertIsDisplayed()
    composeTestRule.onNodeWithText("Maria Silva").assertIsDisplayed()
    assertTrue(composeTestRule.onAllNodesWithText("32 anos", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())

    // Criterion D: Start tile exposes custom accessibility actions "Mover", "Redimensionar", "Desafixar"
    val clockNode = composeTestRule.onNodeWithTag("tile_slot_clock", useUnmergedTree = true)
    val actions = clockNode.fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions).orEmpty()
    assertTrue(actions.any { it.label == "Mover" })
    assertTrue(actions.any { it.label == "Redimensionar" })
    assertTrue(actions.any { it.label == "Desafixar" })

    // Trigger "Mover" custom accessibility action directly (no drag / no long press!)
    val moveAction = actions.first { it.label == "Mover" }
    composeTestRule.runOnUiThread { moveAction.action() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("move_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("move_done_button").performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun `criterion E - creating quick note shows readable VisualAlertBanner and dismisses cleanly`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("cmd_ellipsis").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("bar_menu_quick_note").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("note_title_input").performTextInput("Lista")
    composeTestRule.onNodeWithTag("note_body_input").performTextInput("Pão e café")
    composeTestRule.onNodeWithTag("note_save_button").performClick()

    composeTestRule.waitUntil(3_000) {
      composeTestRule.onAllNodesWithTag("visual_alert_banner").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("visual_alert_banner").assertIsDisplayed()
    composeTestRule.onNodeWithText("Nota salva").assertIsDisplayed()
    composeTestRule.onNodeWithTag("visual_alert_dismiss").performClick()
    composeTestRule.waitForIdle()
    assertTrue(composeTestRule.onAllNodesWithTag("visual_alert_banner").fetchSemanticsNodes().isEmpty())
  }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Bloco3IsolatedUiComponentsTests {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `criterion C - switching dark to light theme preserves open NoteEditorDialog unsaved text`() {
    var darkTheme by mutableStateOf(true)
    var resolvedBg by mutableStateOf(Color.Black)

    composeTestRule.setContent {
      MetroHomeTheme(isDarkTheme = darkTheme, accentColor = MetroDefaultBlue) {
        resolvedBg = MaterialTheme.colorScheme.background
        NoteEditorDialog(
          session = NoteEditorSession(noteId = null, title = "", body = ""),
          accentColor = MetroDefaultBlue,
          onSave = { _, _, _, _, _ -> },
          onRemoveFromStart = {},
          onDeleteNote = {},
          onDismiss = {},
        )
      }
    }

    composeTestRule.onNodeWithTag("note_title_input").performTextInput("Rascunho importante")
    composeTestRule.onNodeWithTag("note_body_input").performTextInput("Texto que não pode sumir")
    composeTestRule.waitForIdle()
    assertEquals(MetroBlack, resolvedBg)

    composeTestRule.runOnUiThread { darkTheme = false }
    composeTestRule.waitForIdle()

    assertEquals(MetroLightBackground, resolvedBg)
    composeTestRule.onNodeWithText("Rascunho importante").assertIsDisplayed()
    composeTestRule.onNodeWithText("Texto que não pode sumir").assertIsDisplayed()
  }

  @Test
  fun `criterion D and E - 1_5x fontScale and persistent error alert stays pinned until Fechar is tapped`() {
    var alert by mutableStateOf<LauncherAlert?>(
      LauncherAlert(message = "Erro ao importar imagem selecionada.", isError = true)
    )

    composeTestRule.setContent {
      CompositionLocalProvider(LocalDensity provides Density(density = 2.5f, fontScale = 1.5f)) {
        MetroHomeTheme(isDarkTheme = false) {
          Column {
            VisualAlertBanner(
              alert = alert,
              accentColor = MetroDefaultBlue,
              onAction = {},
              onDismiss = { alert = null },
            )
            SettingsAccessibilityPage(
              uiState = LauncherUiState(systemAccessibilityEnabled = true),
              fgOnBg = MaterialTheme.colorScheme.onBackground,
              mutedOnBg = MaterialTheme.colorScheme.onSurfaceVariant,
              accentColor = MetroDefaultBlue,
              onToggleVibration = {},
              onToggleReduceMotion = {},
              onOpenSystemAccessibilitySettings = {},
              onOpenSystemCaptionSettings = {},
            )
          }
        }
      }
    }

    composeTestRule.onNodeWithTag("visual_alert_banner").assertIsDisplayed()
    composeTestRule.mainClock.advanceTimeBy(10_000L)
    composeTestRule.waitForIdle()
    // Error alerts MUST stay pinned until dismissed
    composeTestRule.onNodeWithTag("visual_alert_banner").assertIsDisplayed()

    composeTestRule.onNodeWithTag("a11y_group_vision").assertIsDisplayed()
    composeTestRule.onNodeWithTag("visual_alert_dismiss").performClick()
    composeTestRule.waitForIdle()
    assertTrue(composeTestRule.onAllNodesWithTag("visual_alert_banner").fetchSemanticsNodes().isEmpty())
  }
}
