package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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
import com.example.model.AppThemeMode
import com.example.model.LeftCommandSlot
import com.example.model.LightweightMode
import com.example.model.MetroAccent
import com.example.model.MetroColorUtils
import com.example.model.MetroHub
import com.example.model.PhotoCropMode
import com.example.model.PhotoDisplayMode
import com.example.ui.components.ConfirmDestructiveDialog
import com.example.ui.components.MetroTextInputBox
import com.example.ui.theme.MetroWhite
import com.example.ui.theme.metroFocusIndicator

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsAppearancePage(
  uiState: LauncherUiState,
  fgOnBg: Color,
  mutedOnBg: Color,
  accentColor: Color,
  onSelectThemeMode: (AppThemeMode) -> Unit,
  onSelectAccent: (MetroAccent) -> Unit,
  onApplyCustomAccentHex: (Long) -> Unit,
  onToggleShowLabels: (Boolean) -> Unit,
) {
  val fgOnAccent = MetroColorUtils.legibleForeground(accentColor)
  val outline = MaterialTheme.colorScheme.outline
  val bgColor = MaterialTheme.colorScheme.background
  val isLightBg = MetroColorUtils.isLightColor(bgColor)

  var accentHexInput by rememberSaveable(uiState.customAccentHex, uiState.accent) {
    mutableStateOf(MetroColorUtils.formatHexColor(uiState.customAccentHex ?: uiState.accent.colorValue))
  }
  var accentHexError by remember { mutableStateOf(false) }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    // 1. Theme selector
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(text = stringResource(R.string.theme_title), color = fgOnBg, style = MaterialTheme.typography.titleLarge)
      AppThemeMode.entries.forEach { mode ->
        SettingsRadioChoiceRow(
          title = mode.labelPt,
          selected = uiState.themeMode == mode,
          accentColor = accentColor,
          fgColor = fgOnBg,
          mutedColor = mutedOnBg,
          onClick = { onSelectThemeMode(mode) },
          testTag = "theme_option_${mode.id}",
        )
      }
    }

    HorizontalDivider(color = outline)

    // 2. Miniature Start grid preview
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text(text = stringResource(R.string.theme_preview_caption), color = fgOnBg, style = MaterialTheme.typography.titleLarge)
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .background(bgColor)
            .border(1.dp, outline)
            .padding(10.dp)
            .testTag("accent_preview_row"),
      ) {
        val tileOutline = if (isLightBg) Modifier.border(1.dp, outline) else Modifier
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(uiState.gutterDp.dp),
        ) {
          Box(
            modifier =
              Modifier.weight(1f)
                .height(92.dp)
                .background(accentColor)
                .then(tileOutline)
                .padding(8.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.ChatBubbleOutline,
              contentDescription = null,
              tint = fgOnAccent,
              modifier = Modifier.align(Alignment.Center).padding(bottom = 10.dp).size(26.dp),
            )
            if (uiState.showLabels) {
              Text(
                text = stringResource(R.string.messages),
                color = fgOnAccent,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                modifier = Modifier.align(Alignment.BottomStart),
              )
            }
          }

          Box(
            modifier =
              Modifier.weight(1f)
                .height(92.dp)
                .background(accentColor)
                .then(tileOutline)
                .padding(8.dp),
          ) {
            Column(modifier = Modifier.align(Alignment.TopStart), verticalArrangement = Arrangement.spacedBy(3.dp)) {
              Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(Modifier.size(16.dp).background(fgOnAccent.copy(alpha = 0.22f)))
                Box(Modifier.size(16.dp).background(fgOnAccent.copy(alpha = 0.22f)))
              }
              Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(Modifier.size(16.dp).background(fgOnAccent.copy(alpha = 0.22f)))
                Box(Modifier.size(16.dp).background(fgOnAccent.copy(alpha = 0.22f)))
              }
            }
            if (uiState.showLabels) {
              Text(
                text = "Jogos",
                color = fgOnAccent,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                modifier = Modifier.align(Alignment.BottomStart),
              )
            }
          }

          Column(
            modifier =
              Modifier.weight(1.4f)
                .height(92.dp)
                .background(accentColor)
                .then(tileOutline)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Icon(Icons.Outlined.EditNote, contentDescription = null, tint = fgOnAccent.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
              Text(
                text = "Prova de sexta",
                color = fgOnAccent,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Text(
              text = "Revisar capítulos 3 e 4",
              color = fgOnAccent.copy(alpha = 0.85f),
              style = MaterialTheme.typography.labelSmall,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }

    HorizontalDivider(color = outline)

    // 3. Accent swatches + Hex
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(text = stringResource(R.string.accent), color = fgOnBg, style = MaterialTheme.typography.titleLarge)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        MetroAccent.entries.forEach { preset ->
          val selected = uiState.customAccentHex == null && preset == uiState.accent
          val fg = MetroColorUtils.legibleForeground(preset.color)
          Box(
            modifier =
              Modifier.size(52.dp)
                .background(preset.color)
                .then(if (selected) Modifier.border(2.dp, fgOnBg) else Modifier.border(1.dp, outline))
                .metroFocusIndicator(accentColor, fg)
                .clickable(role = Role.RadioButton) { onSelectAccent(preset) }
                .semantics { contentDescription = preset.displayNamePt }
                .testTag("accent_swatch_${preset.id}"),
            contentAlignment = Alignment.Center,
          ) {
            if (selected) {
              Icon(Icons.Filled.Check, contentDescription = null, tint = fg)
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        MetroTextInputBox(
          value = accentHexInput,
          onValueChange = {
            accentHexInput = it
            accentHexError = false
          },
          placeholder = stringResource(R.string.hex_color_label),
          accentColor = accentColor,
          modifier = Modifier.weight(1f),
          inputTestTag = "accent_hex_input",
        )
        Button(
          onClick = {
            val parsed = MetroColorUtils.parseHexColor(accentHexInput)
            if (parsed != null) onApplyCustomAccentHex(parsed) else accentHexError = true
          },
          shape = RectangleShape,
          colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = fgOnAccent),
          modifier = Modifier.heightIn(min = 48.dp).testTag("accent_hex_apply"),
        ) {
          Text(stringResource(R.string.apply_hex))
        }
      }
      if (accentHexError) {
        Text(
          text = stringResource(R.string.invalid_hex),
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.labelMedium,
        )
      }
    }

    HorizontalDivider(color = outline)

    PreferenceSwitchRow(
      title = stringResource(R.string.show_labels),
      description = null,
      checked = uiState.showLabels,
      fgColor = fgOnBg,
      mutedColor = mutedOnBg,
      accentColor = accentColor,
      onCheckedChange = onToggleShowLabels,
      testTag = "show_labels_switch",
    )

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsStartScreenPage(
  uiState: LauncherUiState,
  fgOnBg: Color,
  mutedOnBg: Color,
  accentColor: Color,
  onToggleShowHomePhotos: (Boolean) -> Unit,
  onPickSinglePhoto: () -> Unit,
  onPickMultiplePhotos: () -> Unit,
  onSelectPhotoDisplayMode: (PhotoDisplayMode) -> Unit,
  onSelectPhotoCropMode: (PhotoCropMode) -> Unit,
  onToggleShowPhotoCaptions: (Boolean) -> Unit,
  onForgetSelectedPhotos: () -> Unit,
  onToggleShowPrivatePreviews: (Boolean) -> Unit,
  onRequestCalendarAccess: (Boolean) -> Unit,
  onToggleNotificationsAccess: (Boolean) -> Unit,
  onOpenSystemNotificationAccess: () -> Unit,
  onCreateHubClick: () -> Unit,
  onOpenHub: (String) -> Unit,
  onReorderHub: (String, Int) -> Unit,
  onDeleteHub: (String) -> Unit,
  onSelectGutterDp: (Int) -> Unit,
  onSelectLeftCommandSlot: (LeftCommandSlot) -> Unit,
  onToggleLockLayout: (Boolean) -> Unit,
) {
  var showForgetPhotosConfirm by remember { mutableStateOf(false) }
  var pendingDeleteHub by remember { mutableStateOf<MetroHub?>(null) }

  val fgOnAccent = MetroColorUtils.legibleForeground(accentColor)
  val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
  val onSurface = MaterialTheme.colorScheme.onSurface
  val outline = MaterialTheme.colorScheme.outline

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    // 1. Photos on Home
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(text = stringResource(R.string.home_photos_section), color = fgOnBg, style = MaterialTheme.typography.titleLarge)
      PreferenceSwitchRow(
        title = stringResource(R.string.show_home_photos),
        description = stringResource(R.string.show_home_photos_desc),
        checked = uiState.showHomePhotos,
        fgColor = fgOnBg,
        mutedColor = mutedOnBg,
        accentColor = accentColor,
        onCheckedChange = onToggleShowHomePhotos,
        testTag = "show_home_photos_switch",
      )

      if (uiState.selectedPhotoPaths.isNotEmpty()) {
        Text(
          text = stringResource(R.string.selected_photos_count, uiState.selectedPhotoPaths.size),
          color = mutedOnBg,
          style = MaterialTheme.typography.labelMedium,
        )
      }

      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
          onClick = onPickSinglePhoto,
          shape = RectangleShape,
          colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = fgOnAccent),
          modifier = Modifier.heightIn(min = 48.dp).testTag("pick_single_photo_button"),
        ) {
          Text(stringResource(R.string.pick_single_photo))
        }
        OutlinedButton(
          onClick = onPickMultiplePhotos,
          shape = RectangleShape,
          modifier = Modifier.heightIn(min = 48.dp).testTag("pick_multiple_photos_button"),
        ) {
          Text(stringResource(R.string.pick_multiple_photos), color = fgOnBg)
        }
      }

      Text(text = stringResource(R.string.photo_display_mode), color = fgOnBg, style = MaterialTheme.typography.titleMedium)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PhotoDisplayMode.entries.forEach { mode ->
          val selected = uiState.effectivePhotoMode == mode
          val enabled = !(mode == PhotoDisplayMode.ROTATING && uiState.lightMode)
          Button(
            onClick = { onSelectPhotoDisplayMode(mode) },
            enabled = enabled,
            shape = RectangleShape,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = if (selected) accentColor else surfaceContainer,
                contentColor = if (selected) fgOnAccent else onSurface,
              ),
            modifier = Modifier.heightIn(min = 48.dp).testTag("photo_mode_${mode.id}"),
          ) {
            Text(mode.labelPt)
          }
        }
      }

      Text(text = stringResource(R.string.photo_crop_mode), color = fgOnBg, style = MaterialTheme.typography.titleMedium)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PhotoCropMode.entries.forEach { crop ->
          val selected = uiState.photoCropMode == crop
          Button(
            onClick = { onSelectPhotoCropMode(crop) },
            shape = RectangleShape,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = if (selected) accentColor else surfaceContainer,
                contentColor = if (selected) fgOnAccent else onSurface,
              ),
            modifier = Modifier.heightIn(min = 48.dp).testTag("photo_crop_${crop.id}"),
          ) {
            Text(crop.labelPt)
          }
        }
      }

      PreferenceSwitchRow(
        title = stringResource(R.string.show_photo_captions),
        description = null,
        checked = uiState.showPhotoCaptions,
        fgColor = fgOnBg,
        mutedColor = mutedOnBg,
        accentColor = accentColor,
        onCheckedChange = onToggleShowPhotoCaptions,
        testTag = "show_photo_captions_switch",
      )

      if (uiState.selectedPhotoPaths.isNotEmpty()) {
        OutlinedButton(
          onClick = { showForgetPhotosConfirm = true },
          shape = RectangleShape,
          modifier = Modifier.heightIn(min = 48.dp).testTag("forget_photos_button"),
        ) {
          Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(stringResource(R.string.forget_selected_photos), color = MaterialTheme.colorScheme.error)
        }
      }
    }

    HorizontalDivider(color = outline)

    // 2. Live Tiles & Optional Permissions
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text(text = stringResource(R.string.live_tiles_section), color = fgOnBg, style = MaterialTheme.typography.titleLarge)
      PreferenceSwitchRow(
        title = stringResource(R.string.private_previews),
        description = stringResource(R.string.private_previews_desc),
        checked = uiState.showPrivatePreviews,
        fgColor = fgOnBg,
        mutedColor = mutedOnBg,
        accentColor = accentColor,
        onCheckedChange = onToggleShowPrivatePreviews,
        testTag = "private_previews_switch",
      )
      PreferenceSwitchRow(
        title = stringResource(R.string.calendar_access_title),
        description = stringResource(R.string.calendar_access_desc),
        checked = uiState.calendarAccessEnabled && uiState.liveTiles.hasCalendarPermission,
        fgColor = fgOnBg,
        mutedColor = mutedOnBg,
        accentColor = accentColor,
        onCheckedChange = onRequestCalendarAccess,
        testTag = "calendar_access_switch",
      )
      PreferenceSwitchRow(
        title = stringResource(R.string.notifications_access_title),
        description = stringResource(R.string.notifications_access_desc),
        checked = uiState.notificationsAccessEnabled,
        fgColor = fgOnBg,
        mutedColor = mutedOnBg,
        accentColor = accentColor,
        onCheckedChange = onToggleNotificationsAccess,
        testTag = "notifications_access_switch",
      )
      OutlinedButton(
        onClick = onOpenSystemNotificationAccess,
        shape = RectangleShape,
        modifier = Modifier.heightIn(min = 48.dp).testTag("open_notification_access_button"),
      ) {
        Text(stringResource(R.string.open_notification_access), color = fgOnBg)
      }
    }

    HorizontalDivider(color = outline)

    // 3. Hubs Management
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(text = stringResource(R.string.hubs), color = fgOnBg, style = MaterialTheme.typography.titleLarge)
        Button(
          onClick = onCreateHubClick,
          shape = RectangleShape,
          colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = fgOnAccent),
          modifier = Modifier.heightIn(min = 48.dp).testTag("settings_create_hub_button"),
        ) {
          Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(stringResource(R.string.create_hub))
        }
      }
      uiState.committedLayout.hubs.forEachIndexed { idx, hub ->
        HubManageRow(
          hub = hub,
          canMoveUp = idx > 0,
          canMoveDown = idx < uiState.committedLayout.hubs.lastIndex,
          fallbackAccent = accentColor,
          onOpen = { onOpenHub(hub.id) },
          onMoveUp = { onReorderHub(hub.id, -1) },
          onMoveDown = { onReorderHub(hub.id, 1) },
          onDelete = { pendingDeleteHub = hub },
        )
      }
    }

    HorizontalDivider(color = outline)

    // 4. Gutter, Left Bar Shortcut & Lock Layout
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(text = stringResource(R.string.gutter_spacing), color = fgOnBg, style = MaterialTheme.typography.titleLarge)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(2, 4, 6, 8).forEach { dp ->
          val selected = uiState.gutterDp == dp
          Button(
            onClick = { onSelectGutterDp(dp) },
            shape = RectangleShape,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = if (selected) accentColor else surfaceContainer,
                contentColor = if (selected) fgOnAccent else onSurface,
              ),
            modifier = Modifier.heightIn(min = 48.dp).testTag("gutter_option_$dp"),
          ) {
            Text("$dp dp")
          }
        }
      }

      Text(text = stringResource(R.string.left_bar_slot), color = fgOnBg, style = MaterialTheme.typography.titleLarge)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LeftCommandSlot.entries.forEach { slot ->
          val selected = uiState.leftCommandSlot == slot
          Button(
            onClick = { onSelectLeftCommandSlot(slot) },
            shape = RectangleShape,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = if (selected) accentColor else surfaceContainer,
                contentColor = if (selected) fgOnAccent else onSurface,
              ),
            modifier = Modifier.heightIn(min = 48.dp).testTag("left_slot_option_${slot.id}"),
          ) {
            Text(slot.labelPt)
          }
        }
      }

      PreferenceSwitchRow(
        title = stringResource(R.string.lock_layout),
        description = stringResource(R.string.lock_layout_description),
        checked = uiState.lockLayout,
        fgColor = fgOnBg,
        mutedColor = mutedOnBg,
        accentColor = accentColor,
        onCheckedChange = onToggleLockLayout,
        testTag = "lock_layout_switch",
      )
    }

    Spacer(modifier = Modifier.height(16.dp))
  }

  if (showForgetPhotosConfirm) {
    ConfirmDestructiveDialog(
      title = stringResource(R.string.forget_photos_confirm_title),
      message = stringResource(R.string.forget_photos_confirm_msg),
      confirmLabel = stringResource(R.string.forget_selected_photos),
      accentColor = accentColor,
      onConfirm = {
        showForgetPhotosConfirm = false
        onForgetSelectedPhotos()
      },
      onDismiss = { showForgetPhotosConfirm = false },
    )
  }

  val hubToDelete = pendingDeleteHub
  if (hubToDelete != null) {
    ConfirmDestructiveDialog(
      title = stringResource(R.string.delete_hub_confirm_title, hubToDelete.name),
      message = stringResource(R.string.delete_hub_confirm_msg),
      confirmLabel = stringResource(R.string.delete_hub),
      accentColor = accentColor,
      onConfirm = {
        pendingDeleteHub = null
        onDeleteHub(hubToDelete.id)
      },
      onDismiss = { pendingDeleteHub = null },
    )
  }
}

@Composable
fun SettingsPerformancePage(
  uiState: LauncherUiState,
  fgOnBg: Color,
  mutedOnBg: Color,
  accentColor: Color,
  onSelectLightweightMode: (LightweightMode) -> Unit,
  onToggleReduceMotion: (Boolean) -> Unit,
) {
  val outline = MaterialTheme.colorScheme.outline

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(text = stringResource(R.string.light_mode), color = fgOnBg, style = MaterialTheme.typography.titleLarge)
      Text(
        text = stringResource(R.string.light_mode_description),
        color = mutedOnBg,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.testTag("lightweight_description"),
      )

      if (uiState.isLowRamDevice) {
        Text(
          text =
            if (uiState.lightweightMode == LightweightMode.AUTO) stringResource(R.string.light_mode_auto_active_low_ram)
            else stringResource(R.string.light_mode_low_ram_detected),
          color = accentColor,
          style = MaterialTheme.typography.labelLarge,
          modifier = Modifier.testTag("low_ram_detected_banner"),
        )
      } else if (uiState.lightweightMode == LightweightMode.AUTO) {
        Text(
          text = stringResource(R.string.light_mode_auto_standard_ram),
          color = mutedOnBg,
          style = MaterialTheme.typography.labelMedium,
          modifier = Modifier.testTag("standard_ram_status"),
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      LightweightMode.entries.forEach { mode ->
        SettingsRadioChoiceRow(
          title = mode.labelPt,
          selected = uiState.lightweightMode == mode,
          accentColor = accentColor,
          fgColor = fgOnBg,
          mutedColor = mutedOnBg,
          onClick = { onSelectLightweightMode(mode) },
          testTag = "lightweight_${mode.id}",
        )
      }
    }

    HorizontalDivider(color = outline)

    PreferenceSwitchRow(
      title = stringResource(R.string.motion),
      description = stringResource(R.string.motion_description),
      checked = uiState.reduceMotion,
      fgColor = fgOnBg,
      mutedColor = mutedOnBg,
      accentColor = accentColor,
      onCheckedChange = onToggleReduceMotion,
      testTag = "reduce_motion_switch",
    )

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
fun SettingsAdvancedPage(
  uiState: LauncherUiState,
  fgOnBg: Color,
  mutedOnBg: Color,
  accentColor: Color,
  onToggleThemeThirdPartyIcons: (Boolean) -> Unit,
  onReloadIcons: () -> Unit,
  onRequestHomeRole: () -> Unit,
  onOpenSystemHomeSettings: () -> Unit,
  onSelectBackgroundHex: (Long) -> Unit,
) {
  val fgOnAccent = MetroColorUtils.legibleForeground(accentColor)
  val outline = MaterialTheme.colorScheme.outline
  val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer

  var bgHexInput by rememberSaveable(uiState.backgroundHex) {
    mutableStateOf(MetroColorUtils.formatHexColor(uiState.backgroundHex))
  }
  var bgHexError by remember { mutableStateOf(false) }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    // 1. Icons & Cache
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text(text = stringResource(R.string.reload_icons), color = fgOnBg, style = MaterialTheme.typography.titleLarge)
      PreferenceSwitchRow(
        title = stringResource(R.string.theme_third_party_icons),
        description = stringResource(R.string.theme_third_party_icons_desc),
        checked = uiState.themeThirdPartyIcons,
        fgColor = fgOnBg,
        mutedColor = mutedOnBg,
        accentColor = accentColor,
        onCheckedChange = onToggleThemeThirdPartyIcons,
        testTag = "theme_icons_switch",
      )
      OutlinedButton(
        onClick = onReloadIcons,
        shape = RectangleShape,
        modifier = Modifier.heightIn(min = 48.dp).testTag("reload_icons_button"),
      ) {
        Icon(Icons.Filled.Refresh, contentDescription = null, tint = fgOnBg, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.reload_icons), color = fgOnBg)
      }
    }

    HorizontalDivider(color = outline)

    // 2. Default Home & Moto G14 Restore
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text(text = stringResource(R.string.home_role), color = fgOnBg, style = MaterialTheme.typography.titleLarge)
      Text(
        text =
          if (uiState.isDefaultHome) stringResource(R.string.home_role_active)
          else stringResource(R.string.home_role_description),
        color = mutedOnBg,
        style = MaterialTheme.typography.bodyMedium,
      )
      Button(
        onClick = onRequestHomeRole,
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = fgOnAccent),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("set_default_home_button"),
      ) {
        Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.home_role))
      }

      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(surfaceContainer)
            .border(1.dp, outline)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(stringResource(R.string.home_restore_title), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.home_restore_instructions), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(
          onClick = onOpenSystemHomeSettings,
          shape = RectangleShape,
          modifier = Modifier.heightIn(min = 48.dp).testTag("open_home_settings_button"),
        ) {
          Text(stringResource(R.string.home_open_system_settings), color = MaterialTheme.colorScheme.onSurface)
        }
      }
    }

    HorizontalDivider(color = outline)

    // 3. Dark Theme Canvas Hex Override
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(text = stringResource(R.string.background_color), color = fgOnBg, style = MaterialTheme.typography.titleLarge)
      val bgPresets = listOf(0xFF000000L to "Preto", 0xFF0D1117L to "Ardósia", 0xFF181818L to "Carvão")
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        bgPresets.forEach { (hex, label) ->
          val selected = uiState.backgroundHex == hex
          OutlinedButton(
            onClick = { onSelectBackgroundHex(hex) },
            shape = RectangleShape,
            modifier =
              Modifier.heightIn(min = 48.dp)
                .background(Color(hex))
                .then(if (selected) Modifier.border(2.dp, accentColor) else Modifier)
                .testTag("bg_preset_${MetroColorUtils.formatHexColor(hex).drop(1)}"),
          ) {
            Text(label, color = MetroWhite)
          }
        }
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        MetroTextInputBox(
          value = bgHexInput,
          onValueChange = {
            bgHexInput = it
            bgHexError = false
          },
          placeholder = stringResource(R.string.hex_color_label),
          accentColor = accentColor,
          modifier = Modifier.weight(1f),
          inputTestTag = "bg_hex_input",
        )
        Button(
          onClick = {
            val parsed = MetroColorUtils.parseHexColor(bgHexInput)
            if (parsed != null) onSelectBackgroundHex(parsed) else bgHexError = true
          },
          shape = RectangleShape,
          colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = fgOnAccent),
          modifier = Modifier.heightIn(min = 48.dp).testTag("bg_hex_apply"),
        ) {
          Text(stringResource(R.string.apply_hex))
        }
      }
      if (bgHexError) {
        Text(
          text = stringResource(R.string.invalid_hex),
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.labelMedium,
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}
