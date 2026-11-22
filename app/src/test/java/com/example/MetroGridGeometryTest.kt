package com.example

import com.example.layout.MetroGridGeometry
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class MetroGridGeometryTest {

  private data class GeometryFixture(
    val widthPx: Int,
    val availableTrackPx: Int,
    val starts: IntArray,
    val smallLeftWidth: Int,
    val smallRightStart: Int,
    val smallRightWidth: Int,
    val centerSquareStart: Int,
    val centerSquareEnd: Int,
    val centerSquareWidth: Int,
    val rightSquareStart: Int,
    val rightSquareWidth: Int,
    val leftCalendarEnd: Int,
    val leftCalendarWidth: Int,
    val rightPhotoStart: Int,
    val rightPhotoWidth: Int,
  )

  private val fixtures =
    listOf(
      GeometryFixture(320, 300, intArrayOf(0, 54, 108, 162, 216, 270), 50, 270, 50, 108, 212, 104, 216, 104, 212, 212, 108, 212),
      GeometryFixture(360, 340, intArrayOf(0, 60, 121, 182, 242, 303), 56, 303, 57, 121, 238, 117, 242, 118, 238, 238, 121, 239),
      GeometryFixture(393, 373, intArrayOf(0, 66, 132, 198, 264, 330), 62, 330, 63, 132, 260, 128, 264, 129, 260, 260, 132, 261),
      GeometryFixture(411, 391, intArrayOf(0, 69, 138, 207, 276, 345), 65, 345, 66, 138, 272, 134, 276, 135, 272, 272, 138, 273),
      GeometryFixture(432, 412, intArrayOf(0, 72, 145, 218, 290, 363), 68, 363, 69, 145, 286, 141, 290, 142, 286, 286, 145, 287),
      GeometryFixture(480, 460, intArrayOf(0, 80, 161, 242, 322, 403), 76, 403, 77, 161, 318, 157, 322, 158, 318, 318, 161, 319),
      GeometryFixture(540, 520, intArrayOf(0, 90, 181, 272, 362, 453), 86, 453, 87, 181, 358, 177, 362, 178, 358, 358, 181, 359),
      GeometryFixture(600, 580, intArrayOf(0, 100, 201, 302, 402, 503), 96, 503, 97, 201, 398, 197, 402, 198, 398, 398, 201, 399),
      GeometryFixture(720, 700, intArrayOf(0, 120, 241, 362, 482, 603), 116, 603, 117, 241, 478, 237, 482, 238, 478, 478, 241, 479),
      GeometryFixture(900, 880, intArrayOf(0, 150, 301, 452, 602, 753), 146, 753, 147, 301, 598, 297, 602, 298, 598, 598, 301, 599),
      GeometryFixture(1080, 1060, intArrayOf(0, 180, 361, 542, 722, 903), 176, 903, 177, 361, 718, 357, 722, 358, 718, 718, 361, 719),
      GeometryFixture(1200, 1180, intArrayOf(0, 200, 401, 602, 802, 1003), 196, 1003, 197, 401, 798, 397, 802, 398, 798, 798, 401, 799),
    )

  @Test
  fun `all twelve pixel geometry acceptance fixtures match exactly`() {
    val gutter = 4
    for (f in fixtures) {
      assertEquals("Available track px for ${f.widthPx}", f.availableTrackPx, MetroGridGeometry.computeAvailableTrackPixels(f.widthPx, gutter))
      assertArrayEquals("Column starts for ${f.widthPx}", f.starts, MetroGridGeometry.computeColumnStarts(f.widthPx, gutter))

      val smallLeft = MetroGridGeometry.computeHorizontalBounds(f.widthPx, gutter, col = 0, colSpan = 1)
      assertEquals(0, smallLeft.startPx)
      assertEquals(f.smallLeftWidth, smallLeft.endExclusivePx)
      assertEquals(f.smallLeftWidth, smallLeft.widthPx)

      val smallRight = MetroGridGeometry.computeHorizontalBounds(f.widthPx, gutter, col = 5, colSpan = 1)
      assertEquals(f.smallRightStart, smallRight.startPx)
      assertEquals(f.widthPx, smallRight.endExclusivePx)
      assertEquals(f.smallRightWidth, smallRight.widthPx)

      val centerSquare = MetroGridGeometry.computeHorizontalBounds(f.widthPx, gutter, col = 2, colSpan = 2)
      assertEquals(f.centerSquareStart, centerSquare.startPx)
      assertEquals(f.centerSquareEnd, centerSquare.endExclusivePx)
      assertEquals(f.centerSquareWidth, centerSquare.widthPx)

      val rightSquare = MetroGridGeometry.computeHorizontalBounds(f.widthPx, gutter, col = 4, colSpan = 2)
      assertEquals(f.rightSquareStart, rightSquare.startPx)
      assertEquals(f.widthPx, rightSquare.endExclusivePx)
      assertEquals(f.rightSquareWidth, rightSquare.widthPx)

      val leftCalendar = MetroGridGeometry.computeHorizontalBounds(f.widthPx, gutter, col = 0, colSpan = 4)
      assertEquals(0, leftCalendar.startPx)
      assertEquals(f.leftCalendarEnd, leftCalendar.endExclusivePx)
      assertEquals(f.leftCalendarWidth, leftCalendar.widthPx)

      val rightPhoto = MetroGridGeometry.computeHorizontalBounds(f.widthPx, gutter, col = 2, colSpan = 4)
      assertEquals(f.rightPhotoStart, rightPhoto.startPx)
      assertEquals(f.widthPx, rightPhoto.endExclusivePx)
      assertEquals(f.rightPhotoWidth, rightPhoto.widthPx)
    }
  }
}
