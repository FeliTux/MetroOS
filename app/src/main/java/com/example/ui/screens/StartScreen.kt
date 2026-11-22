package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.LauncherUiState
import com.example.R
import com.example.data.LoadedAppIcon
import com.example.model.AppTargetKey
import com.example.model.LeftCommandSlot
import com.example.model.MetroColorUtils
import com.example.model.MetroFolder
import com.example.model.StartTile
import com.example.model.TileSize
import com.example.ui.components.MetroCommandBar
import com.example.ui.components.MetroFourSquaresOutlineIcon
import com.example.ui.components.MetroTileGridContent
import com.example.ui.theme.metroFocusIndicator
import com.example.widget.AppWidgetController

@Composable
fun StartScreen(
  uiState: LauncherUiState,
  appWidgetController: AppWidgetController?,
  loadAppIcon: suspend (AppTargetKey) -> LoadedAppIcon,
  resolveTileLabel: (StartTile) -> String,
  onSaveStartScrollPosition: (Int, Int) -> Unit,
  onPhotoTargetEdgeMeasured: (Int) -> Unit,
  onTileTap: (StartTile) -> Unit,
  onTileLongPress: (StartTile) -> Unit,
  onOpenAccessibleMove: (StartTile) -> Unit,
  onOpenAccessibleResize: (StartTile) -> Unit,
  onUnpinAccessible: (StartTile) -> Unit,
  onLeftCommandClick: () -> Unit,
  onAppsClick: () -> Unit,
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
  onSaveAndExitEdit: () -> Unit,
  onDiscardEdit: () -> Unit,
  onKeepEditing: () -> Unit,
  onDismissSaveError: () -> Unit,
  onRemoveStaleTile: (StartTile) -> Unit,
  onDismissStalePrompt: () -> Unit,
  onPickPhoto: () -> Unit,
  onDismissPhotoSheet: () -> Unit,
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
  onCreateHubClick: () -> Unit,
  onAddLiveTileClick: () -> Unit,
  onAddWidgetClick: () -> Unit,
  onQuickNoteClick: () -> Unit,
  onSavedNotesClick: () -> Unit,
  onOpenPersonalization: () -> Unit,
  onOpenProfile: () -> Unit,
  onChangeLeftSlot: (LeftCommandSlot) -> Unit,
) {
  BackHandler(enabled = !uiState.isEditing && uiState.expandedFolderId != null) {
    onCollapseFolder()
  }

  val themeBg = MaterialTheme.colorScheme.background
  val onSurface = MaterialTheme.colorScheme.onSurface
  val muted = MaterialTheme.colorScheme.onSurfaceVariant
  val dialogBg = MaterialTheme.colorScheme.surfaceContainerHigh

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(themeBg)
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
  ) {
    val showProfileOnStart = uiState.profile.showOnStart
    MetroTileGridContent(
      uiState = uiState,
      accentForSurface = uiState.effectiveAccentColor,
      appWidgetController = appWidgetController,
      gridTestTag = "start_tile_grid",
      initialScrollIndex = uiState.startSavedScrollIndex,
      initialScrollOffset = uiState.startSavedScrollOffset,
      onSaveScrollPosition = onSaveStartScrollPosition,
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
      headerContent =
        if (showProfileOnStart) {
          {
            StartProfileHeaderStrip(
              name = uiState.profile.name,
              avatarBitmap = uiState.profileAvatarBitmap,
              accentColor = uiState.effectiveAccentColor,
              backgroundColor = themeBg,
              onClick = onOpenProfile,
            )
          }
        } else {
          null
        },
      emptyContent = {
        EmptyStartState(
          accentColor = uiState.effectiveAccentColor,
          backgroundColor = themeBg,
          onOpenApps = onAppsClick,
        )
      },
      modifier = Modifier.weight(1f),
    )

    MetroCommandBar(
      backgroundColor = themeBg,
      isEditing = uiState.isEditing,
      hasSelectedTile = uiState.selectedTileId != null,
      isSaving = uiState.isSaving,
      leftSlot = uiState.leftCommandSlot,
      isHubSurface = false,
      onLeftSlotClick = onLeftCommandClick,
      onAppsClick = onAppsClick,
      onEditClick = onEditClick,
      onResizeClick = onResizeClick,
      onUnpinClick = onUnpinClick,
      onDoneClick = onDoneClick,
      onCreateFolderClick = onCreateFolderClick,
      onCreateHubClick = onCreateHubClick,
      onAddLiveTileClick = onAddLiveTileClick,
      onAddWidgetClick = onAddWidgetClick,
      onQuickNoteClick = onQuickNoteClick,
      onSavedNotesClick = onSavedNotesClick,
      onOpenPersonalizationClick = onOpenPersonalization,
      onOpenProfileClick = onOpenProfile,
      onOpenTileColorClick = onOpenTileColorSheet,
      onOpenMoveClick = onOpenMoveSheet,
      onChangeLeftSlot = onChangeLeftSlot,
    )
  }

  if (uiState.showUnsavedDialog) {
    AlertDialog(
      onDismissRequest = onKeepEditing,
      shape = RectangleShape,
      containerColor = dialogBg,
      titleContentColor = onSurface,
      textContentColor = muted,
      title = { Text(stringResource(R.string.unsaved)) },
      text = { Text(stringResource(R.string.unsaved_message)) },
      confirmButton = {
        Button(
          onClick = onSaveAndExitEdit,
          shape = RectangleShape,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = uiState.effectiveAccentColor,
              contentColor = MetroColorUtils.legibleForeground(uiState.effectiveAccentColor),
            ),
          modifier = Modifier.heightIn(min = 48.dp).testTag("unsaved_save_button"),
        ) {
          Text(stringResource(R.string.save))
        }
      },
      dismissButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          TextButton(onClick = onDiscardEdit, shape = RectangleShape, modifier = Modifier.heightIn(min = 48.dp).testTag("unsaved_discard_button")) {
            Text(stringResource(R.string.discard), color = onSurface)
          }
          TextButton(onClick = onKeepEditing, shape = RectangleShape, modifier = Modifier.heightIn(min = 48.dp).testTag("unsaved_keep_editing_button")) {
            Text(stringResource(R.string.keep_editing), color = onSurface)
          }
        }
      },
    )
  }

  val saveError = uiState.saveErrorMessage
  if (saveError != null) {
    AlertDialog(
      onDismissRequest = onDismissSaveError,
      shape = RectangleShape,
      containerColor = dialogBg,
      titleContentColor = onSurface,
      textContentColor = muted,
      title = { Text(stringResource(R.string.save_failed)) },
      text = { Text(saveError) },
      confirmButton = {
        Button(
          onClick = onDoneClick,
          shape = RectangleShape,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = uiState.effectiveAccentColor,
              contentColor = MetroColorUtils.legibleForeground(uiState.effectiveAccentColor),
            ),
          modifier = Modifier.heightIn(min = 48.dp).testTag("save_retry_button"),
        ) {
          Text(stringResource(R.string.retry))
        }
      },
      dismissButton = {
        TextButton(onClick = onDismissSaveError, shape = RectangleShape, modifier = Modifier.heightIn(min = 48.dp)) {
          Text(stringResource(R.string.cancel), color = onSurface)
        }
      },
    )
  }

  val staleTile = uiState.staleTilePrompt
  if (staleTile != null) {
    AlertDialog(
      onDismissRequest = onDismissStalePrompt,
      shape = RectangleShape,
      containerColor = dialogBg,
      titleContentColor = onSurface,
      textContentColor = muted,
      title = { Text(stringResource(R.string.unavailable)) },
      text = { Text(stringResource(R.string.unavailable_message)) },
      confirmButton = {
        Button(
          onClick = { onRemoveStaleTile(staleTile) },
          shape = RectangleShape,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = uiState.effectiveAccentColor,
              contentColor = MetroColorUtils.legibleForeground(uiState.effectiveAccentColor),
            ),
          modifier = Modifier.heightIn(min = 48.dp).testTag("remove_stale_button"),
        ) {
          Text(stringResource(R.string.unpin))
        }
      },
      dismissButton = {
        TextButton(onClick = onDismissStalePrompt, shape = RectangleShape, modifier = Modifier.heightIn(min = 48.dp)) {
          Text(stringResource(R.string.cancel), color = onSurface)
        }
      },
    )
  }

  if (uiState.showPhotoActionSheet) {
    AlertDialog(
      onDismissRequest = onDismissPhotoSheet,
      shape = RectangleShape,
      containerColor = dialogBg,
      titleContentColor = onSurface,
      textContentColor = muted,
      title = { Text(stringResource(R.string.photo)) },
      confirmButton = {
        Button(
          onClick = onPickPhoto,
          shape = RectangleShape,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = uiState.effectiveAccentColor,
              contentColor = MetroColorUtils.legibleForeground(uiState.effectiveAccentColor),
            ),
          modifier = Modifier.heightIn(min = 48.dp).testTag("replace_photo_button"),
        ) {
          Text(stringResource(R.string.replace))
        }
      },
      dismissButton = {
        TextButton(onClick = onDismissPhotoSheet, shape = RectangleShape, modifier = Modifier.heightIn(min = 48.dp)) {
          Text(stringResource(R.string.cancel), color = onSurface)
        }
      },
    )
  }
}

@Composable
private fun StartProfileHeaderStrip(
  name: String,
  avatarBitmap: ImageBitmap?,
  accentColor: Color,
  backgroundColor: Color,
  onClick: () -> Unit,
) {
  val fg = MetroColorUtils.legibleForeground(backgroundColor)
  val displayName = name.trim().ifEmpty { stringResource(R.string.settings_section_profile) }
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .heightIn(min = 52.dp)
        .metroFocusIndicator(accentColor, fg)
        .clickable(role = Role.Button, onClick = onClick)
        .semantics { contentDescription = displayName }
        .padding(vertical = 4.dp)
        .testTag("start_profile_header"),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier =
        Modifier.size(40.dp)
          .background(accentColor)
          .border(1.dp, MaterialTheme.colorScheme.outline),
      contentAlignment = Alignment.Center,
    ) {
      if (avatarBitmap != null) {
        Image(
          bitmap = avatarBitmap,
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        Icon(
          imageVector = Icons.Outlined.Person,
          contentDescription = null,
          tint = MetroColorUtils.legibleForeground(accentColor),
          modifier = Modifier.size(22.dp),
        )
      }
    }
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text = displayName,
      color = fg,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Light,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun EmptyStartState(
  accentColor: Color,
  backgroundColor: Color,
  onOpenApps: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val fg = MetroColorUtils.legibleForeground(backgroundColor)
  val onAccent = MetroColorUtils.legibleForeground(accentColor)
  Column(
    modifier = modifier.padding(32.dp).testTag("empty_start_container"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Icon(
      imageVector = MetroFourSquaresOutlineIcon,
      contentDescription = null,
      tint = accentColor,
      modifier = Modifier.size(48.dp),
    )
    Text(
      text = stringResource(R.string.empty_start),
      color = fg,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Light,
    )
    Button(
      onClick = onOpenApps,
      shape = RectangleShape,
      colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = onAccent),
      modifier = Modifier.heightIn(min = 48.dp).testTag("empty_start_apps_button"),
    ) {
      Text(stringResource(R.string.apps))
    }
  }
}
