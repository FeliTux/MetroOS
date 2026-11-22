package com.example

import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.example.data.AndroidAppIconLoader
import com.example.data.AppCatalogManager
import com.example.data.AppIconLoader
import com.example.data.AtomicJsonLayoutRepository
import com.example.data.LoadedAppIcon
import com.example.data.PhotoStorageManager
import com.example.data.PreferencesRepository
import com.example.data.metroPreferencesDataStore
import com.example.layout.MetroGridGeometry
import com.example.model.AppTargetKey
import com.example.model.InstalledApp
import com.example.model.MetroAccent
import com.example.model.MetroFolder
import com.example.model.MetroHub
import com.example.model.QuickNote
import com.example.model.StartLayoutState
import com.example.model.StartTile
import com.example.model.TileKind
import com.example.ui.components.AppIconImage
import com.example.ui.theme.MetroHomeTheme
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Bloco2RegressionAndBenchmarkTest {

  @get:Rule val composeTestRule = createComposeRule()
  @get:Rule val tempFolder = TemporaryFolder()

  @Test
  fun `regression - delayed first icon request cannot overwrite new recycled target icon`() {
    val appA = AppTargetKey("com.alpha.first", "com.alpha.first.Main", 0L)
    val appB = AppTargetKey("com.beta.second", "com.beta.second.Main", 0L)

    val bitmapA = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply {
      eraseColor(android.graphics.Color.RED)
    }.asImageBitmap()
    val bitmapB = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply {
      eraseColor(android.graphics.Color.GREEN)
    }.asImageBitmap()

    val firstRequestGate = CompletableDeferred<Unit>()
    var firstRequested = false
    val loadedBitmaps = mutableListOf<Pair<AppTargetKey, Int>>()

    val fakeLoader =
      object : AppIconLoader {
        override val cacheGeneration: StateFlow<Long> = MutableStateFlow(1L)

        override suspend fun loadIcon(targetKey: AppTargetKey, maxPx: Int): LoadedAppIcon {
          return if (targetKey == appA) {
            firstRequested = true
            firstRequestGate.await()
            loadedBitmaps.add(targetKey to bitmapA.width)
            LoadedAppIcon(targetKey, 1L, bitmapA)
          } else {
            loadedBitmaps.add(targetKey to bitmapB.width)
            LoadedAppIcon(targetKey, 1L, bitmapB)
          }
        }

        override fun invalidatePackage(packageName: String) = Unit
        override fun rebuildCache() = Unit
        override fun setLightweightMode(enabled: Boolean) = Unit
        override fun onTrimMemory(level: Int) = Unit
      }

    var currentTarget by mutableStateOf(appA)
    var currentLabel by mutableStateOf("Alpha")

    composeTestRule.setContent {
      MetroHomeTheme {
        AppIconImage(
          targetKey = currentTarget,
          fallbackLabel = currentLabel,
          iconCacheGeneration = 1L,
          loadAppIcon = { key -> fakeLoader.loadIcon(key) },
          iconSize = 44.dp,
          testTagPrefix = "race_icon",
        )
      }
    }

    composeTestRule.waitUntil(2_000) { firstRequested }
    composeTestRule.onNodeWithTag("race_icon_${appA.stableKey}").assertIsDisplayed()

    // Simulate rapid scroll / search query change replacing the recycled row target to appB
    composeTestRule.runOnUiThread {
      currentTarget = appB
      currentLabel = "Beta"
    }

    composeTestRule.waitUntil(2_000) {
      composeTestRule.onAllNodesWithTag("race_icon_${appB.stableKey}").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("race_icon_${appB.stableKey}").assertIsDisplayed()

    // Now release the old delayed appA request and confirm appB remains active and appA node is absent
    firstRequestGate.complete(Unit)
    composeTestRule.waitForIdle()

    assertTrue(composeTestRule.onAllNodesWithTag("race_icon_${appA.stableKey}").fetchSemanticsNodes().isEmpty())
    composeTestRule.onNodeWithTag("race_icon_${appB.stableKey}").assertIsDisplayed()
  }

  @Test
  fun `regression - similarly named apps pinned to Start and Folder retain distinct component identities across restart`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val layoutFile = File(tempFolder.root, "dup_names_layout.json")
    val repo = AtomicJsonLayoutRepository(layoutFile)
    val catalog = AppCatalogManager(context)

    val notasAlpha = AppTargetKey("com.alpha.notes", "com.alpha.notes.EditorActivity", 0L)
    val notasBeta = AppTargetKey("com.beta.notes", "com.beta.notes.MainActivity", 0L)

    val installed =
      listOf(
        InstalledApp(notasAlpha, "Notas", "notas", "N", isSystemApp = false),
        InstalledApp(notasBeta, "Notas", "notas", "N", isSystemApp = false),
      )

    val folder = MetroFolder(id = "folder_work", name = "Trabalho", appKeys = listOf(notasBeta))
    val tileStart =
      StartTile(
        id = "tile_notas_alpha",
        kind = TileKind.APP_SHORTCUT,
        col = 0,
        row = 0,
        colSpan = 2,
        rowSpan = 2,
        target = notasAlpha,
        fallbackLabel = "Notas",
      )
    val tileFolder =
      StartTile(
        id = "tile_folder_work",
        kind = TileKind.FOLDER,
        col = 2,
        row = 0,
        colSpan = 2,
        rowSpan = 2,
        fallbackLabel = "Trabalho",
        folderId = folder.id,
      )

    repo.saveLayout(
      StartLayoutState(tiles = listOf(tileStart, tileFolder), folders = listOf(folder))
    ).getOrThrow()

    val reloaded = AtomicJsonLayoutRepository(layoutFile).loadLayout()
    val reconciled = catalog.reconcileTiles(reloaded.tiles, installed)

    val resolvedStartTile = reconciled.tiles.first { it.id == "tile_notas_alpha" }
    val resolvedFolder = reloaded.folders.first { it.id == "folder_work" }

    assertEquals(notasAlpha, resolvedStartTile.target)
    assertEquals(listOf(notasBeta), resolvedFolder.appKeys)
    assertFalse(resolvedStartTile.needsRepairChoice)
    assertTrue(reconciled.repairProposals.isEmpty())
  }

  @Test
  fun `upgrade reconciliation auto-repairs unambiguous renames and proposes Escolher aplicativo for ambiguous ones`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val catalog = AppCatalogManager(context)

    val calcKey = AppTargetKey("com.calc", "com.calc.CalculatorActivity", 0L)
    val cam1 = AppTargetKey("com.cam.stock", "com.cam.stock.CameraActivity", 0L)
    val cam2 = AppTargetKey("com.cam.pro", "com.cam.pro.ProCameraActivity", 0L)

    val installed =
      listOf(
        InstalledApp(calcKey, "Calculadora", "calculadora", "C", isSystemApp = true),
        InstalledApp(cam1, "Câmera", "camera", "C", isSystemApp = true),
        InstalledApp(cam2, "Câmera", "camera", "C", isSystemApp = false),
      )

    val legacyTiles =
      listOf(
        StartTile(
          id = "legacy_unambiguous",
          kind = TileKind.APP_SHORTCUT,
          col = 0,
          row = 0,
          colSpan = 2,
          rowSpan = 2,
          target = null,
          fallbackLabel = "Calculadora",
        ),
        StartTile(
          id = "legacy_ambiguous",
          kind = TileKind.APP_SHORTCUT,
          col = 2,
          row = 0,
          colSpan = 2,
          rowSpan = 2,
          target = null,
          fallbackLabel = "Câmera",
        ),
      )

    val result = catalog.reconcileTiles(legacyTiles, installed)
    assertTrue(result.repairedAutomatically)

    val repairedCalc = result.tiles.first { it.id == "legacy_unambiguous" }
    assertEquals(calcKey, repairedCalc.target)
    assertFalse(repairedCalc.needsRepairChoice)

    val ambiguousCam = result.tiles.first { it.id == "legacy_ambiguous" }
    assertTrue(ambiguousCam.needsRepairChoice)
    val proposal = result.repairProposals["legacy_ambiguous"]
    assertNotNull(proposal)
    assertEquals(listOf(cam1, cam2), proposal!!.candidates.map { it.key })
  }

  @Test
  fun `fresh install disables home photos while upgrade with saved photo preserves visibility and forget deletes files`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefsRepo = PreferencesRepository(context)
    val storage = PhotoStorageManager(context)

    val fresh = prefsRepo.preferencesFlow.first()
    assertFalse(fresh.showHomePhotos)
    assertFalse(fresh.showPrivatePreviews)
    assertFalse(fresh.themeThirdPartyIcons)

    // Create a local photo file and simulate upgrade / selection
    val dummyPhotoFile = File(context.filesDir, "bloco_test_photo.jpg")
    val bmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
    dummyPhotoFile.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    assertTrue(dummyPhotoFile.exists())

    prefsRepo.setSelectedPhotos(listOf(dummyPhotoFile.absolutePath), revision = 10L, enableDisplay = true)
    assertTrue(prefsRepo.preferencesFlow.first().showHomePhotos)

    // Hide photos without deleting saved selection
    prefsRepo.setShowHomePhotos(false)
    val hiddenPrefs = prefsRepo.preferencesFlow.first()
    assertFalse(hiddenPrefs.showHomePhotos)
    assertEquals(listOf(dummyPhotoFile.absolutePath), hiddenPrefs.selectedPhotoPaths)
    assertTrue(dummyPhotoFile.exists())

    // Forget selected photos clears selection AND deletes file
    prefsRepo.forgetSelectedPhotos()
    storage.deletePhotoFiles(listOf(dummyPhotoFile.absolutePath))
    val forgottenPrefs = prefsRepo.preferencesFlow.first()
    assertTrue(forgottenPrefs.selectedPhotoPaths.isEmpty())
    assertFalse(dummyPhotoFile.exists())
  }

  @Test
  fun `upgrade from legacy v1 photo_file_path preserves showHomePhotos true when show_home_photos key is absent`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val legacyFile = File(context.filesDir, "legacy_v1_photo.jpg")
    legacyFile.writeBytes(byteArrayOf(1, 2, 3))

    context.metroPreferencesDataStore.edit { prefs ->
      prefs.clear()
      prefs[stringPreferencesKey("photo_file_path")] = legacyFile.absolutePath
    }

    val upgradedPrefs = PreferencesRepository(context).preferencesFlow.first()
    assertTrue(upgradedPrefs.showHomePhotos)
    assertEquals(listOf(legacyFile.absolutePath), upgradedPrefs.selectedPhotoPaths)
  }

  @Test
  fun `icon cache rebuild and memory trim safely increment generation and release entries`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val loader = AndroidAppIconLoader(context)
    val initialGen = loader.cacheGeneration.value
    loader.setLightweightMode(true)
    loader.invalidatePackage("com.demo.pkg")
    assertEquals(initialGen + 1L, loader.cacheGeneration.value)
    loader.rebuildCache()
    assertEquals(initialGen + 2L, loader.cacheGeneration.value)
    loader.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)
  }

  @Test
  fun `benchmark - 60 mixed tiles, folders, 1 photo, and 2 Hubs in Standard vs Modo leve`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val layoutFile = File(tempFolder.root, "benchmark_60_layout.json")
    val repo = AtomicJsonLayoutRepository(layoutFile)

    val stressTiles = buildList {
      add(StartTile("b_clock", TileKind.CLOCK, 0, 0, 2, 2, fallbackLabel = "Relógio"))
      add(StartTile("b_photo", TileKind.PHOTO, 2, 0, 4, 2, fallbackLabel = "Fotos"))
      add(StartTile("b_cal", TileKind.CALENDAR, 0, 2, 4, 2, fallbackLabel = "Calendário"))
      add(StartTile("b_bat", TileKind.BATTERY, 4, 2, 2, 2, fallbackLabel = "Bateria"))
      for (i in 0 until 56) {
        val row = 4 + (i / 3) * 2
        val col = (i % 3) * 2
        val kind = if (i % 10 == 0) TileKind.FOLDER else TileKind.APP_SHORTCUT
        add(
          StartTile(
            id = "b_tile_$i",
            kind = kind,
            col = col,
            row = row,
            colSpan = 2,
            rowSpan = 2,
            target = if (kind == TileKind.APP_SHORTCUT) AppTargetKey("com.app.$i", "com.app.$i.Main", 0L) else null,
            folderId = if (kind == TileKind.FOLDER) "f_$i" else null,
            fallbackLabel = "Bloco $i",
          )
        )
      }
    }
    assertEquals(60, stressTiles.size)

    val folders = (0 until 56 step 10).map { i ->
      MetroFolder(
        id = "f_$i",
        name = "Pasta $i",
        appKeys = listOf(AppTargetKey("com.app.$i", "com.app.$i.Main", 0L)),
      )
    }

    val hubEscola =
      MetroHub(
        id = "hub_escola",
        name = "Escola",
        accentHex = MetroAccent.FOREST.colorValue,
        tiles =
          listOf(
            StartTile("escola_cal", TileKind.CALENDAR, 0, 0, 4, 2, fallbackLabel = "Calendário"),
            StartTile("escola_note", TileKind.NOTE, 4, 0, 2, 2, fallbackLabel = "Nota", noteId = "n_escola"),
          ),
      )
    val hubJogos =
      MetroHub(
        id = "hub_jogos",
        name = "Jogos",
        accentHex = MetroAccent.VIOLET.colorValue,
        tiles =
          listOf(
            StartTile("jogos_1", TileKind.APP_SHORTCUT, 0, 0, 2, 2, target = AppTargetKey("com.game.a", "com.game.a.Main", 0L), fallbackLabel = "Jogo A"),
            StartTile("jogos_2", TileKind.APP_SHORTCUT, 2, 0, 2, 2, target = AppTargetKey("com.game.b", "com.game.b.Main", 0L), fallbackLabel = "Jogo B"),
          ),
      )

    repo.saveLayout(
      StartLayoutState(
        tiles = stressTiles,
        folders = folders,
        notes = listOf(QuickNote("n_escola", "Prova", "Estudar história")),
        hubs = listOf(hubEscola, hubJogos),
      )
    ).getOrThrow()

    // Measure Cold JSON Load + Partition + Geometry Pass (20 runs)
    val loadStartNs = System.nanoTime()
    var loadedState = repo.loadLayout()
    val firstColdLoadMs = (System.nanoTime() - loadStartNs) / 1_000_000.0

    val warmPassStartNs = System.nanoTime()
    repeat(20) {
      loadedState = repo.loadLayout()
      val bands = MetroGridGeometry.partitionIntoBands(loadedState.tiles)
      val unitPx = MetroGridGeometry.computeUnitHeightPx(1080, 12)
      bands.forEach { band ->
        band.tiles.forEach { t ->
          MetroGridGeometry.computeTileBounds(1080, 12, unitPx, t, band.startRow)
        }
      }
    }
    val avgWarmPassMs = ((System.nanoTime() - warmPassStartNs) / 1_000_000.0) / 20.0

    // Measure Standard vs Light Mode IconLoader + Photo decode target bounds
    val loader = AndroidAppIconLoader(context)
    loader.setLightweightMode(false)
    val runtime = Runtime.getRuntime()
    val beforeLightMem = runtime.totalMemory() - runtime.freeMemory()
    loader.setLightweightMode(true)
    loader.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)
    val afterTrimMem = runtime.totalMemory() - runtime.freeMemory()

    println(
      "[BLOCO_BENCHMARK] tiles=${loadedState.tiles.size} hubs=${loadedState.hubs.size} " +
        "coldLoadMs=${"%.2f".format(firstColdLoadMs)} avgWarmLoadAndGridMs=${"%.2f".format(avgWarmPassMs)} " +
        "memDeltaBytes=${afterTrimMem - beforeLightMem}"
    )

    assertEquals(60, loadedState.tiles.size)
    assertEquals(2, loadedState.hubs.size)
    assertTrue(avgWarmPassMs < 250.0)
  }
}
