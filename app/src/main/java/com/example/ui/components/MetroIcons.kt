package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.example.model.RoleSlot

val MetroFourSquaresOutlineIcon: ImageVector by lazy {
  ImageVector.Builder(
      name = "MetroFourSquaresOutline",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
    )
    .apply {
      path(
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Square,
        strokeLineJoin = StrokeJoin.Miter,
      ) {
        // Top-left square
        moveTo(3.5f, 3.5f)
        lineTo(10.5f, 3.5f)
        lineTo(10.5f, 10.5f)
        lineTo(3.5f, 10.5f)
        close()
        // Top-right square
        moveTo(13.5f, 3.5f)
        lineTo(20.5f, 3.5f)
        lineTo(20.5f, 10.5f)
        lineTo(13.5f, 10.5f)
        close()
        // Bottom-left square
        moveTo(3.5f, 13.5f)
        lineTo(10.5f, 13.5f)
        lineTo(10.5f, 20.5f)
        lineTo(3.5f, 20.5f)
        close()
        // Bottom-right square
        moveTo(13.5f, 13.5f)
        lineTo(20.5f, 13.5f)
        lineTo(20.5f, 20.5f)
        lineTo(13.5f, 20.5f)
        close()
      }
    }
    .build()
}

val MetroFourSquaresIcon: ImageVector by lazy {
  ImageVector.Builder(
      name = "MetroFourSquares",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
    )
    .apply {
      path(fill = SolidColor(Color.White)) {
        moveTo(3f, 3f); lineTo(10.5f, 3f); lineTo(10.5f, 10.5f); lineTo(3f, 10.5f); close()
        moveTo(13.5f, 3f); lineTo(21f, 3f); lineTo(21f, 10.5f); lineTo(13.5f, 10.5f); close()
        moveTo(3f, 13.5f); lineTo(10.5f, 13.5f); lineTo(10.5f, 21f); lineTo(3f, 21f); close()
        moveTo(13.5f, 13.5f); lineTo(21f, 13.5f); lineTo(21f, 21f); lineTo(13.5f, 21f); close()
      }
    }
    .build()
}

fun roleGlyphOrNull(roleSlot: RoleSlot?): ImageVector? =
  when (roleSlot) {
    RoleSlot.PHONE -> Icons.Outlined.Call
    RoleSlot.CONTACTS -> Icons.Outlined.Person
    RoleSlot.MESSAGES -> Icons.Outlined.ChatBubbleOutline
    RoleSlot.MAIL -> Icons.Outlined.Email
    RoleSlot.MAPS -> Icons.Outlined.LocationOn
    RoleSlot.MUSIC -> Icons.Outlined.MusicNote
    RoleSlot.BROWSER -> Icons.Outlined.Language
    RoleSlot.WALLET -> Icons.Outlined.AccountBalanceWallet
    RoleSlot.READER -> Icons.Outlined.MenuBook
    RoleSlot.FILES -> Icons.Outlined.Folder
    RoleSlot.CAMERA -> Icons.Outlined.PhotoCamera
    RoleSlot.NOTES -> Icons.Outlined.EditNote
    else -> null
  }

val MetroCodeIcon: ImageVector by lazy {
  ImageVector.Builder(
      name = "MetroCodeIcon",
      defaultWidth = 28.dp,
      defaultHeight = 28.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
    )
    .apply {
      path(
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.0f,
        strokeLineCap = StrokeCap.Square,
        strokeLineJoin = StrokeJoin.Miter,
      ) {
        // Left chevron '<'
        moveTo(8.5f, 7f)
        lineTo(3.5f, 12f)
        lineTo(8.5f, 17f)
        // Slash '/'
        moveTo(13.5f, 5.5f)
        lineTo(10.5f, 18.5f)
        // Right chevron '>'
        moveTo(15.5f, 7f)
        lineTo(20.5f, 12f)
        lineTo(15.5f, 17f)
      }
    }
    .build()
}

