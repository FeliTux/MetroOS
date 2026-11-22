package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.LauncherUiState
import com.example.R
import com.example.data.LoadedAppIcon
import com.example.model.AppTargetKey
import com.example.model.MetroColorUtils
import com.example.model.MetroFolder
import com.example.model.MetroHub
import com.example.model.StartTile
import com.example.model.TileSize
import com.example.ui.components.ConfirmDestructiveDialog
import com.example.ui.components.MetroCommandBar
import com.example.ui.components.MetroFourSquaresOutlineIcon
import com.example.ui.components.MetroTileGridContent
import com.example.ui.theme.metroFocusIndicator
import com.example.widget.AppWidgetController

@Composable
fun HubScreen(
  hub: MetroHub,
  uiState: LauncherUiState,
  appWidgetController: AppWidgetController?,
  loadAppIcon: suspend (AppTargetKey) -> LoadedAppIcon,
  resolveTileLabel: (StartTile) -> String,
  onPhotoTargetEdgeMeasured: (Int) -> Unit,
  onTileTap: (StartTile) -> Unit,
  onTileLongPress: (StartTile) -> Unit,
  onOpenAccessibleMove: (StartTile) -> Unit,
  onOpenAccessibleResize: (StartTile) -> Unit,
  onUnpinAccessible: (StartTile) -> Unit,
  onBackToStart: () -> Unit,
  onAddToHub: () -> Unit,
  onEditClick: () -> Unit,
  onResizeClick: () -> Unit,
  onUnpinClick: () -> Unit,
  onDoneClick: () -> Unit,
  onSelectSize: (TileSize) -> Unit,
  onDismissResizeSheet: () -> Unit,
  onOpenMoveSheet: () -> Unit,
  onStepMove: (Int, Int) -> Unit,
  onMoveTileTo: (String, Int, Int) -> Unit,
  onDismissMoveSheet: () -> Unit,
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
  onCreateFolderClick: () -> Unit,
  onQuickNoteClick: () -> Unit,
  onSavedNotesClick: () -> Unit,
  onAddLiveTileClick: () -> Unit,
  onAddWidgetClick: () -> Unit,
  onOpenPersonalization: () -> Unit,
  onOpenProfile: () -> Unit,
  onRenameHub: () -> Unit,
  onChangeHubColor: () -> Unit,
  onPinHubToStart: () -> Unit,
  onDeleteHub: () -> Unit,
  onHandleEditBack: () -> Unit,
) {
  var showHubMenu by remember { mutableStateOf(false) }
  var showDeleteConfirm by remember { mutableStateOf(false) }

  val bgColor = MaterialTheme.colorScheme.background
  val fgOnBg = MetroColorUtils.legibleForeground(bgColor)
  val hubAccent = hub.accentHex?.let { Color(it) } ?: uiState.effectiveAccentColor
  val fgOnAccent = MetroColorUtils.legibleForeground(hubAccent)
  val menuBg = MaterialTheme.colorScheme.surfaceContainerHigh
  val menuFg = MaterialTheme.colorScheme.onSurface
  val errorFg = MaterialTheme.colorScheme.error

  BackHandler {
    when {
      uiState.isEditing -> onHandleEditBack()
      uiState.expandedFolderId != null -> onCollapseFolder()
      else -> onBackToStart()
    }
  }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(bgColor)
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(
        onClick = {
          if (uiState.isEditing) onHandleEditBack() else onBackToStart()
        },
        modifier =
          Modifier.size(48.dp)
            .metroFocusIndicator(hubAccent, fgOnBg)
            .testTag("hub_back_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = stringResource(R.string.back_to_start),
          tint = fgOnBg,
        )
      }
      Box(modifier = Modifier.size(10.dp, 28.dp).background(hubAccent))
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = hub.name,
        color = fgOnBg,
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.Light,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f).testTag("hub_title"),
      )
      Box {
        IconButton(
          onClick = { showHubMenu = true },
          modifier =
            Modifier.size(48.dp)
              .metroFocusIndicator(hubAccent, fgOnBg)
              .testTag("hub_overflow_button"),
        ) {
          Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options), tint = fgOnBg)
        }
        DropdownMenu(
          expanded = showHubMenu,
          onDismissRequest = { showHubMenu = false },
          modifier = Modifier.background(menuBg),
        ) {
          DropdownMenuItem(
            text = { Text(stringResource(R.string.rename_hub), color = menuFg) },
            onClick = {
              showHubMenu = false
              onRenameHub()
            },
            modifier = Modifier.testTag("hub_menu_rename"),
          )
          DropdownMenuItem(
            text = { Text(stringResource(R.string.hub_color), color = menuFg) },
            onClick = {
              showHubMenu = false
              onChangeHubColor()
            },
            modifier = Modifier.testTag("hub_menu_color"),
          )
          DropdownMenuItem(
            text = { Text(stringResource(R.string.pin_hub_to_start), color = menuFg) },
            onClick = {
              showHubMenu = false
              onPinHubToStart()
            },
            modifier = Modifier.testTag("hub_menu_pin_start"),
          )
          DropdownMenuItem(
            text = { Text(stringResource(R.string.delete_hub), color = errorFg) },
            onClick = {
              showHubMenu = false
              showDeleteConfirm = true
            },
            modifier = Modifier.testTag("hub_menu_delete"),
          )
        }
      }
    }

    MetroTileGridContent(
      uiState = uiState,
      accentForSurface = hubAccent,
      appWidgetController = appWidgetController,
      gridTestTag = "hub_tile_grid",
      loadAppIcon = loadAppIcon,
      resolveTileLabel = resolveTileLabel,
      onPhotoTargetEdgeMeasured = onPhotoTargetEdgeMeasured,
      onTileTap = onTileTap,
      onTileLongPress = onTileLongPress,
      onOpenAccessibleMove = onOpenAccessibleMove,
      onOpenAccessibleResize = onOpenAccessibleResize,
      onUnpinAccessible = onUnpinAccessible,
      onOpenMoveSheet = onOpenMoveSheet,
      onStepMove = onStepMove,
      onMoveTileTo = onMoveTileTo,
      onDismissMoveSheet = onDismissMoveSheet,
      onSelectSize = onSelectSize,
      onDismissResizeSheet = onDismissResizeSheet,
      onOpenTileColorSheet = onOpenTileColorSheet,
      onSelectTileColor = onSelectTileColor,
      onDismissTileColorSheet = onDismissTileColorSheet,
      onToggleOriginalIcon = onToggleOriginalIcon,
      onToggleLiveMode = onToggleLiveMode,
      onMediaPlayPause = onMediaPlayPause,
      onMediaPrevious = onMediaPrevious,
      onMediaNext = onMediaNext,
      onLocateHandled = onLocateHandled,
      onRestoreFocusHandled = onRestoreFocusHandled,
      onCollapseFolder = onCollapseFolder,
      onLaunchFolderApp = onLaunchFolderApp,
      onReorderFolderApp = onReorderFolderApp,
      onMoveFolderAppToStart = onMoveFolderAppToStart,
      onRemoveAppFromFolder = onRemoveAppFromFolder,
      onRenameFolder = onRenameFolder,
      onAddAppsToFolder = onAddAppsToFolder,
      onDissolveFolder = onDissolveFolder,
      emptyContent = {
        Column(
          modifier = Modifier.padding(32.dp).testTag("empty_hub_container"),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Icon(MetroFourSquaresOutlineIcon, contentDescription = null, tint = hubAccent, modifier = Modifier.size(44.dp))
          Text(
            text = stringResource(R.string.hub_empty),
            color = fgOnBg,
            style = MaterialTheme.typography.bodyLarge,
          )
          Button(
            onClick = onAddToHub,
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(containerColor = hubAccent, contentColor = fgOnAccent),
            modifier = Modifier.heightIn(min = 48.dp).testTag("empty_hub_add_button"),
          ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.add_to_hub))
          }
        }
      },
      modifier = Modifier.weight(1f),
    )

    MetroCommandBar(
      backgroundColor = bgColor,
      isEditing = uiState.isEditing,
      hasSelectedTile = uiState.selectedTileId != null,
      isSaving = uiState.isSaving,
      leftSlot = uiState.leftCommandSlot,
      isHubSurface = true,
      onBackToStartClick = onBackToStart,
      onAddToHubClick = onAddToHub,
      onLeftSlotClick = onBackToStart,
      onAppsClick = onAddToHub,
      onEditClick = onEditClick,
      onResizeClick = onResizeClick,
      onUnpinClick = onUnpinClick,
      onDoneClick = onDoneClick,
      onCreateFolderClick = onCreateFolderClick,
      onAddLiveTileClick = onAddLiveTileClick,
      onAddWidgetClick = onAddWidgetClick,
      onQuickNoteClick = onQuickNoteClick,
      onSavedNotesClick = onSavedNotesClick,
      onOpenPersonalizationClick = onOpenPersonalization,
      onOpenProfileClick = onOpenProfile,
      onOpenTileColorClick = onOpenTileColorSheet,
      onOpenMoveClick = onOpenMoveSheet,
      onChangeLeftSlot = {},
    )
  }

  if (showDeleteConfirm) {
    ConfirmDestructiveDialog(
      title = stringResource(R.string.delete_hub_confirm_title, hub.name),
      message = stringResource(R.string.delete_hub_confirm_msg),
      confirmLabel = stringResource(R.string.delete_hub),
      accentColor = hubAccent,
      onConfirm = {
        showDeleteConfirm = false
        onDeleteHub()
      },
      onDismiss = { showDeleteConfirm = false },
    )
  }
}
