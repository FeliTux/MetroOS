package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.LeftCommandSlot
import com.example.model.MetroColorUtils
import com.example.ui.theme.metroFocusIndicator

@Composable
fun MetroCommandBar(
  backgroundColor: Color,
  isEditing: Boolean,
  hasSelectedTile: Boolean,
  isSaving: Boolean,
  leftSlot: LeftCommandSlot,
  isHubSurface: Boolean = false,
  onBackToStartClick: () -> Unit = {},
  onAddToHubClick: () -> Unit = {},
  onLeftSlotClick: () -> Unit,
  onAppsClick: () -> Unit,
  onEditClick: () -> Unit,
  onResizeClick: () -> Unit,
  onUnpinClick: () -> Unit,
  onDoneClick: () -> Unit,
  onCreateFolderClick: () -> Unit,
  onCreateHubClick: () -> Unit = {},
  onAddLiveTileClick: () -> Unit = {},
  onAddWidgetClick: () -> Unit = {},
  onQuickNoteClick: () -> Unit,
  onSavedNotesClick: () -> Unit,
  onOpenPersonalizationClick: () -> Unit,
  onOpenProfileClick: () -> Unit = onOpenPersonalizationClick,
  onOpenTileColorClick: () -> Unit,
  onOpenMoveClick: () -> Unit,
  onChangeLeftSlot: (LeftCommandSlot) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showLabelsDrawer by remember { mutableStateOf(false) }
  var showSecondaryMenu by remember { mutableStateOf(false) }

  val barBg = backgroundColor
  val fg = MetroColorUtils.legibleForeground(barBg)
  val menuBg = MaterialTheme.colorScheme.surfaceContainerHigh
  val menuFg = MaterialTheme.colorScheme.onSurface
  val focusAccent = MaterialTheme.colorScheme.primary

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .background(barBg)
        .windowInsetsPadding(WindowInsets.navigationBars)
  ) {
    HorizontalDivider(thickness = 1.dp, color = fg.copy(alpha = 0.22f))
    Row(
      modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (!isEditing) {
        if (isHubSurface) {
          CommandBarCell(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            label = stringResource(R.string.back_to_start),
            showLabel = showLabelsDrawer,
            foregroundColor = fg,
            focusAccent = focusAccent,
            enabled = true,
            onClick = onBackToStartClick,
            modifier = Modifier.weight(1f).testTag("cmd_back_to_start"),
          )
          CommandBarCell(
            icon = Icons.Filled.Add,
            label = stringResource(R.string.add_to_hub),
            showLabel = showLabelsDrawer,
            foregroundColor = fg,
            focusAccent = focusAccent,
            enabled = true,
            onClick = onAddToHubClick,
            modifier = Modifier.weight(1f).testTag("cmd_add_to_hub"),
          )
          CommandBarCell(
            icon = Icons.Outlined.Edit,
            label = stringResource(R.string.edit),
            showLabel = showLabelsDrawer,
            foregroundColor = fg,
            focusAccent = focusAccent,
            enabled = true,
            onClick = onEditClick,
            modifier = Modifier.weight(1f).testTag("cmd_edit"),
          )
        } else {
          val (leftIcon, leftLabel, leftTag) =
            when (leftSlot) {
              LeftCommandSlot.SEARCH ->
                Triple(Icons.Outlined.Search, stringResource(R.string.search), "cmd_search")
              LeftCommandSlot.QUICK_NOTE ->
                Triple(Icons.Outlined.EditNote, stringResource(R.string.quick_note), "cmd_quick_note")
              LeftCommandSlot.FOLDERS ->
                Triple(Icons.Outlined.Folder, stringResource(R.string.folders), "cmd_folders")
            }

          CommandBarCell(
            icon = leftIcon,
            label = leftLabel,
            showLabel = showLabelsDrawer,
            foregroundColor = fg,
            focusAccent = focusAccent,
            enabled = true,
            onClick = onLeftSlotClick,
            modifier = Modifier.weight(1f).testTag(leftTag),
          )
          CommandBarCell(
            icon = MetroFourSquaresOutlineIcon,
            label = stringResource(R.string.apps),
            showLabel = showLabelsDrawer,
            foregroundColor = fg,
            focusAccent = focusAccent,
            enabled = true,
            onClick = onAppsClick,
            modifier = Modifier.weight(1f).testTag("cmd_apps"),
          )
          CommandBarCell(
            icon = Icons.Outlined.Edit,
            label = stringResource(R.string.edit),
            showLabel = showLabelsDrawer,
            foregroundColor = fg,
            focusAccent = focusAccent,
            enabled = true,
            onClick = onEditClick,
            modifier = Modifier.weight(1f).testTag("cmd_edit"),
          )
        }
      } else {
        CommandBarCell(
          icon = Icons.Outlined.AspectRatio,
          label = stringResource(R.string.resize),
          showLabel = true,
          foregroundColor = fg,
          focusAccent = focusAccent,
          enabled = hasSelectedTile && !isSaving,
          onClick = onResizeClick,
          modifier = Modifier.weight(1f).testTag("cmd_resize"),
        )
        CommandBarCell(
          icon = Icons.Outlined.Close,
          label = stringResource(R.string.unpin),
          showLabel = true,
          foregroundColor = fg,
          focusAccent = focusAccent,
          enabled = hasSelectedTile && !isSaving,
          onClick = onUnpinClick,
          modifier = Modifier.weight(1f).testTag("cmd_unpin"),
        )
        CommandBarCell(
          icon = Icons.Outlined.Check,
          label = stringResource(R.string.done),
          showLabel = true,
          foregroundColor = fg,
          focusAccent = focusAccent,
          enabled = !isSaving,
          onClick = onDoneClick,
          modifier = Modifier.weight(1f).testTag("cmd_done"),
        )
      }

      Box {
        IconButton(
          onClick = {
            showLabelsDrawer = !showLabelsDrawer
            showSecondaryMenu = true
          },
          modifier =
            Modifier.size(width = 48.dp, height = 56.dp)
              .metroFocusIndicator(focusAccent, fg)
              .testTag("cmd_ellipsis"),
        ) {
          Icon(
            imageVector = Icons.Filled.MoreHoriz,
            contentDescription = stringResource(R.string.bar_more_actions),
            tint = fg,
          )
        }
        DropdownMenu(
          expanded = showSecondaryMenu,
          onDismissRequest = { showSecondaryMenu = false },
          modifier = Modifier.background(menuBg),
        ) {
          if (!isEditing) {
            if (!isHubSurface) {
              DropdownMenuItem(
                text = { Text(stringResource(R.string.create_hub), color = menuFg) },
                onClick = {
                  showSecondaryMenu = false
                  onCreateHubClick()
                },
                modifier = Modifier.testTag("bar_menu_create_hub"),
              )
            }
            DropdownMenuItem(
              text = { Text(stringResource(R.string.create_folder), color = menuFg) },
              onClick = {
                showSecondaryMenu = false
                onCreateFolderClick()
              },
              modifier = Modifier.testTag("bar_menu_create_folder"),
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.new_note), color = menuFg) },
              onClick = {
                showSecondaryMenu = false
                onQuickNoteClick()
              },
              modifier = Modifier.testTag("bar_menu_quick_note"),
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.saved_notes), color = menuFg) },
              onClick = {
                showSecondaryMenu = false
                onSavedNotesClick()
              },
              modifier = Modifier.testTag("bar_menu_saved_notes"),
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.add_live_tile), color = menuFg) },
              onClick = {
                showSecondaryMenu = false
                onAddLiveTileClick()
              },
              modifier = Modifier.testTag("bar_menu_add_live_tile"),
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.add_system_widget), color = menuFg) },
              onClick = {
                showSecondaryMenu = false
                onAddWidgetClick()
              },
              modifier = Modifier.testTag("bar_menu_add_widget"),
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.settings_section_profile), color = menuFg) },
              onClick = {
                showSecondaryMenu = false
                onOpenProfileClick()
              },
              modifier = Modifier.testTag("bar_menu_profile"),
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.settings), color = menuFg) },
              onClick = {
                showSecondaryMenu = false
                onOpenPersonalizationClick()
              },
              modifier = Modifier.testTag("bar_menu_personalization"),
            )
            if (!isHubSurface) {
              HorizontalDivider(color = MaterialTheme.colorScheme.outline)
              LeftCommandSlot.entries.forEach { slot ->
                val prefix = if (slot == leftSlot) "✓ " else ""
                DropdownMenuItem(
                  text = { Text("$prefix${stringResource(R.string.left_bar_slot)}: ${slot.labelPt}", color = menuFg) },
                  onClick = {
                    showSecondaryMenu = false
                    onChangeLeftSlot(slot)
                  },
                  modifier = Modifier.testTag("bar_menu_left_slot_${slot.id}"),
                )
              }
            }
          } else {
            DropdownMenuItem(
              text = { Text(stringResource(R.string.tile_color), color = menuFg) },
              enabled = hasSelectedTile,
              onClick = {
                showSecondaryMenu = false
                onOpenTileColorClick()
              },
              modifier = Modifier.testTag("bar_menu_tile_color"),
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.move), color = menuFg) },
              enabled = hasSelectedTile,
              onClick = {
                showSecondaryMenu = false
                onOpenMoveClick()
              },
              modifier = Modifier.testTag("bar_menu_move_tile"),
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.create_folder), color = menuFg) },
              onClick = {
                showSecondaryMenu = false
                onCreateFolderClick()
              },
              modifier = Modifier.testTag("bar_menu_edit_create_folder"),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CommandBarCell(
  icon: ImageVector,
  label: String,
  showLabel: Boolean,
  foregroundColor: Color,
  focusAccent: Color,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  Column(
    modifier =
      modifier
        .heightIn(min = 56.dp)
        .metroFocusIndicator(focusAccent, foregroundColor)
        .clickable(
          enabled = enabled,
          role = Role.Button,
          interactionSource = interactionSource,
          indication = ripple(bounded = true, color = foregroundColor),
          onClick = onClick,
        )
        .semantics { contentDescription = label }
        .padding(horizontal = 4.dp, vertical = 6.dp)
        .alpha(if (enabled) 1f else 0.35f),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = foregroundColor,
      modifier = Modifier.size(22.dp),
    )
    if (showLabel) {
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = label,
        color = foregroundColor,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

