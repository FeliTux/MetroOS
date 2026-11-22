package com.example.layout

import com.example.model.StartTile
import com.example.model.TileKind
import com.example.model.TileSize
import kotlin.math.max

object MetroGridEditor {
  private const val GRID_COLUMNS = MetroGridGeometry.GRID_COLUMNS

  fun overlaps(a: StartTile, b: StartTile): Boolean {
    val horizOverlap = a.col < (b.col + b.colSpan) && (a.col + a.colSpan) > b.col
    val vertOverlap = a.row < (b.row + b.rowSpan) && (a.row + a.rowSpan) > b.row
    return horizOverlap && vertOverlap
  }

  fun findFirstOpenPosition(
    tiles: List<StartTile>,
    colSpan: Int,
    rowSpan: Int,
  ): Pair<Int, Int> {
    val clampedColSpan = colSpan.coerceIn(1, GRID_COLUMNS)
    val maxRow = tiles.maxOfOrNull { it.row + it.rowSpan } ?: 0
    for (row in 0..(maxRow + rowSpan + 2)) {
      for (col in 0..(GRID_COLUMNS - clampedColSpan)) {
        val probe =
          StartTile(
            id = "__probe__",
            kind = TileKind.APP_SHORTCUT,
            col = col,
            row = row,
            colSpan = clampedColSpan,
            rowSpan = rowSpan,
          )
        if (tiles.none { overlaps(it, probe) }) {
          return col to row
        }
      }
    }
    return 0 to maxRow
  }

  fun findMergeTargetAtCell(
    tiles: List<StartTile>,
    draggedTile: StartTile,
    targetCol: Int,
    targetRow: Int,
  ): StartTile? {
    if (draggedTile.kind != TileKind.APP_SHORTCUT || draggedTile.target == null) return null
    val centerCol = targetCol.coerceIn(0, GRID_COLUMNS - 1)
    val centerRow = targetRow.coerceAtLeast(0)
    return tiles.firstOrNull { candidate ->
      candidate.id != draggedTile.id &&
        (candidate.kind == TileKind.APP_SHORTCUT && candidate.target != null || candidate.kind == TileKind.FOLDER) &&
        centerCol >= candidate.col &&
        centerCol < candidate.col + candidate.colSpan &&
        centerRow >= candidate.row &&
        centerRow < candidate.row + candidate.rowSpan
    }
  }

  fun moveTileWithReflow(
    tiles: List<StartTile>,
    tileId: String,
    targetCol: Int,
    targetRow: Int,
  ): List<StartTile> {
    val active = tiles.firstOrNull { it.id == tileId } ?: return tiles
    val clampedCol = targetCol.coerceIn(0, GRID_COLUMNS - active.colSpan)
    val clampedRow = targetRow.coerceAtLeast(0)
    val moved = active.copy(col = clampedCol, row = clampedRow)
    return resolveCollisionsWithAnchor(tiles, moved)
  }

  fun stepMoveTile(
    tiles: List<StartTile>,
    tileId: String,
    deltaCol: Int,
    deltaRow: Int,
  ): List<StartTile> {
    val active = tiles.firstOrNull { it.id == tileId } ?: return tiles
    return moveTileWithReflow(
      tiles = tiles,
      tileId = tileId,
      targetCol = active.col + deltaCol,
      targetRow = active.row + deltaRow,
    )
  }

  fun resizeTileWithReflow(
    tiles: List<StartTile>,
    tileId: String,
    newSize: TileSize,
  ): List<StartTile> {
    val active = tiles.firstOrNull { it.id == tileId } ?: return tiles
    val newCol = active.col.coerceIn(0, GRID_COLUMNS - newSize.colSpan)
    val resized =
      active.copy(
        col = newCol,
        colSpan = newSize.colSpan,
        rowSpan = newSize.rowSpan,
      )
    return resolveCollisionsWithAnchor(tiles, resized)
  }

  fun removeTile(tiles: List<StartTile>, tileId: String): List<StartTile> {
    return tiles.filterNot { it.id == tileId }
  }

  fun placeTilesAfterDissolve(
    existingTilesWithoutFolder: List<StartTile>,
    releasedTiles: List<StartTile>,
  ): List<StartTile> {
    var current = existingTilesWithoutFolder
    for (tile in releasedTiles) {
      val (c, r) = findFirstOpenPosition(current, tile.colSpan, tile.rowSpan)
      current = current + tile.copy(col = c, row = r)
    }
    return current.sortedWith(compareBy<StartTile> { it.row }.thenBy { it.col }.thenBy { it.id })
  }

  private fun resolveCollisionsWithAnchor(
    originalTiles: List<StartTile>,
    anchorTile: StartTile,
  ): List<StartTile> {
    val others =
      originalTiles
        .filterNot { it.id == anchorTile.id }
        .sortedWith(compareBy<StartTile> { it.row }.thenBy { it.col }.thenBy { it.id })

    val settled = mutableListOf(anchorTile)
    for (tile in others) {
      var candidate = tile
      while (true) {
        val blocker = settled.firstOrNull { overlaps(it, candidate) } ?: break
        candidate = candidate.copy(row = max(candidate.row + 1, blocker.row + blocker.rowSpan))
      }
      settled.add(candidate)
    }
    return settled.sortedWith(compareBy<StartTile> { it.row }.thenBy { it.col }.thenBy { it.id })
  }
}
