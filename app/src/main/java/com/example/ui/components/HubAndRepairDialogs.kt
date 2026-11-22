package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.AppContextMenuState
import com.example.R
import com.example.data.LoadedAppIcon
import com.example.model.AppRepairProposal
import com.example.model.AppTargetKey
import com.example.model.DeepShortcutItem
import com.example.model.InstalledApp
import com.example.model.MetroAccent
import com.example.model.MetroColorUtils
import com.example.model.MetroHub
import com.example.model.QuickNote
import com.example.model.TileKind
import com.example.ui.theme.metroFocusIndicator

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateHubDialog(
  accentColor: Color,
  onConfirm: (name: String, accentHex: Long?, pinToStart: Boolean) -> Unit,
  onDismiss: () -> Unit,
) {
  var name by rememberSaveable { mutableStateOf("") }
  var selectedHex by rememberSaveable { mutableStateOf<Long?>(null) }
  var pinToStart by rememberSaveable { mutableStateOf(true) }
  val fgOnAccent = MetroColorUtils.legibleForeground(accentColor)
  val onSurface = MaterialTheme.colorScheme.onSurface
  val muted = MaterialTheme.colorScheme.onSurfaceVariant
  val outline = MaterialTheme.colorScheme.outline

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      contentColor = onSurface,
      border = BorderStroke(1.dp, outline),
      shape = RectangleShape,
      modifier = Modifier.fillMaxWidth(0.92f).imePadding().testTag("create_hub_dialog"),
    ) {
      Column(
        modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = stringResource(R.string.create_hub),
          color = onSurface,
          style = MaterialTheme.typography.titleLarge,
        )

        MetroTextInputBox(
          value = name,
          onValueChange = { name = it },
          placeholder = stringResource(R.string.hub_name),
          accentColor = accentColor,
          modifier = Modifier.fillMaxWidth(),
          inputTestTag = "create_hub_name_input",
        )

        Text(
          text = stringResource(R.string.hub_color),
          color = muted,
          style = MaterialTheme.typography.labelLarge,
        )

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          MetroAccent.entries.forEach { preset ->
            val isSelected = selectedHex == preset.colorValue
            val fg = MetroColorUtils.legibleForeground(preset.color)
            Box(
              modifier =
                Modifier.size(48.dp)
                  .background(preset.color)
                  .then(if (isSelected) Modifier.border(2.dp, onSurface) else Modifier)
                  .metroFocusIndicator(accentColor, fg)
                  .clickable(role = Role.RadioButton) {
                    selectedHex = if (isSelected) null else preset.colorValue
                  }
                  .semantics { contentDescription = preset.displayNamePt }
                  .testTag("hub_color_preset_${preset.id}"),
              contentAlignment = Alignment.Center,
            ) {
              if (isSelected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = fg)
              }
            }
          }
        }

        Row(
          modifier =
            Modifier.fillMaxWidth()
              .heightIn(min = 48.dp)
              .clickable { pinToStart = !pinToStart },
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Checkbox(
            checked = pinToStart,
            onCheckedChange = { pinToStart = it },
            colors = CheckboxDefaults.colors(checkedColor = accentColor, checkmarkColor = fgOnAccent),
          )
          Text(
            text = stringResource(R.string.pin_hub_to_start),
            color = onSurface,
            style = MaterialTheme.typography.bodyMedium,
          )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onDismiss, shape = RectangleShape, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.cancel), color = muted)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = { onConfirm(name, selectedHex, pinToStart) },
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = fgOnAccent),
            modifier =
              Modifier.heightIn(min = 48.dp)
                .metroFocusIndicator(accentColor, fgOnAccent)
                .testTag("create_hub_confirm_button"),
          ) {
            Text(stringResource(R.string.confirm))
          }
        }
      }
    }
  }
}

@Composable
fun RenameHubDialog(
  initialName: String,
  accentColor: Color,
  onConfirm: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var name by rememberSaveable { mutableStateOf(initialName) }
  val onSurface = MaterialTheme.colorScheme.onSurface
  val fgOnAccent = MetroColorUtils.legibleForeground(accentColor)

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RectangleShape,
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    titleContentColor = onSurface,
    title = { Text(stringResource(R.string.rename_hub)) },
    text = {
      MetroTextInputBox(
        value = name,
        onValueChange = { name = it },
        placeholder = stringResource(R.string.hub_name),
        accentColor = accentColor,
        modifier = Modifier.fillMaxWidth(),
        inputTestTag = "rename_hub_input",
      )
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(name) },
        shape = RectangleShape,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = accentColor,
            contentColor = fgOnAccent,
          ),
        modifier = Modifier.heightIn(min = 48.dp).testTag("rename_hub_confirm"),
      ) {
        Text(stringResource(R.string.confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, shape = RectangleShape, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(stringResource(R.string.cancel), color = onSurface)
      }
    },
  )
}

@Composable
fun HubColorDialog(
  currentAccentHex: Long?,
  globalAccentColor: Color,
  onSelectColor: (Long?) -> Unit,
  onDismiss: () -> Unit,
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    TileColorSheet(
      currentCustomColorHex = currentAccentHex,
      globalAccentColor = globalAccentColor,
      onSelectColor = onSelectColor,
      onDismiss = onDismiss,
      modifier = Modifier.fillMaxWidth(0.92f),
    )
  }
}

@Composable
fun PinToHubDialog(
  app: InstalledApp,
  hubs: List<MetroHub>,
  accentColor: Color,
  onSelectHub: (String) -> Unit,
  onCreateNewHub: () -> Unit,
  onDismiss: () -> Unit,
) {
  val onSurface = MaterialTheme.colorScheme.onSurface
  val muted = MaterialTheme.colorScheme.onSurfaceVariant
  val surfaceRow = MaterialTheme.colorScheme.surfaceContainer
  val outline = MaterialTheme.colorScheme.outline

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      contentColor = onSurface,
      border = BorderStroke(1.dp, outline),
      shape = RectangleShape,
      modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 480.dp).testTag("pin_to_hub_dialog"),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "${stringResource(R.string.pin_to_hub)}: ${app.label}",
          color = onSurface,
          style = MaterialTheme.typography.titleLarge,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )

        if (hubs.isEmpty()) {
          Text(
            text = stringResource(R.string.hub_empty),
            color = muted,
            style = MaterialTheme.typography.bodyMedium,
          )
        } else {
          LazyColumn(
            modifier = Modifier.weight(1f, fill = false).heightIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            items(hubs, key = { it.id }) { hub ->
              val hubColor = hub.accentHex?.let { Color(it) } ?: accentColor
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .background(surfaceRow)
                    .border(1.dp, outline)
                    .metroFocusIndicator(accentColor, onSurface)
                    .clickable(role = Role.Button) { onSelectHub(hub.id) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("pin_to_hub_option_${hub.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Box(modifier = Modifier.size(16.dp).background(hubColor))
                Text(
                  text = hub.name,
                  color = onSurface,
                  style = MaterialTheme.typography.bodyLarge,
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedButton(
            onClick = onCreateNewHub,
            shape = RectangleShape,
            modifier = Modifier.heightIn(min = 48.dp).testTag("pin_to_hub_create_new"),
          ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = onSurface, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.create_hub), color = onSurface)
          }
          TextButton(onClick = onDismiss, shape = RectangleShape, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.cancel), color = muted)
          }
        }
      }
    }
  }
}

@Composable
fun AddToHubDialog(
  hubName: String,
  savedNotes: List<QuickNote>,
  accentColor: Color,
  onAddLiveTile: (TileKind) -> Unit,
  onAddSavedNote: (QuickNote) -> Unit,
  onCreateNote: () -> Unit,
  onCreateFolder: () -> Unit,
  onAddWidget: () -> Unit,
  onOpenAppsToPin: () -> Unit,
  onDismiss: () -> Unit,
) {
  val onSurface = MaterialTheme.colorScheme.onSurface
  val muted = MaterialTheme.colorScheme.onSurfaceVariant
  val outline = MaterialTheme.colorScheme.outline

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      contentColor = onSurface,
      border = BorderStroke(1.dp, outline),
      shape = RectangleShape,
      modifier = Modifier.fillMaxWidth(0.92f).heightIn(max = 580.dp).testTag("add_to_hub_dialog"),
    ) {
      Column(
        modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "${stringResource(R.string.add_to_hub)}: $hubName",
          color = onSurface,
          style = MaterialTheme.typography.titleLarge,
        )

        ActionRowButton(
          icon = Icons.Outlined.Apps,
          label = stringResource(R.string.apps),
          accentColor = accentColor,
          onClick = onOpenAppsToPin,
          testTag = "hub_add_apps",
        )
        ActionRowButton(
          icon = Icons.Outlined.EditNote,
          label = stringResource(R.string.new_note),
          accentColor = accentColor,
          onClick = onCreateNote,
          testTag = "hub_add_new_note",
        )
        ActionRowButton(
          icon = Icons.Outlined.Folder,
          label = stringResource(R.string.create_folder),
          accentColor = accentColor,
          onClick = onCreateFolder,
          testTag = "hub_add_folder",
        )
        ActionRowButton(
          icon = Icons.Outlined.Widgets,
          label = stringResource(R.string.add_system_widget),
          accentColor = accentColor,
          onClick = onAddWidget,
          testTag = "hub_add_widget",
        )

        HorizontalDivider(color = outline)
        Text(
          text = stringResource(R.string.add_live_tile),
          color = muted,
          style = MaterialTheme.typography.labelLarge,
        )

        val liveOptions =
          listOf(
            Triple(TileKind.CALENDAR, Icons.Outlined.CalendarToday, stringResource(R.string.calendar)),
            Triple(TileKind.CLOCK, Icons.Outlined.Schedule, stringResource(R.string.clock)),
            Triple(TileKind.BATTERY, Icons.Outlined.BatteryStd, stringResource(R.string.battery)),
            Triple(TileKind.MUSIC, Icons.Outlined.MusicNote, stringResource(R.string.music_tile)),
            Triple(TileKind.NOTIFICATIONS, Icons.Outlined.Notifications, stringResource(R.string.notifications_tile)),
            Triple(TileKind.PHOTO, Icons.Outlined.PhotoLibrary, stringResource(R.string.photo)),
          )
        liveOptions.forEach { (kind, icon, label) ->
          ActionRowButton(
            icon = icon,
            label = label,
            accentColor = accentColor,
            onClick = { onAddLiveTile(kind) },
            testTag = "hub_add_live_${kind.name.lowercase()}",
          )
        }

        if (savedNotes.isNotEmpty()) {
          HorizontalDivider(color = outline)
          Text(
            text = stringResource(R.string.add_existing_note),
            color = muted,
            style = MaterialTheme.typography.labelLarge,
          )
          savedNotes.take(6).forEach { note ->
            ActionRowButton(
              icon = Icons.Outlined.EditNote,
              label = note.title.ifBlank { note.body.lines().firstOrNull().orEmpty().ifBlank { stringResource(R.string.quick_note) } },
              accentColor = accentColor,
              onClick = { onAddSavedNote(note) },
              testTag = "hub_add_saved_note_${note.id}",
            )
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
}

@Composable
fun AddLiveTileDialog(
  accentColor: Color,
  onSelectLiveTile: (TileKind) -> Unit,
  onDismiss: () -> Unit,
) {
  val onSurface = MaterialTheme.colorScheme.onSurface
  val muted = MaterialTheme.colorScheme.onSurfaceVariant
  val outline = MaterialTheme.colorScheme.outline

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      contentColor = onSurface,
      border = BorderStroke(1.dp, outline),
      shape = RectangleShape,
      modifier = Modifier.fillMaxWidth(0.9f).testTag("add_live_tile_dialog"),
    ) {
      Column(
        modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = stringResource(R.string.add_live_tile),
          color = onSurface,
          style = MaterialTheme.typography.titleLarge,
        )
        val liveOptions =
          listOf(
            Triple(TileKind.CLOCK, Icons.Outlined.Schedule, stringResource(R.string.clock)),
            Triple(TileKind.CALENDAR, Icons.Outlined.CalendarToday, stringResource(R.string.calendar)),
            Triple(TileKind.BATTERY, Icons.Outlined.BatteryStd, stringResource(R.string.battery)),
            Triple(TileKind.MUSIC, Icons.Outlined.MusicNote, stringResource(R.string.music_tile)),
            Triple(TileKind.NOTIFICATIONS, Icons.Outlined.Notifications, stringResource(R.string.notifications_tile)),
            Triple(TileKind.PHOTO, Icons.Outlined.PhotoLibrary, stringResource(R.string.photo)),
          )
        liveOptions.forEach { (kind, icon, label) ->
          ActionRowButton(
            icon = icon,
            label = label,
            accentColor = accentColor,
            onClick = { onSelectLiveTile(kind) },
            testTag = "add_live_${kind.name.lowercase()}",
          )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onDismiss, shape = RectangleShape, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.cancel), color = muted)
          }
        }
      }
    }
  }
}

@Composable
fun AppRepairDialog(
  proposal: AppRepairProposal,
  iconCacheGeneration: Long,
  loadAppIcon: suspend (AppTargetKey) -> LoadedAppIcon,
  accentColor: Color,
  onSelectCandidate: (InstalledApp) -> Unit,
  onDismiss: () -> Unit,
) {
  val onSurface = MaterialTheme.colorScheme.onSurface
  val muted = MaterialTheme.colorScheme.onSurfaceVariant
  val surfaceRow = MaterialTheme.colorScheme.surfaceContainer
  val outline = MaterialTheme.colorScheme.outline

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      contentColor = onSurface,
      border = BorderStroke(1.dp, outline),
      shape = RectangleShape,
      modifier = Modifier.fillMaxWidth(0.92f).heightIn(max = 520.dp).testTag("app_repair_dialog"),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "${stringResource(R.string.choose_app)}: ${proposal.labelHint}",
          color = onSurface,
          style = MaterialTheme.typography.titleLarge,
        )
        Text(
          text = stringResource(R.string.choose_app_repair_desc),
          color = muted,
          style = MaterialTheme.typography.bodyMedium,
        )

        LazyColumn(
          modifier = Modifier.weight(1f, fill = false).heightIn(max = 300.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          items(proposal.candidates, key = { it.key.stableKey }) { candidate ->
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .heightIn(min = 52.dp)
                  .background(surfaceRow)
                  .border(1.dp, outline)
                  .metroFocusIndicator(accentColor, onSurface)
                  .clickable(role = Role.Button) { onSelectCandidate(candidate) }
                  .padding(horizontal = 10.dp, vertical = 8.dp)
                  .testTag("repair_candidate_${candidate.key.stableKey}"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Box(
                modifier = Modifier.size(40.dp).background(accentColor),
                contentAlignment = Alignment.Center,
              ) {
                AppIconImage(
                  targetKey = candidate.key,
                  fallbackLabel = candidate.label,
                  iconCacheGeneration = iconCacheGeneration,
                  loadAppIcon = loadAppIcon,
                  iconSize = 32.dp,
                  foregroundFallbackColor = MetroColorUtils.legibleForeground(accentColor),
                )
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = candidate.label,
                  color = onSurface,
                  style = MaterialTheme.typography.bodyLarge,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = candidate.key.packageName,
                  color = muted,
                  style = MaterialTheme.typography.labelSmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
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
}

@Composable
fun AppContextMenuDialog(
  menuState: AppContextMenuState,
  accentColor: Color,
  onPinToStart: () -> Unit,
  onPinToHub: () -> Unit,
  onLaunchDeepShortcut: (DeepShortcutItem) -> Unit,
  onOpenAppInfo: () -> Unit,
  onDismiss: () -> Unit,
) {
  val app = menuState.app
  val onSurface = MaterialTheme.colorScheme.onSurface
  val muted = MaterialTheme.colorScheme.onSurfaceVariant
  val outline = MaterialTheme.colorScheme.outline

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      contentColor = onSurface,
      border = BorderStroke(1.dp, outline),
      shape = RectangleShape,
      modifier = Modifier.fillMaxWidth(0.88f).testTag("app_context_menu_dialog"),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = app.label,
          color = onSurface,
          style = MaterialTheme.typography.titleLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (app.isWorkProfile) {
          Text(
            text =
              if (app.isProfilePaused) stringResource(R.string.work_profile_paused)
              else stringResource(R.string.work_profile_badge),
            color = muted,
            style = MaterialTheme.typography.labelMedium,
          )
        }
        HorizontalDivider(color = outline)

        ActionRowButton(
          icon = Icons.Filled.Add,
          label = stringResource(R.string.pin),
          accentColor = accentColor,
          onClick = onPinToStart,
          testTag = "app_menu_pin_start",
        )
        ActionRowButton(
          icon = Icons.Outlined.Folder,
          label = stringResource(R.string.pin_to_hub),
          accentColor = accentColor,
          onClick = onPinToHub,
          testTag = "app_menu_pin_hub",
        )
        ActionRowButton(
          icon = Icons.Outlined.Info,
          label = stringResource(R.string.app_info),
          accentColor = accentColor,
          onClick = onOpenAppInfo,
          testTag = "app_menu_app_info",
        )

        if (menuState.deepShortcuts.isNotEmpty()) {
          HorizontalDivider(color = outline)
          Text(
            text = stringResource(R.string.deep_shortcuts),
            color = muted,
            style = MaterialTheme.typography.labelLarge,
          )
          menuState.deepShortcuts.forEach { shortcut ->
            ActionRowButton(
              icon = Icons.Outlined.Apps,
              label = shortcut.label,
              accentColor = accentColor,
              onClick = { onLaunchDeepShortcut(shortcut) },
              testTag = "app_deep_shortcut_${shortcut.id}",
            )
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
}

@Composable
private fun ActionRowButton(
  icon: ImageVector,
  label: String,
  accentColor: Color,
  onClick: () -> Unit,
  testTag: String,
) {
  val onSurface = MaterialTheme.colorScheme.onSurface
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .heightIn(min = 48.dp)
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .border(1.dp, MaterialTheme.colorScheme.outline)
        .metroFocusIndicator(accentColor, onSurface)
        .clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 10.dp)
        .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
    Text(
      text = label,
      color = onSurface,
      style = MaterialTheme.typography.bodyLarge,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )
  }
}
