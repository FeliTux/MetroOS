package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.LoadedAppIcon
import com.example.model.AppTargetKey
import com.example.model.LiveTilesState
import com.example.model.MetroFolder
import com.example.model.MetroHub
import com.example.model.PhotoCropMode
import com.example.model.QuickNote
import com.example.model.StartTile
import com.example.model.TileSize
import com.example.ui.theme.MetroBlack
import com.example.ui.theme.MetroWhite
import com.example.widget.AppWidgetController

@Composable
fun AppShortcutTileFace(
  tile: StartTile,
  label: String,
  showLabels: Boolean,
  themeThirdPartyIcons: Boolean,
  iconCacheGeneration: Long,
  fgColor: Color,
  loadAppIcon: suspend (AppTargetKey) -> LoadedAppIcon,
) {
  val isSmall = tile.size == TileSize.SMALL
  val tintForIcon = if (themeThirdPartyIcons && !tile.useOriginalIcon) fgColor else null

  Box(modifier = Modifier.fillMaxSize()) {
    val iconSize = if (isSmall) 28.dp else 44.dp
    val iconBottomPadding = if (isSmall || !showLabels) 0.dp else 14.dp

    AppIconImage(
      targetKey = tile.target,
      fallbackLabel = label,
      iconCacheGeneration = iconCacheGeneration,
      loadAppIcon = loadAppIcon,
      iconSize = iconSize,
      foregroundFallbackColor = fgColor,
      themedMonochromeTint = tintForIcon,
      testTagPrefix = "tile_icon_${tile.id}",
      modifier = Modifier.align(Alignment.Center).padding(bottom = iconBottomPadding),
    )

    if (!isSmall && showLabels) {
      Text(
        text = label,
        color = fgColor,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier =
          Modifier.align(Alignment.BottomStart)
            .padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
      )
    }
  }
}

@Composable
fun FolderTileFace(
  tile: StartTile,
  folder: MetroFolder?,
  showLabels: Boolean,
  iconCacheGeneration: Long,
  fgColor: Color,
  loadAppIcon: suspend (AppTargetKey) -> LoadedAppIcon,
) {
  val isSmall = tile.size == TileSize.SMALL
  val previewKeys = folder?.appKeys.orEmpty().take(4)

  Box(modifier = Modifier.fillMaxSize().padding(if (isSmall) 4.dp else 10.dp)) {
    if (previewKeys.isEmpty()) {
      Icon(
        imageVector = Icons.Outlined.Folder,
        contentDescription = null,
        tint = fgColor,
        modifier =
          Modifier.align(Alignment.Center)
            .padding(bottom = if (isSmall || !showLabels) 0.dp else 14.dp)
            .size(if (isSmall) 24.dp else 36.dp),
      )
    } else {
      val gridMod =
        if (isSmall) {
          Modifier.align(Alignment.Center)
        } else {
          Modifier.align(Alignment.TopStart).padding(bottom = if (showLabels) 18.dp else 0.dp)
        }
      Column(
        modifier = gridMod,
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        for (rowIdx in 0..1) {
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (colIdx in 0..1) {
              val itemIdx = rowIdx * 2 + colIdx
              val key = previewKeys.getOrNull(itemIdx)
              val iconBoxSize = if (isSmall) 16.dp else 26.dp
              if (key != null) {
                Box(
                  modifier = Modifier.size(iconBoxSize).background(fgColor.copy(alpha = 0.14f)),
                  contentAlignment = Alignment.Center,
                ) {
                  AppIconImage(
                    targetKey = key,
                    fallbackLabel = key.packageName,
                    iconCacheGeneration = iconCacheGeneration,
                    loadAppIcon = loadAppIcon,
                    iconSize = iconBoxSize * 0.84f,
                    foregroundFallbackColor = fgColor,
                  )
                }
              } else {
                Spacer(modifier = Modifier.size(iconBoxSize))
              }
            }
          }
        }
      }
    }

    if (!isSmall && showLabels) {
      Text(
        text = folder?.name ?: tile.fallbackLabel,
        color = fgColor,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.align(Alignment.BottomStart),
      )
    }
  }
}

@Composable
fun NoteTileFace(
  tile: StartTile,
  note: QuickNote?,
  fgColor: Color,
  mutedFg: Color,
) {
  val isWide = tile.size == TileSize.WIDE
  val displayTitle = note?.title.orEmpty()
  val formattedBody =
    remember(note?.body, note?.isChecklist) {
      val raw = note?.body.orEmpty()
      if (note?.isChecklist == true) {
        raw.lines()
          .filter { it.isNotBlank() }
          .joinToString("\n") { line ->
            when {
              line.startsWith("[x] ", ignoreCase = true) -> "☑ ${line.drop(4)}"
              line.startsWith("[ ] ") -> "☐ ${line.drop(4)}"
              else -> "☐ $line"
            }
          }
      } else {
        raw
      }
    }

  Column(
    modifier = Modifier.fillMaxSize().padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Icon(
        imageVector = Icons.Outlined.EditNote,
        contentDescription = null,
        tint = mutedFg,
        modifier = Modifier.size(16.dp),
      )
      Text(
        text = displayTitle.ifBlank { stringResource(R.string.quick_note) },
        color = fgColor,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Text(
      text = formattedBody.ifBlank { stringResource(R.string.note_body_hint) },
      color = if (formattedBody.isBlank()) mutedFg else fgColor,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Normal,
      maxLines = if (isWide) 4 else 3,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
fun ClockTileFace(
  tile: StartTile,
  liveTiles: LiveTilesState,
  label: String,
  showLabels: Boolean,
  fgColor: Color,
  mutedFg: Color,
) {
  val isSmall = tile.size == TileSize.SMALL
  BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(if (isSmall) 4.dp else 10.dp)) {
    val compactHeight = maxHeight < 110.dp
    if (isSmall) {
      Text(
        text = if (tile.isLiveEnabled) liveTiles.timeFormatted.ifBlank { "--:--" } else label.take(3),
        color = fgColor,
        fontSize = 14.sp,
        fontWeight = FontWeight.Light,
        maxLines = 1,
        modifier = Modifier.align(Alignment.Center),
      )
    } else if (!tile.isLiveEnabled) {
      Column(modifier = Modifier.align(Alignment.CenterStart)) {
        Text(text = label, color = fgColor, style = MaterialTheme.typography.titleLarge)
        Text(
          text = stringResource(R.string.tile_mode_static),
          color = mutedFg,
          style = MaterialTheme.typography.labelMedium,
        )
      }
    } else {
      Column(
        modifier = Modifier.align(Alignment.CenterStart).padding(bottom = if (showLabels) 14.dp else 0.dp),
      ) {
        val clockSp =
          when {
            compactHeight -> 26.sp
            tile.size == TileSize.WIDE -> 40.sp
            else -> 32.sp
          }
        Text(
          text = liveTiles.timeFormatted.ifBlank { "--:--" },
          color = fgColor,
          fontSize = clockSp,
          fontWeight = FontWeight.Light,
          maxLines = 1,
        )
        if (!liveTiles.nextAlarmFormatted.isNullOrBlank()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(Icons.Filled.Alarm, contentDescription = null, tint = mutedFg, modifier = Modifier.size(13.dp))
            Text(
              text = stringResource(R.string.next_alarm, liveTiles.nextAlarmFormatted),
              color = mutedFg,
              style = MaterialTheme.typography.labelSmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
      if (showLabels) {
        Text(
          text = label,
          color = fgColor,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Normal,
          maxLines = 1,
          modifier = Modifier.align(Alignment.BottomStart),
        )
      }
    }
  }
}

@Composable
fun BatteryTileFace(
  tile: StartTile,
  liveTiles: LiveTilesState,
  label: String,
  showLabels: Boolean,
  fgColor: Color,
  mutedFg: Color,
) {
  val isSmall = tile.size == TileSize.SMALL
  val pctText = liveTiles.batteryPercent?.let { "$it%" } ?: "--"
  Box(modifier = Modifier.fillMaxSize().padding(if (isSmall) 4.dp else 10.dp)) {
    if (isSmall) {
      Text(
        text = if (tile.isLiveEnabled) pctText else "--",
        color = fgColor,
        fontSize = 14.sp,
        fontWeight = FontWeight.Light,
        modifier = Modifier.align(Alignment.Center),
      )
    } else {
      Column(
        modifier = Modifier.align(Alignment.CenterStart).padding(bottom = if (showLabels) 14.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = if (liveTiles.isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryStd,
            contentDescription = null,
            tint = fgColor,
            modifier = Modifier.size(26.dp),
          )
          Text(
            text = if (tile.isLiveEnabled) pctText else label,
            color = fgColor,
            fontSize = if (tile.isLiveEnabled) 26.sp else 18.sp,
            fontWeight = FontWeight.Light,
            maxLines = 1,
          )
        }
        if (tile.isLiveEnabled && liveTiles.batteryPercent != null && tile.size == TileSize.WIDE) {
          val stateText =
            if (liveTiles.isCharging) stringResource(R.string.battery_charging, liveTiles.batteryPercent)
            else stringResource(R.string.battery_on_battery, liveTiles.batteryPercent)
          Text(
            text = stateText,
            color = mutedFg,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      if (showLabels) {
        Text(
          text = label,
          color = fgColor,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Normal,
          maxLines = 1,
          modifier = Modifier.align(Alignment.BottomStart),
        )
      }
    }
  }
}

@Composable
fun CalendarTileFace(
  tile: StartTile,
  liveTiles: LiveTilesState,
  label: String,
  showLabels: Boolean,
  fgColor: Color,
  mutedFg: Color,
) {
  val isSmall = tile.size == TileSize.SMALL
  BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(if (isSmall) 4.dp else 10.dp)) {
    val compactHeight = maxHeight < 110.dp
    if (isSmall) {
      Text(
        text = if (tile.isLiveEnabled) liveTiles.dayNumber.ifBlank { "--" } else "--",
        color = fgColor,
        fontSize = 18.sp,
        fontWeight = FontWeight.Light,
        modifier = Modifier.align(Alignment.Center),
      )
    } else if (!tile.isLiveEnabled) {
      Column(modifier = Modifier.align(Alignment.CenterStart)) {
        Text(text = label, color = fgColor, style = MaterialTheme.typography.titleLarge)
        Text(text = stringResource(R.string.tile_mode_static), color = mutedFg, style = MaterialTheme.typography.labelMedium)
      }
    } else {
      val daySp =
        when {
          compactHeight -> 32.sp
          tile.size == TileSize.WIDE -> 48.sp
          else -> 34.sp
        }
      Column(
        modifier = Modifier.align(Alignment.CenterStart).padding(bottom = if (showLabels) 14.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = liveTiles.dayNumber.ifBlank { "--" },
            color = fgColor,
            fontSize = daySp,
            fontWeight = FontWeight.Light,
            lineHeight = daySp * 1.05f,
            maxLines = 1,
          )
          Column {
            Text(
              text = liveTiles.weekdayLabel,
              color = fgColor,
              style = MaterialTheme.typography.titleMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = liveTiles.monthLabel,
              color = mutedFg,
              style = MaterialTheme.typography.bodyMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        val eventLine =
          when {
            !liveTiles.nextCalendarEventTitle.isNullOrBlank() && !liveTiles.nextCalendarEventTime.isNullOrBlank() ->
              "${liveTiles.nextCalendarEventTime} • ${liveTiles.nextCalendarEventTitle}"
            !liveTiles.nextCalendarEventTime.isNullOrBlank() ->
              stringResource(R.string.calendar_event_scheduled, liveTiles.nextCalendarEventTime)
            else -> null
          }
        if (eventLine != null && tile.size == TileSize.WIDE) {
          Text(
            text = eventLine,
            color = fgColor,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      if (showLabels) {
        Text(
          text = label,
          color = fgColor,
          style = MaterialTheme.typography.labelMedium,
          maxLines = 1,
          modifier = Modifier.align(Alignment.BottomStart),
        )
      }
    }
  }
}

@Composable
fun PhotoTileFace(
  tile: StartTile,
  showHomePhotos: Boolean,
  showPhotoCaptions: Boolean,
  photoCropMode: PhotoCropMode,
  photoBitmap: ImageBitmap?,
  liveTiles: LiveTilesState,
  fgColor: Color,
  mutedFg: Color,
) {
  val isSmall = tile.size == TileSize.SMALL
  if (!showHomePhotos) {
    Box(
      modifier = Modifier.fillMaxSize().padding(if (isSmall) 4.dp else 10.dp).testTag("photos_hidden_state"),
    ) {
      Icon(
        imageVector = Icons.Filled.PhotoLibrary,
        contentDescription = null,
        tint = fgColor,
        modifier =
          Modifier.align(Alignment.Center)
            .padding(bottom = if (isSmall) 0.dp else 14.dp)
            .size(if (isSmall) 24.dp else 34.dp),
      )
      if (!isSmall) {
        Text(
          text = stringResource(R.string.photos_hidden_notice),
          color = mutedFg,
          style = MaterialTheme.typography.labelMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.align(Alignment.BottomStart),
        )
      }
    }
    return
  }

  if (photoBitmap != null && !liveTiles.isPhotoMissing) {
    val align =
      when (photoCropMode) {
        PhotoCropMode.CENTER -> Alignment.Center
        PhotoCropMode.TOP -> Alignment.TopCenter
        PhotoCropMode.BOTTOM -> Alignment.BottomCenter
      }
    Box(modifier = Modifier.fillMaxSize().testTag("photo_tile_bitmap_container")) {
      Image(
        bitmap = photoBitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = align,
        modifier = Modifier.fillMaxSize(),
      )
      if (!isSmall && showPhotoCaptions) {
        Box(
          modifier =
            Modifier.align(Alignment.BottomCenter)
              .fillMaxWidth()
              .height(44.dp)
              .background(
                Brush.verticalGradient(
                  colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.68f)),
                )
              )
        )
        Text(
          text = stringResource(R.string.photo),
          color = MetroWhite,
          style = MaterialTheme.typography.labelMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 10.dp, vertical = 8.dp),
        )
      }
    }
  } else {
    Box(modifier = Modifier.fillMaxSize().padding(if (isSmall) 4.dp else 10.dp)) {
      Icon(
        imageVector = Icons.Filled.PhotoLibrary,
        contentDescription = null,
        tint = fgColor,
        modifier =
          Modifier.align(Alignment.Center)
            .padding(bottom = if (isSmall) 0.dp else 14.dp)
            .size(if (isSmall) 24.dp else 34.dp),
      )
      if (!isSmall) {
        val caption =
          if (liveTiles.isPhotoMissing) stringResource(R.string.choose_another_photo)
          else stringResource(R.string.pick)
        Text(
          text = caption,
          color = fgColor,
          style = MaterialTheme.typography.labelMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.align(Alignment.BottomStart),
        )
      }
    }
  }
}

@Composable
fun MusicTileFace(
  tile: StartTile,
  liveTiles: LiveTilesState,
  label: String,
  showLabels: Boolean,
  fgColor: Color,
  mutedFg: Color,
  onPlayPause: () -> Unit,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
) {
  val media = liveTiles.mediaSession
  val isSmall = tile.size == TileSize.SMALL
  if (isSmall) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = fgColor, modifier = Modifier.size(26.dp))
    }
    return
  }

  if (!tile.isLiveEnabled || !media.isActive) {
    Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
      Row(
        modifier = Modifier.align(Alignment.CenterStart).padding(bottom = if (showLabels) 14.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = fgColor, modifier = Modifier.size(28.dp))
        Text(
          text = if (tile.isLiveEnabled) stringResource(R.string.music_idle) else stringResource(R.string.tile_mode_static),
          color = mutedFg,
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (showLabels) {
        Text(
          text = label,
          color = fgColor,
          style = MaterialTheme.typography.labelMedium,
          maxLines = 1,
          modifier = Modifier.align(Alignment.BottomStart),
        )
      }
    }
    return
  }

  Row(
    modifier = Modifier.fillMaxSize().padding(10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    val artSize = if (tile.size == TileSize.WIDE) 68.dp else 44.dp
    if (media.albumArt != null) {
      Image(
        bitmap = media.albumArt,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(artSize),
      )
    } else {
      Box(
        modifier = Modifier.size(artSize).background(fgColor.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = fgColor, modifier = Modifier.size(28.dp))
      }
    }

    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = media.playerLabel.ifBlank { label },
        color = mutedFg,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = media.trackTitle,
        color = fgColor,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (media.artist.isNotBlank()) {
        Text(
          text = media.artist,
          color = mutedFg,
          style = MaterialTheme.typography.labelSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      if (media.canSkipPrevious && tile.size == TileSize.WIDE) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp).testTag("music_prev_button")) {
          Icon(Icons.Filled.SkipPrevious, contentDescription = stringResource(R.string.music_previous), tint = fgColor)
        }
      }
      if (media.canPlayPause) {
        IconButton(onClick = onPlayPause, modifier = Modifier.size(48.dp).testTag("music_play_pause_button")) {
          Icon(
            imageVector = if (media.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = stringResource(if (media.isPlaying) R.string.music_pause else R.string.music_play),
            tint = fgColor,
          )
        }
      }
      if (media.canSkipNext && tile.size == TileSize.WIDE) {
        IconButton(onClick = onNext, modifier = Modifier.size(48.dp).testTag("music_next_button")) {
          Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.music_next), tint = fgColor)
        }
      }
    }
  }
}

@Composable
fun NotificationsTileFace(
  tile: StartTile,
  liveTiles: LiveTilesState,
  label: String,
  showLabels: Boolean,
  fgColor: Color,
  mutedFg: Color,
) {
  val notif = liveTiles.notifications
  val isSmall = tile.size == TileSize.SMALL
  if (isSmall) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      val badgeText = if (notif.hasPermission && tile.isLiveEnabled) "${notif.activeCount}" else "!"
      Text(text = badgeText, color = fgColor, fontSize = 18.sp, fontWeight = FontWeight.Light)
    }
    return
  }

  Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
    Column(
      modifier = Modifier.align(Alignment.CenterStart).padding(bottom = if (showLabels) 14.dp else 0.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Outlined.Notifications, contentDescription = null, tint = fgColor, modifier = Modifier.size(20.dp))
        Text(
          text =
            when {
              !tile.isLiveEnabled -> stringResource(R.string.tile_mode_static)
              !notif.hasPermission -> stringResource(R.string.notifications_permission_needed)
              notif.activeCount == 0 -> stringResource(R.string.no_active_notifications)
              else -> stringResource(R.string.active_notifications_count, notif.activeCount)
            },
          color = fgColor,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (tile.isLiveEnabled && notif.hasPermission && notif.activeCount > 0) {
        val previewLine =
          listOfNotNull(notif.latestAppName, notif.latestTitle).joinToString(": ")
        if (previewLine.isNotBlank()) {
          Text(
            text = previewLine,
            color = mutedFg,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
    if (showLabels) {
      Text(
        text = label,
        color = fgColor,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        modifier = Modifier.align(Alignment.BottomStart),
      )
    }
  }
}

@Composable
fun HubTileFace(
  tile: StartTile,
  hub: MetroHub?,
  showLabels: Boolean,
  fgColor: Color,
  mutedFg: Color,
) {
  val isSmall = tile.size == TileSize.SMALL
  val count = hub?.tiles?.size ?: 0
  Box(modifier = Modifier.fillMaxSize().padding(if (isSmall) 4.dp else 10.dp)) {
    Column(
      modifier = Modifier.align(Alignment.CenterStart).padding(bottom = if (!isSmall && showLabels) 14.dp else 0.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = MetroFourSquaresIcon,
        contentDescription = null,
        tint = fgColor,
        modifier = Modifier.size(if (isSmall) 22.dp else 28.dp),
      )
      if (!isSmall) {
        Text(
          text = "$count blocos",
          color = mutedFg,
          style = MaterialTheme.typography.labelSmall,
          maxLines = 1,
        )
      }
    }
    if (!isSmall && showLabels) {
      Text(
        text = hub?.name ?: tile.fallbackLabel,
        color = fgColor,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.align(Alignment.BottomStart),
      )
    }
  }
}

@Composable
fun SystemWidgetTileFace(
  tile: StartTile,
  appWidgetController: AppWidgetController?,
  fgColor: Color,
  mutedFg: Color,
) {
  val widgetId = tile.appWidgetId
  val hostView =
    remember(widgetId, appWidgetController) {
      if (widgetId != null && appWidgetController != null) {
        appWidgetController.createHostView(widgetId)
      } else {
        null
      }
    }

  if (hostView != null) {
    AndroidView(
      factory = { hostView },
      modifier = Modifier.fillMaxSize().background(MetroBlack),
    )
  } else {
    Box(modifier = Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
      Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(Icons.Outlined.Widgets, contentDescription = null, tint = fgColor, modifier = Modifier.size(26.dp))
        Text(
          text = stringResource(R.string.widget_unavailable),
          color = mutedFg,
          style = MaterialTheme.typography.labelMedium,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
