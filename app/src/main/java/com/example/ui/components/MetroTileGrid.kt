package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.LauncherUiState
import com.example.R
import com.example.data.LoadedAppIcon
import com.example.layout.MetroGridGeometry
import com.example.layout.TileBand
import com.example.model.AppTargetKey
import com.example.model.InstalledApp
import com.example.model.MetroColorUtils
import com.example.model.MetroFolder
import com.example.model.StartTile
import com.example.model.TileKind
import com.example.model.TileSize
import com.example.ui.theme.MetroBlack
import com.example.ui.theme.MetroWhite
import com.example.ui.theme.metroFocusIndicator
import com.example.widget.AppWidgetController
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun MetroTileGridContent(
  uiState: LauncherUiState,
  accentForSurface: Color,
  appWidgetController: AppWidgetController?,
  gridTestTag: String,
  initialScrollIndex: Int = 0,
  initialScrollOffset: Int = 0,
  onSaveScrollPosition: (Int, Int) -> Unit = { _, _ -> },
  loadAppIcon: suspend (AppTargetKey) -> LoadedAppIcon,
  resolveTileLabel: (StartTile) -> String,
  onPhotoTargetEdgeMeasured: (Int) -> Unit,
  onTileTap: (StartTile) -> Unit,
  onTileLongPress: (StartTile) -> Unit,
  onOpenMoveSheet: () -> Unit,
  onStepMove: (Int, Int) -> Unit,
  onMoveTileTo: (String, Int, Int) -> Unit,
  onDismissMoveSheet: () -> Unit,
  onSelectSize: (TileSize) -> Unit,
  onDismissResizeSheet: () -> Unit,
  onOpenTileColorSheet: () -> Unit,
  onSelectTileColor: (Long?) -> Unit,
  onDismissTileColorSheet: () -> Unit,
  onToggleOriginalIcon: () -> Unit,
  onToggleLiveMode: () -> Unit,
  onMediaPlayPause: () -> Unit,
  onMediaPrevious: () -> Unit,
  onMediaNext: () -> Unit,
  onLocateHandled: () -> Unit,
  onRestoreFocusHandled: () -> Unit,
  onCollapseFolder: () -> Unit,
  onLaunchFolderApp: (AppTargetKey) -> Unit,
  onReorderFolderApp: (String, Int, Int) -> Unit,
  onMoveFolderAppToStart: (String, AppTargetKey) -> Unit,
  onRemoveAppFromFolder: (String, AppTargetKey) -> Unit,
  onRenameFolder: (MetroFolder) -> Unit,
  onAddAppsToFolder: (MetroFolder) -> Unit,
  onDissolveFolder: (String) -> Unit,
  onOpenAccessibleMove: (StartTile) -> Unit = {},
  onOpenAccessibleResize: (StartTile) -> Unit = {},
  onUnpinAccessible: (StartTile) -> Unit = {},
  headerContent: (@Composable () -> Unit)? = null,
  emptyContent: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  val listState: LazyListState =
    rememberLazyListState(
      initialFirstVisibleItemIndex = initialScrollIndex,
      initialFirstVisibleItemScrollOffset = initialScrollOffset,
    )
  val bands = remember(uiState.visibleTiles) { MetroGridGeometry.partitionIntoBands(uiState.visibleTiles) }

  DisposableEffect(listState) {
    onDispose {
      onSaveScrollPosition(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
    }
  }

  LaunchedEffect(uiState.scrollToTileId, bands) {
    val targetId = uiState.scrollToTileId ?: return@LaunchedEffect
    val bandOffset = if (headerContent != null) 1 else 0
    val bandIdx = bands.indexOfFirst { band -> band.tiles.any { it.id == targetId } }
    if (bandIdx >= 0) {
      listState.animateScrollToItem(bandIdx + bandOffset)
    }
    delay(1200)
    onLocateHandled()
  }

  LaunchedEffect(uiState.selectedTileId, uiState.isEditing) {
    val selectedId = uiState.selectedTileId ?: return@LaunchedEffect
    if (!uiState.isEditing) return@LaunchedEffect
    val bandOffset = if (headerContent != null) 1 else 0
    val bandIdx = bands.indexOfFirst { band -> band.tiles.any { it.id == selectedId } }
    if (bandIdx >= 0) {
      listState.animateScrollToItem(bandIdx + bandOffset)
    }
  }

  Box(modifier = modifier.fillMaxWidth()) {
    if (!uiState.isLoading && uiState.visibleTiles.isEmpty() && !uiState.isEditing) {
      Column(modifier = Modifier.fillMaxSize()) {
        headerContent?.invoke()
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
          emptyContent()
        }
      }
    } else {
      BoxWithConstraints(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)
      ) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth
        val gutterPx = with(density) { uiState.gutterDp.dp.roundToPx() }
        val unitHeightPx = remember(widthPx, gutterPx) {
          MetroGridGeometry.computeUnitHeightPx(widthPx, gutterPx)
        }
        val colStarts = remember(widthPx, gutterPx) {
          MetroGridGeometry.computeColumnStarts(widthPx, gutterPx)
        }

        val photoTile = remember(uiState.visibleTiles) {
          uiState.visibleTiles.firstOrNull { it.kind == TileKind.PHOTO }
        }
        LaunchedEffect(photoTile, widthPx, gutterPx, unitHeightPx) {
          if (photoTile != null) {
            val b =
              MetroGridGeometry.computeTileBounds(
                widthPx = widthPx,
                gutterPx = gutterPx,
                unitHeightPx = unitHeightPx,
                tile = photoTile,
              )
            onPhotoTargetEdgeMeasured(maxOf(b.widthPx, b.heightPx))
          }
        }

        val expandedFolder =
          remember(uiState.expandedFolderId, uiState.visibleFolders) {
            uiState.visibleFolders.firstOrNull { it.id == uiState.expandedFolderId }
          }

        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize().testTag(gridTestTag),
          contentPadding = PaddingValues(top = 8.dp, bottom = if (uiState.isEditing) 168.dp else 28.dp),
          verticalArrangement = Arrangement.spacedBy(with(density) { gutterPx.toDp() }),
        ) {
          if (headerContent != null) {
            item(key = "grid_header_profile") {
              headerContent()
            }
          }

          bands.forEach { band ->
            item(key = "band_${band.startRow}_${band.tiles.firstOrNull()?.id.orEmpty()}") {
              TileBandLayout(
                band = band,
                widthPx = widthPx,
                gutterPx = gutterPx,
                unitHeightPx = unitHeightPx,
                colStarts = colStarts,
                accentForSurface = accentForSurface,
                uiState = uiState,
                appWidgetController = appWidgetController,
                loadAppIcon = loadAppIcon,
                resolveTileLabel = resolveTileLabel,
                onTileTap = onTileTap,
                onTileLongPress = onTileLongPress,
                onOpenMoveSheet = onOpenMoveSheet,
                onOpenAccessibleMove = onOpenAccessibleMove,
                onOpenAccessibleResize = onOpenAccessibleResize,
                onUnpinAccessible = onUnpinAccessible,
                onMoveTileTo = onMoveTileTo,
                onMediaPlayPause = onMediaPlayPause,
                onMediaPrevious = onMediaPrevious,
                onMediaNext = onMediaNext,
                onRestoreFocusHandled = onRestoreFocusHandled,
              )
            }

            if (
              !uiState.isEditing &&
                expandedFolder != null &&
                band.tiles.any { it.folderId == expandedFolder.id }
            ) {
              item(key = "expanded_folder_${expandedFolder.id}") {
                val folderTile = band.tiles.first { it.folderId == expandedFolder.id }
                val folderColor = folderTile.customColorHex?.let { Color(it) } ?: accentForSurface
                ExpandedFolderPanel(
                  folder = expandedFolder,
                  folderColor = folderColor,
                  installedApps = uiState.installedApps,
                  iconCacheGeneration = uiState.iconCacheGeneration,
                  loadAppIcon = loadAppIcon,
                  onLaunchApp = onLaunchFolderApp,
                  onReorderApp = { idx, delta -> onReorderFolderApp(expandedFolder.id, idx, delta) },
                  onMoveToStart = { key -> onMoveFolderAppToStart(expandedFolder.id, key) },
                  onRemoveFromFolder = { key -> onRemoveAppFromFolder(expandedFolder.id, key) },
                  onRename = { onRenameFolder(expandedFolder) },
                  onAddApps = { onAddAppsToFolder(expandedFolder) },
                  onDissolve = { onDissolveFolder(expandedFolder.id) },
                  onClose = onCollapseFolder,
                )
              }
            }
          }
        }
      }
    }

    if (
      uiState.isEditing &&
        uiState.selectedTile != null &&
        !uiState.showResizeSheet &&
        !uiState.showMoveSheet &&
        !uiState.showTileColorSheet
    ) {
      val selected = uiState.selectedTile!!
      val selectedFolder =
        selected.folderId?.let { fid -> uiState.visibleFolders.firstOrNull { it.id == fid } }
      SelectedTileQuickBar(
        selectedTile = selected,
        selectedFolder = selectedFolder,
        themeThirdPartyIcons = uiState.themeThirdPartyIcons,
        accentColor = accentForSurface,
        onMoveClick = onOpenMoveSheet,
        onColorClick = onOpenTileColorSheet,
        onToggleOriginalIcon = onToggleOriginalIcon,
        onToggleLiveMode = onToggleLiveMode,
        onRenameFolder = { selectedFolder?.let(onRenameFolder) },
        onDissolveFolder = { selectedFolder?.let { onDissolveFolder(it.id) } },
        modifier = Modifier.align(Alignment.BottomCenter),
      )
    }

    if (uiState.isEditing && uiState.showResizeSheet && uiState.selectedTile != null) {
      ResizeTileSheet(
        tile = uiState.selectedTile!!,
        accentColor = accentForSurface,
        onSelectSize = onSelectSize,
        onDismiss = onDismissResizeSheet,
        modifier = Modifier.align(Alignment.BottomCenter),
      )
    }

    if (uiState.isEditing && uiState.showMoveSheet && uiState.selectedTile != null) {
      MoveTileSheet(
        tile = uiState.selectedTile!!,
        accentColor = accentForSurface,
        onStepMove = onStepMove,
        onDone = onDismissMoveSheet,
        modifier = Modifier.align(Alignment.BottomCenter),
      )
    }

    if (uiState.isEditing && uiState.showTileColorSheet && uiState.selectedTile != null) {
      TileColorSheet(
        currentCustomColorHex = uiState.selectedTile!!.customColorHex,
        globalAccentColor = accentForSurface,
        onSelectColor = onSelectTileColor,
        onDismiss = onDismissTileColorSheet,
        modifier = Modifier.align(Alignment.BottomCenter),
      )
    }
  }
}

@Composable
private fun TileBandLayout(
  band: TileBand,
  widthPx: Int,
  gutterPx: Int,
  unitHeightPx: Int,
  colStarts: IntArray,
  accentForSurface: Color,
  uiState: LauncherUiState,
  appWidgetController: AppWidgetController?,
  loadAppIcon: suspend (AppTargetKey) -> LoadedAppIcon,
  resolveTileLabel: (StartTile) -> String,
  onTileTap: (StartTile) -> Unit,
  onTileLongPress: (StartTile) -> Unit,
  onOpenMoveSheet: () -> Unit,
  onOpenAccessibleMove: (StartTile) -> Unit,
  onOpenAccessibleResize: (StartTile) -> Unit,
  onUnpinAccessible: (StartTile) -> Unit,
  onMoveTileTo: (String, Int, Int) -> Unit,
  onMediaPlayPause: () -> Unit,
  onMediaPrevious: () -> Unit,
  onMediaNext: () -> Unit,
  onRestoreFocusHandled: () -> Unit,
) {
  val bandHeightPx = band.rowSpan * unitHeightPx + (band.rowSpan - 1).coerceAtLeast(0) * gutterPx

  Layout(
    content = {
      for (tile in band.tiles) {
        val bounds =
          MetroGridGeometry.computeTileBounds(
            widthPx = widthPx,
            gutterPx = gutterPx,
            unitHeightPx = unitHeightPx,
            tile = tile,
            rowOffset = band.startRow,
          )
        val folder = tile.folderId?.let { fid -> uiState.visibleFolders.firstOrNull { it.id == fid } }
        val note = tile.noteId?.let { nid -> uiState.visibleNotes.firstOrNull { it.id == nid } }
        val hub = tile.hubId?.let { hid -> uiState.committedLayout.hubs.firstOrNull { it.id == hid } }
        val appRecord = tile.target?.let { key -> uiState.installedApps.firstOrNull { it.key == key } }
        val tileColor =
          tile.customColorHex?.let { Color(it) }
            ?: hub?.accentHex?.let { Color(it) }
            ?: accentForSurface

        StartTileCard(
          tile = tile,
          tileLabel = resolveTileLabel(tile),
          tileColor = tileColor,
          uiState = uiState,
          appRecord = appRecord,
          folder = folder,
          note = note,
          hub = hub,
          appWidgetController = appWidgetController,
          loadAppIcon = loadAppIcon,
          onTap = { onTileTap(tile) },
          onLongPress = { onTileLongPress(tile) },
          onOpenMove = onOpenMoveSheet,
          onAccessibleMove = { onOpenAccessibleMove(tile) },
          onAccessibleResize = { onOpenAccessibleResize(tile) },
          onAccessibleUnpin = { onUnpinAccessible(tile) },
          onMediaPlayPause = onMediaPlayPause,
          onMediaPrevious = onMediaPrevious,
          onMediaNext = onMediaNext,
          onDragDropDelta = { dx, dy ->
            val targetCenterX = bounds.leftPx + dx + bounds.widthPx / 2f
            val targetTopY = bounds.topPx + dy
            val stepY = (unitHeightPx + gutterPx).coerceAtLeast(1)
            val newCol =
              colStarts.indices
                .minByOrNull { idx ->
                  kotlin.math.abs((colStarts[idx] + bounds.widthPx / 2f) - targetCenterX)
                }
                ?.coerceIn(0, MetroGridGeometry.GRID_COLUMNS - 1) ?: tile.col
            val rowDelta = (targetTopY / stepY).roundToInt()
            val newRow = (band.startRow + rowDelta).coerceAtLeast(0)
            onMoveTileTo(tile.id, newCol, newRow)
          },
          onFocusRestored = onRestoreFocusHandled,
        )
      }
    },
    modifier = Modifier.fillMaxWidth(),
  ) { measurables, _ ->
    val placeables =
      band.tiles.mapIndexed { index, tile ->
        val bounds =
          MetroGridGeometry.computeTileBounds(
            widthPx = widthPx,
            gutterPx = gutterPx,
            unitHeightPx = unitHeightPx,
            tile = tile,
            rowOffset = band.startRow,
          )
        val childConstraints =
          Constraints.fixed(
            width = bounds.widthPx.coerceAtLeast(1),
            height = bounds.heightPx.coerceAtLeast(1),
          )
        bounds to measurables[index].measure(childConstraints)
      }

    layout(widthPx, bandHeightPx.coerceAtLeast(1)) {
      placeables.forEach { (bounds, placeable) ->
        placeable.placeRelative(bounds.leftPx, bounds.topPx)
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StartTileCard(
  tile: StartTile,
  tileLabel: String,
  tileColor: Color,
  uiState: LauncherUiState,
  appRecord: InstalledApp?,
  folder: MetroFolder?,
  note: com.example.model.QuickNote?,
  hub: com.example.model.MetroHub?,
  appWidgetController: AppWidgetController?,
  loadAppIcon: suspend (AppTargetKey) -> LoadedAppIcon,
  onTap: () -> Unit,
  onLongPress: () -> Unit,
  onOpenMove: () -> Unit,
  onAccessibleMove: () -> Unit,
  onAccessibleResize: () -> Unit,
  onAccessibleUnpin: () -> Unit,
  onMediaPlayPause: () -> Unit,
  onMediaPrevious: () -> Unit,
  onMediaNext: () -> Unit,
  onDragDropDelta: (Float, Float) -> Unit,
  onFocusRestored: () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val focusRequester = remember { FocusRequester() }
  val isEditing = uiState.isEditing
  val isSelected = uiState.selectedTileId == tile.id
  val isHighlighted = uiState.highlightedTileId == tile.id
  val shouldRestoreFocus = uiState.restoreFocusTileId == tile.id
  val reduceMotion = uiState.effectiveReducedMotion
  val liveTiles = uiState.liveTiles
  val isLightBackground = MetroColorUtils.isLightColor(MaterialTheme.colorScheme.background)

  var dragX by remember(tile.id, isEditing) { mutableFloatStateOf(0f) }
  var dragY by remember(tile.id, isEditing) { mutableFloatStateOf(0f) }
  var isDragging by remember(tile.id, isEditing) { mutableStateOf(false) }

  LaunchedEffect(shouldRestoreFocus) {
    if (shouldRestoreFocus) {
      runCatching { focusRequester.requestFocus() }
      onFocusRestored()
    }
  }

  val fgColor = MetroColorUtils.legibleForeground(tileColor)
  val mutedFg = MetroColorUtils.mutedForeground(tileColor)

  val targetScale =
    when {
      isDragging -> 1.03f
      isPressed && !reduceMotion -> 0.96f
      isEditing && !isSelected -> 0.96f
      else -> 1f
    }
  val scale by animateFloatAsState(
    targetValue = targetScale,
    animationSpec = tween(durationMillis = if (reduceMotion) 0 else 90),
    label = "tile_scale",
  )

  val moveActionLabel = stringResource(R.string.a11y_action_move)
  val resizeActionLabel = stringResource(R.string.a11y_action_resize)
  val unpinActionLabel = stringResource(R.string.a11y_action_unpin)

  val selectionStrokeColor = MaterialTheme.colorScheme.onBackground
  val borderModifier =
    when {
      isSelected || isHighlighted -> Modifier.border(2.dp, selectionStrokeColor)
      isLightBackground -> Modifier.border(1.dp, MaterialTheme.colorScheme.outline)
      else -> Modifier
    }

  val folderCount = folder?.appKeys?.size ?: 0
  val folderAnnouncement =
    if (folderCount == 1) {
      stringResource(R.string.a11y_folder_single_app_announcement, folder?.name ?: tileLabel)
    } else {
      stringResource(R.string.a11y_folder_announcement, folder?.name ?: tileLabel, folderCount)
    }

  val accessibleName =
    when (tile.kind) {
      TileKind.CLOCK ->
        if (tile.isLiveEnabled && liveTiles.timeFormatted.isNotBlank()) "$tileLabel, ${liveTiles.timeFormatted}" else tileLabel
      TileKind.BATTERY ->
        if (tile.isLiveEnabled && liveTiles.batteryPercent != null) "$tileLabel, ${liveTiles.batteryPercent}%" else tileLabel
      TileKind.CALENDAR ->
        if (tile.isLiveEnabled) "$tileLabel, ${liveTiles.weekdayLabel} ${liveTiles.dayNumber} ${liveTiles.monthLabel}" else tileLabel
      TileKind.PHOTO -> tileLabel
      TileKind.MUSIC ->
        if (liveTiles.mediaSession.isActive) "$tileLabel, ${liveTiles.mediaSession.trackTitle}" else tileLabel
      TileKind.NOTIFICATIONS ->
        "$tileLabel, ${liveTiles.notifications.activeCount}"
      TileKind.HUB ->
        "${hub?.name ?: tileLabel}, ${hub?.tiles?.size ?: 0}"
      TileKind.SYSTEM_WIDGET -> tileLabel
      TileKind.FOLDER -> folderAnnouncement
      TileKind.NOTE ->
        "${note?.title?.ifBlank { tileLabel } ?: tileLabel}: ${note?.body.orEmpty()}"
      TileKind.APP_SHORTCUT ->
        when {
          tile.needsRepairChoice -> "$tileLabel, ${stringResource(R.string.choose_app)}"
          appRecord?.isProfilePaused == true -> "$tileLabel, ${stringResource(R.string.work_profile_paused)}"
          tile.isUnavailable -> "$tileLabel, ${stringResource(R.string.unavailable)}"
          else -> tileLabel
        }
    }

  val dragModifier =
    if (isEditing) {
      Modifier.pointerInput(tile.id, isEditing) {
        detectDragGesturesAfterLongPress(
          onDragStart = {
            isDragging = true
            onTap()
          },
          onDragEnd = {
            val dx = dragX
            val dy = dragY
            dragX = 0f
            dragY = 0f
            isDragging = false
            onDragDropDelta(dx, dy)
          },
          onDragCancel = {
            dragX = 0f
            dragY = 0f
            isDragging = false
          },
          onDrag = { change, dragAmount ->
            change.consume()
            dragX += dragAmount.x
            dragY += dragAmount.y
          },
        )
      }
    } else {
      Modifier
    }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .zIndex(if (isDragging || isSelected) 2f else 0f)
        .offset { IntOffset(dragX.roundToInt(), dragY.roundToInt()) }
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .background(tileColor)
        .then(borderModifier)
        .metroFocusIndicator(selectionStrokeColor, fgColor)
        .focusRequester(focusRequester)
        .then(dragModifier)
        .combinedClickable(
          interactionSource = interactionSource,
          indication = ripple(bounded = true, color = fgColor),
          role = Role.Button,
          onClick = onTap,
          onLongClick = onLongPress,
        )
        .semantics {
          contentDescription = accessibleName
          if (!uiState.lockLayout) {
            customActions =
              listOf(
                CustomAccessibilityAction(moveActionLabel) {
                  onAccessibleMove()
                  true
                },
                CustomAccessibilityAction(resizeActionLabel) {
                  onAccessibleResize()
                  true
                },
                CustomAccessibilityAction(unpinActionLabel) {
                  onAccessibleUnpin()
                  true
                },
              )
          }
        }
        .testTag("tile_${tile.id}")
  ) {
    when (tile.kind) {
      TileKind.PHOTO ->
        PhotoTileFace(
          tile = tile,
          showHomePhotos = uiState.showHomePhotos,
          showPhotoCaptions = uiState.showPhotoCaptions,
          photoCropMode = uiState.photoCropMode,
          photoBitmap = uiState.photoBitmap,
          liveTiles = liveTiles,
          fgColor = fgColor,
          mutedFg = mutedFg,
        )
      TileKind.CLOCK ->
        ClockTileFace(tile = tile, liveTiles = liveTiles, label = tileLabel, showLabels = uiState.showLabels, fgColor = fgColor, mutedFg = mutedFg)
      TileKind.BATTERY ->
        BatteryTileFace(tile = tile, liveTiles = liveTiles, label = tileLabel, showLabels = uiState.showLabels, fgColor = fgColor, mutedFg = mutedFg)
      TileKind.CALENDAR ->
        CalendarTileFace(tile = tile, liveTiles = liveTiles, label = tileLabel, showLabels = uiState.showLabels, fgColor = fgColor, mutedFg = mutedFg)
      TileKind.MUSIC ->
        MusicTileFace(
          tile = tile,
          liveTiles = liveTiles,
          label = tileLabel,
          showLabels = uiState.showLabels,
          fgColor = fgColor,
          mutedFg = mutedFg,
          onPlayPause = onMediaPlayPause,
          onPrevious = onMediaPrevious,
          onNext = onMediaNext,
        )
      TileKind.NOTIFICATIONS ->
        NotificationsTileFace(tile = tile, liveTiles = liveTiles, label = tileLabel, showLabels = uiState.showLabels, fgColor = fgColor, mutedFg = mutedFg)
      TileKind.HUB ->
        HubTileFace(tile = tile, hub = hub, showLabels = uiState.showLabels, fgColor = fgColor, mutedFg = mutedFg)
      TileKind.SYSTEM_WIDGET ->
        SystemWidgetTileFace(tile = tile, appWidgetController = appWidgetController, fgColor = fgColor, mutedFg = mutedFg)
      TileKind.FOLDER ->
        FolderTileFace(
          tile = tile,
          folder = folder,
          showLabels = uiState.showLabels,
          iconCacheGeneration = uiState.iconCacheGeneration,
          fgColor = fgColor,
          loadAppIcon = loadAppIcon,
        )
      TileKind.NOTE ->
        NoteTileFace(tile = tile, note = note, fgColor = fgColor, mutedFg = mutedFg)
      TileKind.APP_SHORTCUT ->
        AppShortcutTileFace(
          tile = tile,
          label = tileLabel,
          showLabels = uiState.showLabels,
          themeThirdPartyIcons = uiState.themeThirdPartyIcons,
          iconCacheGeneration = uiState.iconCacheGeneration,
          fgColor = fgColor,
          loadAppIcon = loadAppIcon,
        )
    }

    if (tile.needsRepairChoice) {
      Box(
        modifier =
          Modifier.align(Alignment.TopEnd)
            .padding(4.dp)
            .background(MetroBlack.copy(alpha = 0.78f))
            .padding(3.dp)
            .testTag("repair_badge_${tile.id}")
      ) {
        Icon(Icons.Filled.Build, contentDescription = stringResource(R.string.choose_app), tint = MetroWhite, modifier = Modifier.size(14.dp))
      }
    } else if (tile.isUnavailable || appRecord?.isProfilePaused == true) {
      Box(
        modifier =
          Modifier.align(Alignment.TopEnd)
            .padding(4.dp)
            .background(MetroBlack.copy(alpha = 0.7f))
            .padding(2.dp)
      ) {
        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MetroWhite, modifier = Modifier.size(16.dp))
      }
    } else if (appRecord?.isWorkProfile == true) {
      Box(
        modifier =
          Modifier.align(Alignment.TopEnd)
            .padding(4.dp)
            .background(MetroBlack.copy(alpha = 0.55f))
            .padding(2.dp)
      ) {
        Icon(Icons.Outlined.WorkOutline, contentDescription = stringResource(R.string.work_profile_badge), tint = MetroWhite, modifier = Modifier.size(12.dp))
      }
    }

    if (isEditing && isSelected) {
      IconButton(
        onClick = onOpenMove,
        modifier = Modifier.align(Alignment.TopEnd).size(48.dp).testTag("tile_move_action_${tile.id}"),
      ) {
        Box(
          modifier = Modifier.background(MetroBlack.copy(alpha = 0.75f)).padding(6.dp),
          contentAlignment = Alignment.Center,
        ) {
          Icon(Icons.Filled.OpenWith, contentDescription = moveActionLabel, tint = MetroWhite, modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ExpandedFolderPanel(
  folder: MetroFolder,
  folderColor: Color,
  installedApps: List<InstalledApp>,
  iconCacheGeneration: Long,
  loadAppIcon: suspend (AppTargetKey) -> LoadedAppIcon,
  onLaunchApp: (AppTargetKey) -> Unit,
  onReorderApp: (Int, Int) -> Unit,
  onMoveToStart: (AppTargetKey) -> Unit,
  onRemoveFromFolder: (AppTargetKey) -> Unit,
  onRename: () -> Unit,
  onAddApps: () -> Unit,
  onDissolve: () -> Unit,
  onClose: () -> Unit,
) {
  var menuForIndex by remember { mutableStateOf<Int?>(null) }
  val panelBg = MaterialTheme.colorScheme.surfaceContainer
  val panelFg = MaterialTheme.colorScheme.onSurface
  val panelMuted = MaterialTheme.colorScheme.onSurfaceVariant
  val menuBg = MaterialTheme.colorScheme.surfaceContainerHigh

  Surface(
    color = panelBg,
    contentColor = panelFg,
    border = BorderStroke(2.dp, folderColor),
    shape = RectangleShape,
    modifier = Modifier.fillMaxWidth().testTag("expanded_folder_panel_${folder.id}"),
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = folder.name,
          color = panelFg,
          style = MaterialTheme.typography.titleLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        IconButton(
          onClick = onClose,
          modifier =
            Modifier.size(48.dp)
              .metroFocusIndicator(folderColor, panelFg)
              .testTag("folder_close_button"),
        ) {
          Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close_folder), tint = panelFg)
        }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        TextButton(
          onClick = onRename,
          shape = RectangleShape,
          modifier = Modifier.heightIn(min = 48.dp).metroFocusIndicator(folderColor, panelFg).testTag("folder_rename_button"),
        ) {
          Text(stringResource(R.string.rename_folder), color = panelFg)
        }
        TextButton(
          onClick = onAddApps,
          shape = RectangleShape,
          modifier = Modifier.heightIn(min = 48.dp).metroFocusIndicator(folderColor, panelFg).testTag("folder_add_apps_button"),
        ) {
          Text(stringResource(R.string.add_apps_to_folder), color = panelFg)
        }
        TextButton(
          onClick = onDissolve,
          shape = RectangleShape,
          modifier = Modifier.heightIn(min = 48.dp).metroFocusIndicator(folderColor, panelFg).testTag("folder_dissolve_button"),
        ) {
          Text(stringResource(R.string.dissolve_folder), color = panelMuted)
        }
      }

      if (folder.appKeys.isEmpty()) {
        Text(
          text = stringResource(R.string.folder_empty),
          color = panelMuted,
          style = MaterialTheme.typography.bodyMedium,
        )
      } else {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          folder.appKeys.forEachIndexed { index, key ->
            val label = installedApps.firstOrNull { it.key == key }?.label ?: key.packageName
            val fg = MetroColorUtils.legibleForeground(folderColor)

            Box {
              Box(
                modifier =
                  Modifier.size(width = 104.dp, height = 98.dp)
                    .background(folderColor)
                    .metroFocusIndicator(panelFg, fg)
                    .combinedClickable(
                      role = Role.Button,
                      onClick = { onLaunchApp(key) },
                      onLongClick = { menuForIndex = index },
                    )
                    .semantics { contentDescription = label }
                    .padding(8.dp)
                    .testTag("folder_child_${key.stableKey}"),
              ) {
                Column(
                  modifier = Modifier.fillMaxSize(),
                  verticalArrangement = Arrangement.SpaceBetween,
                ) {
                  Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AppIconImage(
                      targetKey = key,
                      fallbackLabel = label,
                      iconCacheGeneration = iconCacheGeneration,
                      loadAppIcon = loadAppIcon,
                      iconSize = 36.dp,
                      foregroundFallbackColor = fg,
                      testTagPrefix = "folder_child_icon",
                    )
                  }
                  Text(
                    text = label,
                    color = fg,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }

                // Visible tap alternative to long-press for folder items
                IconButton(
                  onClick = { menuForIndex = index },
                  modifier = Modifier.align(Alignment.TopEnd).size(32.dp).testTag("folder_child_more_${key.stableKey}"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "${stringResource(R.string.more_options)}: $label",
                    tint = fg.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp),
                  )
                }
              }

              DropdownMenu(
                expanded = menuForIndex == index,
                onDismissRequest = { menuForIndex = null },
                modifier = Modifier.background(menuBg),
              ) {
                if (index > 0) {
                  DropdownMenuItem(
                    text = { Text(stringResource(R.string.move_earlier), color = panelFg) },
                    onClick = {
                      menuForIndex = null
                      onReorderApp(index, -1)
                    },
                  )
                }
                if (index < folder.appKeys.lastIndex) {
                  DropdownMenuItem(
                    text = { Text(stringResource(R.string.move_later), color = panelFg) },
                    onClick = {
                      menuForIndex = null
                      onReorderApp(index, 1)
                    },
                  )
                }
                DropdownMenuItem(
                  text = { Text(stringResource(R.string.move_to_start), color = panelFg) },
                  onClick = {
                    menuForIndex = null
                    onMoveToStart(key)
                  },
                )
                DropdownMenuItem(
                  text = { Text(stringResource(R.string.remove_from_folder), color = panelFg) },
                  onClick = {
                    menuForIndex = null
                    onRemoveFromFolder(key)
                  },
                )
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectedTileQuickBar(
  selectedTile: StartTile,
  selectedFolder: MetroFolder?,
  themeThirdPartyIcons: Boolean,
  accentColor: Color,
  onMoveClick: () -> Unit,
  onColorClick: () -> Unit,
  onToggleOriginalIcon: () -> Unit,
  onToggleLiveMode: () -> Unit,
  onRenameFolder: () -> Unit,
  onDissolveFolder: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val supportsLiveToggle =
    selectedTile.kind in
      setOf(
        TileKind.CLOCK,
        TileKind.BATTERY,
        TileKind.CALENDAR,
        TileKind.PHOTO,
        TileKind.MUSIC,
        TileKind.NOTIFICATIONS,
      )

  val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
  val onSurface = MaterialTheme.colorScheme.onSurface
  val muted = MaterialTheme.colorScheme.onSurfaceVariant
  val outline = MaterialTheme.colorScheme.outline

  Surface(
    color = surfaceColor,
    contentColor = onSurface,
    border = BorderStroke(1.dp, outline),
    shape = RectangleShape,
    modifier = modifier.fillMaxWidth().testTag("selected_tile_quick_bar"),
  ) {
    FlowRow(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      OutlinedButton(
        onClick = onMoveClick,
        shape = RectangleShape,
        modifier = Modifier.heightIn(min = 48.dp).metroFocusIndicator(accentColor, onSurface).testTag("quick_move_button"),
      ) {
        Icon(Icons.Filled.OpenWith, contentDescription = null, tint = onSurface, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(stringResource(R.string.move), color = onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      OutlinedButton(
        onClick = onColorClick,
        shape = RectangleShape,
        modifier = Modifier.heightIn(min = 48.dp).metroFocusIndicator(accentColor, onSurface).testTag("quick_color_button"),
      ) {
        Icon(Icons.Filled.Palette, contentDescription = null, tint = onSurface, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(stringResource(R.string.tile_color), color = onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      if (selectedTile.kind == TileKind.APP_SHORTCUT && themeThirdPartyIcons) {
        val originalOn = selectedTile.useOriginalIcon
        OutlinedButton(
          onClick = onToggleOriginalIcon,
          shape = RectangleShape,
          colors =
            ButtonDefaults.outlinedButtonColors(
              containerColor = if (originalOn) accentColor.copy(alpha = 0.25f) else Color.Transparent,
            ),
          modifier = Modifier.heightIn(min = 48.dp).testTag("quick_toggle_original_icon"),
        ) {
          Text(
            text = stringResource(R.string.use_original_icon),
            color = onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      if (supportsLiveToggle) {
        val isLive = selectedTile.isLiveEnabled
        OutlinedButton(
          onClick = onToggleLiveMode,
          shape = RectangleShape,
          colors =
            ButtonDefaults.outlinedButtonColors(
              containerColor = if (isLive) accentColor.copy(alpha = 0.25f) else Color.Transparent,
            ),
          modifier = Modifier.heightIn(min = 48.dp).testTag("quick_toggle_live_mode"),
        ) {
          Text(
            text = stringResource(if (isLive) R.string.tile_mode_live else R.string.tile_mode_static),
            color = onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      if (selectedTile.kind == TileKind.FOLDER && selectedFolder != null) {
        TextButton(onClick = onRenameFolder, shape = RectangleShape, modifier = Modifier.heightIn(min = 48.dp)) {
          Text(stringResource(R.string.rename_folder), color = accentColor)
        }
        TextButton(onClick = onDissolveFolder, shape = RectangleShape, modifier = Modifier.heightIn(min = 48.dp)) {
          Text(stringResource(R.string.dissolve_folder), color = muted)
        }
      }
    }
  }
}

@Composable
fun ResizeTileSheet(
  tile: StartTile,
  accentColor: Color,
  onSelectSize: (TileSize) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val currentSize = tile.size
  val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
  val onSurface = MaterialTheme.colorScheme.onSurface
  val muted = MaterialTheme.colorScheme.onSurfaceVariant
  val outline = MaterialTheme.colorScheme.outline

  Surface(
    color = surfaceColor,
    contentColor = onSurface,
    border = BorderStroke(1.dp, outline),
    shape = RectangleShape,
    modifier = modifier.fillMaxWidth().testTag("resize_sheet"),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(text = stringResource(R.string.resize), color = onSurface, style = MaterialTheme.typography.titleMedium)
      HorizontalDivider(color = outline)
      val choices =
        if (tile.kind == TileKind.NOTE) {
          listOf(TileSize.MEDIUM to stringResource(R.string.medium), TileSize.WIDE to stringResource(R.string.wide))
        } else {
          listOf(
            TileSize.SMALL to stringResource(R.string.small),
            TileSize.MEDIUM to stringResource(R.string.medium),
            TileSize.WIDE to stringResource(R.string.wide),
          )
        }
      choices.forEach { (size, label) ->
        val selected = size == currentSize
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .heightIn(min = 48.dp)
              .background(if (selected) accentColor.copy(alpha = 0.25f) else Color.Transparent)
              .metroFocusIndicator(accentColor, onSurface)
              .clickable(role = Role.RadioButton) { onSelectSize(size) }
              .padding(horizontal = 12.dp, vertical = 10.dp)
              .testTag("resize_option_${size.name.lowercase()}"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(text = label, color = onSurface, style = MaterialTheme.typography.bodyLarge)
          if (selected) {
            Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = onSurface)
          }
        }
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onDismiss, shape = RectangleShape, modifier = Modifier.heightIn(min = 48.dp)) {
          Text(stringResource(R.string.cancel), color = muted)
        }
      }
    }
  }
}

@Composable
fun MoveTileSheet(
  tile: StartTile,
  accentColor: Color,
  onStepMove: (Int, Int) -> Unit,
  onDone: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val positionDescription = stringResource(R.string.tile_position, tile.col + 1, tile.row + 1)
  val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
  val onSurface = MaterialTheme.colorScheme.onSurface
  val muted = MaterialTheme.colorScheme.onSurfaceVariant
  val outline = MaterialTheme.colorScheme.outline

  Surface(
    color = surfaceColor,
    contentColor = onSurface,
    border = BorderStroke(1.dp, outline),
    shape = RectangleShape,
    modifier = modifier.fillMaxWidth().testTag("move_sheet"),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(text = stringResource(R.string.move), color = onSurface, style = MaterialTheme.typography.titleMedium)
        Text(
          text = positionDescription,
          color = muted,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
      }
      HorizontalDivider(color = outline)
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        OutlinedButton(
          onClick = { onStepMove(-1, 0) },
          enabled = tile.col > 0,
          shape = RectangleShape,
          modifier = Modifier.heightIn(min = 48.dp).testTag("move_left_button"),
        ) {
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.left), tint = onSurface)
        }
        OutlinedButton(
          onClick = { onStepMove(0, -1) },
          enabled = tile.row > 0,
          shape = RectangleShape,
          modifier = Modifier.heightIn(min = 48.dp).testTag("move_up_button"),
        ) {
          Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.up), tint = onSurface)
        }
        OutlinedButton(
          onClick = { onStepMove(0, 1) },
          shape = RectangleShape,
          modifier = Modifier.heightIn(min = 48.dp).testTag("move_down_button"),
        ) {
          Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.down), tint = onSurface)
        }
        OutlinedButton(
          onClick = { onStepMove(1, 0) },
          enabled = tile.col + tile.colSpan < MetroGridGeometry.GRID_COLUMNS,
          shape = RectangleShape,
          modifier = Modifier.heightIn(min = 48.dp).testTag("move_right_button"),
        ) {
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.right), tint = onSurface)
        }
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Button(
          onClick = onDone,
          shape = RectangleShape,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = accentColor,
              contentColor = MetroColorUtils.legibleForeground(accentColor),
            ),
          modifier = Modifier.heightIn(min = 48.dp).testTag("move_done_button"),
        ) {
          Text(stringResource(R.string.done))
        }
      }
    }
  }
}
