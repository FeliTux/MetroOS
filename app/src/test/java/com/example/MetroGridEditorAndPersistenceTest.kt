package com.example

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppCatalogManager
import com.example.data.AtomicJsonLayoutRepository
import com.example.data.PreferencesRepository
import com.example.layout.MetroGridEditor
import com.example.layout.MetroGridGeometry
import com.example.model.AppTargetKey
import com.example.model.LeftCommandSlot
import com.example.model.MetroAccent
import com.example.model.MetroColorUtils
import com.example.model.MetroFolder
import com.example.model.QuickNote
import com.example.model.RoleSlot
import com.example.model.StartLayoutState
import com.example.model.StartTile
import com.example.model.TileKind
import com.example.model.TileSize
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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MetroGridEditorAndPersistenceTest {

  @get:Rule val tempFolder = TemporaryFolder()

  private fun sampleTiles(): List<StartTile> =
    listOf(
      StartTile(
        id = "t1",
        kind = TileKind.APP_SHORTCUT,
        col = 0,
        row = 0,
        colSpan = 1,
        rowSpan = 1,
        target = AppTargetKey("com.alpha", "com.alpha.Main", 0L),
        fallbackLabel = "Alpha",
      ),
      StartTile(
        id = "t2",
        kind = TileKind.APP_SHORTCUT,
        col = 1,
        row = 0,
        colSpan = 1,
        rowSpan = 1,
        target = AppTargetKey("com.beta", "com.beta.Main", 0L),
        fallbackLabel = "Beta",
      ),
      StartTile("t3", TileKind.CLOCK, col = 2, row = 0, colSpan = 2, rowSpan = 2, fallbackLabel = "Relógio"),
      StartTile("t4", TileKind.BATTERY, col = 4, row = 0, colSpan = 2, rowSpan = 2, fallbackLabel = "Bateria"),
    )

  @Test
  fun `growing a colliding tile reflows displaced tiles without overlap`() {
    val resized = MetroGridEditor.resizeTileWithReflow(sampleTiles(), "t1", TileSize.WIDE)
    val t1 = resized.first { it.id == "t1" }
    assertEquals(4, t1.colSpan)
    assertEquals(2, t1.rowSpan)
    assertEquals(4, resized.size)

    for (i in resized.indices) {
      for (j in i + 1 until resized.size) {
        assertFalse(
          "Tiles ${resized[i].id} and ${resized[j].id} must not overlap",
          MetroGridEditor.overlaps(resized[i], resized[j]),
        )
      }
    }
  }

  @Test
  fun `stepMoveTile clamps to 6-column bounds`() {
    val movedRight = MetroGridEditor.stepMoveTile(sampleTiles(), "t4", deltaCol = 1, deltaRow = 0)
    val t4 = movedRight.first { it.id == "t4" }
    assertEquals(4, t4.col)

    val movedLeft = MetroGridEditor.stepMoveTile(sampleTiles(), "t1", deltaCol = -1, deltaRow = 0)
    val t1 = movedLeft.first { it.id == "t1" }
    assertEquals(0, t1.col)
  }

  @Test
  fun `band partitioning keeps non-overlapping rows separate`() {
    val tiles =
      sampleTiles() +
        StartTile("t5", TileKind.PHOTO, col = 0, row = 2, colSpan = 4, rowSpan = 2, fallbackLabel = "Fotos")
    val bands = MetroGridGeometry.partitionIntoBands(tiles)
    assertEquals(2, bands.size)
    assertEquals(0, bands[0].startRow)
    assertEquals(2, bands[0].endRowExclusive)
    assertEquals(2, bands[1].startRow)
    assertEquals(4, bands[1].endRowExclusive)
  }

  @Test
  fun `60-tile stress layout partitions cleanly without overlapping tiles`() {
    val tiles =
      (0 until 60).map { idx ->
        val row = (idx / 3) * 2
        val col = (idx % 3) * 2
        StartTile(
          id = "stress_$idx",
          kind = TileKind.APP_SHORTCUT,
          col = col,
          row = row,
          colSpan = 2,
          rowSpan = 2,
          fallbackLabel = "App $idx",
        )
      }
    val bands = MetroGridGeometry.partitionIntoBands(tiles)
    assertEquals(20, bands.size)
    assertEquals(tiles.size, bands.sumOf { it.tiles.size })
  }

  @Test
  fun `drag merge target and folder dissolve preserve non-overlapping positions`() {
    val tiles = sampleTiles()
    val dragged = tiles.first { it.id == "t1" }
    val mergeTarget = MetroGridEditor.findMergeTargetAtCell(tiles, dragged, targetCol = 1, targetRow = 0)
    assertNotNull(mergeTarget)
    assertEquals("t2", mergeTarget?.id)

    // Clock tile is not a merge target
    val nonMergeTarget = MetroGridEditor.findMergeTargetAtCell(tiles, dragged, targetCol = 2, targetRow = 0)
    assertNull(nonMergeTarget)

    // Dissolving releases child shortcuts into open positions without colliding with clock/battery
    val remaining = tiles.filterNot { it.id == "t1" || it.id == "t2" }
    val released =
      listOf(
        StartTile("rel1", TileKind.APP_SHORTCUT, 0, 0, 2, 2, fallbackLabel = "Alpha"),
        StartTile("rel2", TileKind.APP_SHORTCUT, 0, 0, 2, 2, fallbackLabel = "Beta"),
      )
    val placed = MetroGridEditor.placeTilesAfterDissolve(remaining, released)
    assertEquals(4, placed.size)
    for (i in placed.indices) {
      for (j in i + 1 until placed.size) {
        assertFalse(MetroGridEditor.overlaps(placed[i], placed[j]))
      }
    }
  }

  @Test
  fun `schema v1 layout migrates seamlessly to v2 preserving existing pinned tiles and positions`() = runTest {
    val file = File(tempFolder.root, "legacy_v1.json")
    file.writeText(
      """
      {
        "schemaVersion": 1,
        "revision": 7,
        "initialized": true,
        "tiles": [
          {
            "id": "legacy_phone",
            "kind": "APP_SHORTCUT",
            "col": 0,
            "row": 0,
            "colSpan": 1,
            "rowSpan": 1,
            "fallbackLabel": "Telefone",
            "roleSlot": "PHONE",
            "target": {
              "packageName": "com.motorola.dialer",
              "className": "com.motorola.dialer.Main",
              "userSerialNumber": 0
            }
          }
        ]
      }
      """.trimIndent()
    )

    val migrated = AtomicJsonLayoutRepository(file).loadLayout()
    assertTrue(migrated.initialized)
    assertEquals(7L, migrated.revision)
    assertEquals(StartLayoutState.CURRENT_SCHEMA_VERSION, migrated.schemaVersion)
    assertEquals(1, migrated.tiles.size)
    val tile = migrated.tiles.first()
    assertEquals("legacy_phone", tile.id)
    assertEquals(RoleSlot.PHONE, tile.roleSlot)
    assertEquals("com.motorola.dialer", tile.target?.packageName)
    assertTrue(migrated.folders.isEmpty())
    assertTrue(migrated.notes.isEmpty())
  }

  @Test
  fun `atomic layout persistence preserves schema version and intentional empty grid`() = runTest {
    val file = File(tempFolder.root, "layout.json")
    val repo = AtomicJsonLayoutRepository(file)

    val initial = repo.loadLayout()
    assertFalse(initial.initialized)

    val withTile =
      StartLayoutState(
        tiles =
          listOf(
            StartTile(
              id = "pin_1",
              kind = TileKind.APP_SHORTCUT,
              col = 0,
              row = 0,
              colSpan = 2,
              rowSpan = 2,
              target = AppTargetKey("com.demo", "com.demo.Main", 0L),
              fallbackLabel = "Demo",
            )
          )
      )
    val saved1 = repo.saveLayout(withTile).getOrThrow()
    assertEquals(1L, saved1.revision)
    assertTrue(saved1.initialized)

    // Remove final tile and persist intentional empty layout
    val savedEmpty = repo.saveLayout(saved1.copy(tiles = emptyList())).getOrThrow()
    assertEquals(2L, savedEmpty.revision)
    assertTrue(savedEmpty.initialized)
    assertTrue(savedEmpty.tiles.isEmpty())

    // Reloading in a fresh repository instance preserves initialized=true and empty tiles
    val reloaded = AtomicJsonLayoutRepository(file).loadLayout()
    assertTrue(reloaded.initialized)
    assertEquals(2L, reloaded.revision)
    assertTrue(reloaded.tiles.isEmpty())
  }

  @Test
  fun `acceptance flow - Jogos folder, wide note Prova de sexta in violet and quick note bar survive restart`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val layoutFile = File(tempFolder.root, "bloco_acceptance.json")
    val repo = AtomicJsonLayoutRepository(layoutFile)
    val prefsRepo = PreferencesRepository(context)

    val gameKey1 = AppTargetKey("com.game.one", "com.game.one.Main", 0L)
    val gameKey2 = AppTargetKey("com.game.two", "com.game.two.Main", 0L)
    val folderJogos = MetroFolder(id = "folder_jogos", name = "Jogos", appKeys = listOf(gameKey1, gameKey2))
    val noteProva =
      QuickNote(
        id = "note_prova",
        title = "Prova de sexta",
        body = "[ ] Revisar matemática\n[x] Fazer simulado",
        isChecklist = true,
        updatedAtMs = 123456L,
      )

    val folderTile =
      StartTile(
        id = "tile_folder_jogos",
        kind = TileKind.FOLDER,
        col = 0,
        row = 6,
        colSpan = 2,
        rowSpan = 2,
        fallbackLabel = "Jogos",
        folderId = folderJogos.id,
      )
    val noteTile =
      StartTile(
        id = "tile_note_prova",
        kind = TileKind.NOTE,
        col = 2,
        row = 6,
        colSpan = 4,
        rowSpan = 2,
        fallbackLabel = "Prova de sexta",
        noteId = noteProva.id,
        customColorHex = MetroAccent.VIOLET.colorValue,
      )

    repo.saveLayout(
      StartLayoutState(
        tiles = listOf(folderTile, noteTile),
        folders = listOf(folderJogos),
        notes = listOf(noteProva),
      )
    ).getOrThrow()
    prefsRepo.setLeftCommandSlot(LeftCommandSlot.QUICK_NOTE)
    prefsRepo.setGutterDp(6)

    // Simulate launcher cold restart with fresh repositories
    val reloadedLayout = AtomicJsonLayoutRepository(layoutFile).loadLayout()
    val reloadedPrefs = PreferencesRepository(context).preferencesFlow.first()

    val savedFolder = reloadedLayout.folders.firstOrNull { it.name == "Jogos" }
    assertNotNull(savedFolder)
    assertEquals(listOf(gameKey1, gameKey2), savedFolder?.appKeys)

    val savedNote = reloadedLayout.notes.firstOrNull { it.title == "Prova de sexta" }
    assertNotNull(savedNote)
    assertTrue(savedNote!!.isChecklist)
    assertTrue(savedNote.body.contains("Fazer simulado"))

    val savedNoteTile = reloadedLayout.tiles.first { it.noteId == noteProva.id }
    assertEquals(TileSize.WIDE, savedNoteTile.size)
    assertEquals(MetroAccent.VIOLET.colorValue, savedNoteTile.customColorHex)
    assertNull(reloadedLayout.tiles.first { it.folderId == folderJogos.id }.customColorHex)

    assertEquals(LeftCommandSlot.QUICK_NOTE, reloadedPrefs.leftCommandSlot)
    assertEquals(6, reloadedPrefs.gutterDp)
  }

  @Test
  fun `color contrast selects dark foreground on light colors and parses hex codes`() {
    assertEquals(Color(0xFF111111), MetroColorUtils.legibleForeground(Color(0xFFFFEE58)))
    assertEquals(Color.White, MetroColorUtils.legibleForeground(MetroAccent.METRO_BLUE.color))
    assertEquals(Color.White, MetroColorUtils.legibleForeground(MetroAccent.VIOLET.color))
    assertEquals(0xFF6B3FA0L, MetroColorUtils.parseHexColor("#6B3FA0"))
    assertEquals("#256D94", MetroColorUtils.formatHexColor(0xFF256D94L))
    assertEquals(MetroAccent.METRO_BLUE, MetroAccent.fromId("cobalt"))
    assertEquals(MetroAccent.VIOLET, MetroAccent.fromId("royal_purple"))
  }

  @Test
  fun `search normalization matches accented Portuguese labels with unaccented queries`() {
    assertEquals("musica", AppCatalogManager.normalizeForSearch("Música"))
    assertEquals("relogio", AppCatalogManager.normalizeForSearch("Relógio"))
    assertEquals("configuracoes", AppCatalogManager.normalizeForSearch("Configurações"))
    assertTrue(
      AppCatalogManager.normalizeForSearch("Calendário")
        .contains(AppCatalogManager.normalizeForSearch("calendario"))
    )
  }
}
