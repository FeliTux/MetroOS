package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.LoadedAppIcon
import com.example.model.AppTargetKey
import com.example.model.InstalledApp
import kotlinx.coroutines.isActive

/**
 * Displays an installed app icon safely:
 * 1) Synchronously resets to a neutral loading placeholder whenever [targetKey] or [iconCacheGeneration] changes.
 * 2) Cancels in-flight work cleanly during fast scroll / filter changes.
 * 3) Double-checks that the returned [LoadedAppIcon] still matches [targetKey] and [iconCacheGeneration].
 */
@Composable
fun AppIconImage(
  targetKey: AppTargetKey?,
  fallbackLabel: String,
  iconCacheGeneration: Long,
  loadAppIcon: suspend (AppTargetKey) -> LoadedAppIcon,
  iconSize: Dp,
  modifier: Modifier = Modifier,
  foregroundFallbackColor: Color = Color.White,
  themedMonochromeTint: Color? = null,
  testTagPrefix: String? = null,
) {
  // Synchronously invalidated whenever targetKey or generation changes so recycled views never show stale icons.
  var resolved by remember(targetKey, iconCacheGeneration) {
    mutableStateOf<LoadedAppIcon?>(null)
  }
  var loadFinished by remember(targetKey, iconCacheGeneration) {
    mutableStateOf(targetKey == null)
  }

  LaunchedEffect(targetKey, iconCacheGeneration) {
    if (targetKey == null) {
      resolved = null
      loadFinished = true
      return@LaunchedEffect
    }
    val result = loadAppIcon(targetKey)
    if (isActive && result.targetKey == targetKey && result.generation == iconCacheGeneration) {
      resolved = result
      loadFinished = true
    }
  }

  val currentBitmap =
    resolved?.takeIf { it.targetKey == targetKey && it.generation == iconCacheGeneration }?.bitmap

  val tagModifier =
    if (testTagPrefix != null && targetKey != null) {
      Modifier.testTag("${testTagPrefix}_${targetKey.stableKey}")
    } else {
      Modifier
    }

  Box(
    modifier = modifier.size(iconSize).then(tagModifier),
    contentAlignment = Alignment.Center,
  ) {
    when {
      currentBitmap != null -> {
        Image(
          bitmap = currentBitmap,
          contentDescription = null,
          contentScale = ContentScale.Fit,
          colorFilter = themedMonochromeTint?.let { ColorFilter.tint(it) },
          modifier = Modifier.size(iconSize),
        )
      }
      !loadFinished -> {
        // Neutral placeholder while async decode completes
        Box(
          modifier =
            Modifier.size(iconSize * 0.72f)
              .background(foregroundFallbackColor.copy(alpha = 0.16f)),
        )
      }
      else -> {
        Text(
          text = fallbackLabel.trim().take(2).uppercase().ifBlank { "?" },
          color = foregroundFallbackColor,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Light,
        )
      }
    }
  }
}

fun resolveAppRecord(
  targetKey: AppTargetKey?,
  fallbackLabel: String,
  installedApps: List<InstalledApp>,
): InstalledApp? {
  if (targetKey == null) return null
  return installedApps.firstOrNull { it.key == targetKey }
}
