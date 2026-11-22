package com.example.layout

import com.example.model.StartTile
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

data class HorizontalBounds(
  val startPx: Int,
  val endExclusivePx: Int,
) {
  val widthPx: Int
    get() = endExclusivePx - startPx
}

data class TilePixelBounds(
  val leftPx: Int,
  val topPx: Int,
  val rightPx: Int,
  val bottomPx: Int,
) {
  val widthPx: Int
    get() = rightPx - leftPx

  val heightPx: Int
    get() = bottomPx - topPx
}

data class TileBand(
  val startRow: Int,
  val endRowExclusive: Int,
  val tiles: List<StartTile>,
) {
  val rowSpan: Int
    get() = endRowExclusive - startRow
}

object MetroGridGeometry {
  const val GRID_COLUMNS = 6

  fun computeAvailableTrackPixels(widthPx: Int, gutterPx: Int): Int {
    return (widthPx - (GRID_COLUMNS - 1) * gutterPx).coerceAtLeast(GRID_COLUMNS)
  }

  /**
   * Computes exact column start pixel coordinates matching:
   * floor(i * p / 6) + gutterPx * i
   */
  fun computeColumnStarts(widthPx: Int, gutterPx: Int): IntArray {
    val p = computeAvailableTrackPixels(widthPx, gutterPx)
    return IntArray(GRID_COLUMNS) { col ->
      floor((col.toDouble() * p) / GRID_COLUMNS).toInt() + gutterPx * col
    }
  }

  /**
   * Computes inclusive-start, exclusive-end horizontal bounds for a tile spanning
   * [col, col + colSpan), stretching the rightmost column to exact widthPx so no
   * right-edge gap ever accumulates.
   */
  fun computeHorizontalBounds(
    widthPx: Int,
    gutterPx: Int,
    col: Int,
    colSpan: Int,
  ): HorizontalBounds {
    require(col in 0 until GRID_COLUMNS) { "Column $col out of 0..5" }
    require(colSpan >= 1 && col + colSpan <= GRID_COLUMNS) {
      "Span $colSpan at col $col exceeds $GRID_COLUMNS columns"
    }
    val starts = computeColumnStarts(widthPx, gutterPx)
    val startPx = starts[col]
    val endCol = col + colSpan
    val endExclusivePx =
      if (endCol >= GRID_COLUMNS) {
        widthPx
      } else {
        starts[endCol] - gutterPx
      }
    return HorizontalBounds(startPx = startPx, endExclusivePx = endExclusivePx)
  }

  fun computeUnitHeightPx(widthPx: Int, gutterPx: Int): Int {
    val p = computeAvailableTrackPixels(widthPx, gutterPx)
    return (p.toDouble() / GRID_COLUMNS).roundToInt().coerceAtLeast(1)
  }

  fun computeTileBounds(
    widthPx: Int,
    gutterPx: Int,
    unitHeightPx: Int,
    tile: StartTile,
    rowOffset: Int = 0,
  ): TilePixelBounds {
    val horizontal =
      computeHorizontalBounds(
        widthPx = widthPx,
        gutterPx = gutterPx,
        col = tile.col.coerceIn(0, GRID_COLUMNS - 1),
        colSpan = tile.colSpan.coerceIn(1, GRID_COLUMNS - tile.col.coerceIn(0, GRID_COLUMNS - 1)),
      )
    val localRow = (tile.row - rowOffset).coerceAtLeast(0)
    val topPx = localRow * (unitHeightPx + gutterPx)
    val heightPx = tile.rowSpan * unitHeightPx + (tile.rowSpan - 1).coerceAtLeast(0) * gutterPx
    return TilePixelBounds(
      leftPx = horizontal.startPx,
      topPx = topPx,
      rightPx = horizontal.endExclusivePx,
      bottomPx = topPx + heightPx,
    )
  }

  /**
   * Partitions tiles into non-overlapping vertical row bands so LazyColumn can recycle
   * horizontal bands while rendering exact irregular 6-unit Metro footprints inside each band.
   */
  fun partitionIntoBands(tiles: List<StartTile>): List<TileBand> {
    if (tiles.isEmpty()) return emptyList()
    val sorted = tiles.sortedWith(compareBy<StartTile> { it.row }.thenBy { it.col }.thenBy { it.id })
    val bands = mutableListOf<TileBand>()
    var currentStart = sorted.first().row
    var currentEnd = sorted.first().row + sorted.first().rowSpan
    var currentTiles = mutableListOf(sorted.first())

    for (index in 1 until sorted.size) {
      val tile = sorted[index]
      val tileEnd = tile.row + tile.rowSpan
      if (tile.row < currentEnd) {
        currentEnd = max(currentEnd, tileEnd)
        currentTiles.add(tile)
      } else {
        bands.add(TileBand(currentStart, currentEnd, currentTiles))
        currentStart = tile.row
        currentEnd = tileEnd
        currentTiles = mutableListOf(tile)
      }
    }
    bands.add(TileBand(currentStart, currentEnd, currentTiles))
    return bands
  }
}
