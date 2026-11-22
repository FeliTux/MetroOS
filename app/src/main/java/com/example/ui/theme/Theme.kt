package com.example.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.MetroColorUtils

private val MetroShapes =
  Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
  )

@Composable
fun MetroHomeTheme(
  isDarkTheme: Boolean = true,
  accentColor: Color = MetroDefaultBlue,
  backgroundColor: Color = if (isDarkTheme) MetroBlack else MetroLightBackground,
  content: @Composable () -> Unit,
) {
  val effectiveBg =
    if (isDarkTheme) {
      if (MetroColorUtils.relativeLuminance(backgroundColor) > 0.45f) MetroBlack else backgroundColor
    } else {
      MetroLightBackground
    }
  val onBg = MetroColorUtils.legibleForeground(effectiveBg)
  val onPrimary = MetroColorUtils.legibleForeground(accentColor)

  val colorScheme =
    if (isDarkTheme) {
      darkColorScheme(
        primary = accentColor,
        onPrimary = onPrimary,
        secondary = accentColor,
        onSecondary = onPrimary,
        background = effectiveBg,
        onBackground = onBg,
        surface = effectiveBg,
        onSurface = onBg,
        surfaceVariant = MetroSurface,
        onSurfaceVariant = MetroMutedWhite,
        surfaceContainer = MetroSurface,
        surfaceContainerHigh = MetroSurfaceElevated,
        outline = MetroSubtleDivider,
        error = MetroErrorRed,
        onError = MetroBlack,
      )
    } else {
      lightColorScheme(
        primary = accentColor,
        onPrimary = onPrimary,
        secondary = accentColor,
        onSecondary = onPrimary,
        background = effectiveBg,
        onBackground = MetroLightInk,
        surface = effectiveBg,
        onSurface = MetroLightInk,
        surfaceVariant = MetroLightSurface,
        onSurfaceVariant = MetroLightMutedInk,
        surfaceContainer = MetroLightSurface,
        surfaceContainerHigh = MetroLightSurfaceElevated,
        outline = MetroLightDivider,
        error = MetroLightErrorRed,
        onError = MetroWhite,
      )
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = MetroTypography,
    shapes = MetroShapes,
    content = content,
  )
}

fun Modifier.metroFocusIndicator(
  accentColor: Color,
  contrastColor: Color,
): Modifier = composed {
  var isFocused by remember { mutableStateOf(false) }
  this.onFocusChanged { isFocused = it.isFocused }
    .then(
      if (isFocused) {
        Modifier.border(2.dp, accentColor).paddingFocusInner(contrastColor)
      } else {
        Modifier
      }
    )
}

private fun Modifier.paddingFocusInner(contrastColor: Color): Modifier =
  this.border(1.dp, contrastColor)
