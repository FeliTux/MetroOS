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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.NoteEditorSession
import com.example.R
import com.example.data.AppCatalogManager
import com.example.model.AppTargetKey
import com.example.model.InstalledApp
import com.example.model.MetroAccent
import com.example.model.MetroColorUtils
import com.example.model.QuickNote
import com.example.model.TileSize
import com.example.ui.theme.metroFocusIndicator

@Composable
fun CreateOrEditFolderAppsDialog(
  title: String,
  showNameField: Boolean,
  initialName: String,
  initialSelectedKeys: List<AppTargetKey>,
  installedApps: List<InstalledApp>,
  accentColor: Color,
  onConfirm: (name: String, selectedKeys: List<AppTargetKey>) -> Unit,
  onDismiss: () -> Unit,
) {
  var folderName by rememberSaveable { mutableStateOf(initialName) }
  var searchQuery by rememberSaveable { mutableStateOf("") }
  val selectedKeys = remember {
    mutableStateListOf<AppTargetKey>().apply { addAll(initialSelectedKeys) }
  }

  val filtered =
    remember(installedApps, searchQuery) {
      if (searchQuery.isBlank()) installedApps
      else {
        val norm = AppCatalogManager.normalizeForSearch(searchQuery)
        installedApps.filter { it.normalizedLabel.contains(norm) }
      }
    }

  val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
  val onSurface = MaterialTheme.colorScheme.onSurface
  val muted = MaterialTheme.colorScheme.onSurfaceVariant
  val outline = MaterialTheme.colorScheme.outline
  val fgOnAccent = MetroColorUtils.legibleForeground(accentColor)

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      color = containerColor,
      contentColor = onSurface,
      border = BorderStroke(1.dp, outline),
      shape = RectangleShape,
      modifier = Modifier.fillMaxWidth(0.92f).heightIn(max = 560.dp).imePadding().testTag("create_folder_dialog"),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = title, color = onSurface, style = MaterialTheme.typography.titleLarge)

        if (showNameField) {
          MetroTextInputBox(
            value = folderName,
            onValueChange = { folderName = it },
            placeholder = stringResource(R.string.folder_name),
            accentColor = accentColor,
            modifier = Modifier.fillMaxWidth(),
            inputTestTag = "folder_name_input",
          )
        }

        MetroTextInputBox(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = stringResource(R.string.search_hint),
          accentColor = accentColor,
          modifier = Modifier.fillMaxWidth(),
          inputTestTag = "folder_apps_search_input",
        )

        LazyColumn(
          modifier = Modifier.weight(1f, fill = false).heightIn(min = 160.dp, max = 300.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          items(filtered, key = { it.key.stableKey }) { app ->
            val checked = app.key in selectedKeys
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .heightIn(min = 48.dp)
                  .metroFocusIndicator(accentColor, onSurface)
                  .clickable {
                    if (checked) selectedKeys.remove(app.key) else selectedKeys.add(app.key)
                  }
                  .padding(horizontal = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Checkbox(
                checked = checked,
                onCheckedChange = {
                  if (checked) selectedKeys.remove(app.key) else selectedKeys.add(app.key)
                },
                colors =
                  CheckboxDefaults.colors(
                    checkedColor = accentColor,
                    checkmarkColor = fgOnAccent,
                  ),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = app.label,
                color = onSurface,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(
            onClick = onDismiss,
            shape = RectangleShape,
            modifier = Modifier.heightIn(min = 48.dp).metroFocusIndicator(accentColor, onSurface),
          ) {
            Text(stringResource(R.string.cancel), color = muted)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = { onConfirm(folderName, selectedKeys.toList()) },
            enabled = selectedKeys.isNotEmpty(),
            shape = RectangleShape,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = fgOnAccent,
              ),
            modifier =
              Modifier.heightIn(min = 48.dp)
                .metroFocusIndicator(accentColor, fgOnAccent)
                .testTag("folder_confirm_button"),
          ) {
            Text(stringResource(R.string.confirm))
          }
        }
      }
    }
  }
}

@Composable
fun FolderNameDialog(
  title: String,
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
    title = { Text(title) },
    text = {
      MetroTextInputBox(
        value = name,
        onValueChange = { name = it },
        placeholder = stringResource(R.string.folder_name),
        accentColor = accentColor,
        modifier = Modifier.fillMaxWidth(),
        inputTestTag = "folder_rename_input",
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
        modifier =
          Modifier.heightIn(min = 48.dp)
            .metroFocusIndicator(accentColor, fgOnAccent)
            .testTag("folder_name_confirm_button"),
      ) {
        Text(stringResource(R.string.confirm))
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        shape = RectangleShape,
        modifier = Modifier.heightIn(min = 48.dp).metroFocusIndicator(accentColor, onSurface),
      ) {
        Text(stringResource(R.string.cancel), color = onSurface)
      }
    },
  )
}

@Composable
fun NoteEditorDialog(
  session: NoteEditorSession,
  accentColor: Color,
  onSave: (title: String, body: String, isChecklist: Boolean, pinToStart: Boolean, size: TileSize) -> Unit,
  onRemoveFromStart: (noteId: String) -> Unit,
  onDeleteNote: (noteId: String) -> Unit,
  onDismiss: () -> Unit,
) {
  var title by rememberSaveable(session.noteId) { mutableStateOf(session.title) }
  var body by rememberSaveable(session.noteId) { mutableStateOf(session.body) }
  var isChecklist by rememberSaveable(session.noteId) { mutableStateOf(session.isChecklist) }
  var pinToStart by rememberSaveable(session.noteId) { mutableStateOf(session.isPinnedToStart) }
  var selectedSizeName by rememberSaveable(session.noteId) { mutableStateOf(session.preferredSize.name) }
  var newChecklistItem by rememberSaveable(session.noteId) { mutableStateOf("") }

  val selectedSize = if (selectedSizeName == TileSize.WIDE.name) TileSize.WIDE else TileSize.MEDIUM
  val fgOnAccent = MetroColorUtils.legibleForeground(accentColor)
  val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
  val surfaceRow = MaterialTheme.colorScheme.surfaceContainer
  val onSurface = MaterialTheme.colorScheme.onSurface
  val muted = MaterialTheme.colorScheme.onSurfaceVariant
  val outline = MaterialTheme.colorScheme.outline

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      color = containerColor,
      contentColor = onSurface,
      border = BorderStroke(1.dp, outline),
      shape = RectangleShape,
      modifier = Modifier.fillMaxWidth(0.94f).heightIn(max = 620.dp).imePadding().testTag("note_editor_dialog"),
    ) {
      Column(
        modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(R.string.quick_note),
            color = onSurface,
            style = MaterialTheme.typography.titleLarge,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val plainSelected = !isChecklist
            TextButton(
              onClick = { isChecklist = false },
              shape = RectangleShape,
              modifier =
                Modifier.heightIn(min = 48.dp)
                  .background(if (plainSelected) accentColor else surfaceRow)
                  .metroFocusIndicator(accentColor, onSurface)
                  .testTag("note_mode_text"),
            ) {
              Text(
                stringResource(R.string.note_plain_text),
                color = if (plainSelected) fgOnAccent else onSurface,
              )
            }
            TextButton(
              onClick = { isChecklist = true },
              shape = RectangleShape,
              modifier =
                Modifier.heightIn(min = 48.dp)
                  .background(if (isChecklist) accentColor else surfaceRow)
                  .metroFocusIndicator(accentColor, onSurface)
                  .testTag("note_mode_checklist"),
            ) {
              Text(
                stringResource(R.string.note_checklist),
                color = if (isChecklist) fgOnAccent else onSurface,
              )
            }
          }
        }

        MetroTextInputBox(
          value = title,
          onValueChange = { title = it },
          placeholder = stringResource(R.string.note_title_hint),
          accentColor = accentColor,
          modifier = Modifier.fillMaxWidth(),
          inputTestTag = "note_title_input",
        )

        if (!isChecklist) {
          MetroTextInputBox(
            value = body,
            onValueChange = { body = it },
            placeholder = stringResource(R.string.note_body_hint),
            accentColor = accentColor,
            singleLine = false,
            minHeightDp = 140,
            modifier = Modifier.fillMaxWidth(),
            inputTestTag = "note_body_input",
          )
        } else {
          val lines = remember(body) { body.lines().filter { it.isNotBlank() } }
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            lines.forEachIndexed { idx, rawLine ->
              val checked = rawLine.startsWith("[x] ", ignoreCase = true)
              val itemText =
                rawLine.removePrefix("[x] ").removePrefix("[X] ").removePrefix("[ ] ")
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .background(surfaceRow)
                    .clickable {
                      val updatedLines = lines.toMutableList()
                      updatedLines[idx] = if (checked) "[ ] $itemText" else "[x] $itemText"
                      body = updatedLines.joinToString("\n")
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = if (checked) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                  contentDescription = null,
                  tint = onSurface,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = itemText,
                  color = if (checked) muted else onSurface,
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier.weight(1f),
                )
                IconButton(
                  onClick = {
                    val updatedLines = lines.toMutableList().apply { removeAt(idx) }
                    body = updatedLines.joinToString("\n")
                  },
                  modifier = Modifier.size(48.dp),
                ) {
                  Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel), tint = muted)
                }
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              MetroTextInputBox(
                value = newChecklistItem,
                onValueChange = { newChecklistItem = it },
                placeholder = stringResource(R.string.add_checklist_item),
                accentColor = accentColor,
                modifier = Modifier.weight(1f),
                inputTestTag = "note_checklist_item_input",
              )
              IconButton(
                onClick = {
                  val cleanItem = newChecklistItem.trim()
                  if (cleanItem.isNotEmpty()) {
                    body = (lines + "[ ] $cleanItem").joinToString("\n")
                    newChecklistItem = ""
                  }
                },
                modifier =
                  Modifier.size(48.dp)
                    .background(accentColor)
                    .metroFocusIndicator(accentColor, fgOnAccent)
                    .testTag("note_add_item_button"),
              ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_checklist_item), tint = fgOnAccent)
              }
            }
          }
        }

        // Pin & Size options
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 48.dp).clickable { pinToStart = !pinToStart },
          ) {
            Checkbox(
              checked = pinToStart,
              onCheckedChange = { pinToStart = it },
              colors = CheckboxDefaults.colors(checkedColor = accentColor, checkmarkColor = fgOnAccent),
            )
            Text(stringResource(R.string.pin_note_to_start), color = onSurface, style = MaterialTheme.typography.bodyMedium)
          }

          if (pinToStart) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              TextButton(
                onClick = { selectedSizeName = TileSize.MEDIUM.name },
                shape = RectangleShape,
                modifier =
                  Modifier.heightIn(min = 48.dp)
                    .background(if (selectedSize == TileSize.MEDIUM) accentColor else surfaceRow)
                    .testTag("note_size_medium"),
              ) {
                Text(stringResource(R.string.medium), color = if (selectedSize == TileSize.MEDIUM) fgOnAccent else onSurface)
              }
              TextButton(
                onClick = { selectedSizeName = TileSize.WIDE.name },
                shape = RectangleShape,
                modifier =
                  Modifier.heightIn(min = 48.dp)
                    .background(if (selectedSize == TileSize.WIDE) accentColor else surfaceRow)
                    .testTag("note_size_wide"),
              ) {
                Text(stringResource(R.string.wide), color = if (selectedSize == TileSize.WIDE) fgOnAccent else onSurface)
              }
            }
          }
        }

        if (session.noteId != null) {
          HorizontalDivider(color = outline)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            if (session.isPinnedToStart) {
              OutlinedButton(
                onClick = { onRemoveFromStart(session.noteId) },
                shape = RectangleShape,
                modifier = Modifier.heightIn(min = 48.dp).testTag("note_remove_from_start_button"),
              ) {
                Text(stringResource(R.string.remove_from_start), color = onSurface)
              }
            } else {
              Spacer(modifier = Modifier.width(1.dp))
            }
            TextButton(
              onClick = { onDeleteNote(session.noteId) },
              shape = RectangleShape,
              modifier = Modifier.heightIn(min = 48.dp).testTag("note_delete_button"),
            ) {
              Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(stringResource(R.string.delete_note), color = MaterialTheme.colorScheme.error)
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(
            onClick = onDismiss,
            shape = RectangleShape,
            modifier = Modifier.heightIn(min = 48.dp),
          ) {
            Text(stringResource(R.string.cancel), color = muted)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = { onSave(title, body, isChecklist, pinToStart, selectedSize) },
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = fgOnAccent),
            modifier =
              Modifier.heightIn(min = 48.dp)
                .metroFocusIndicator(accentColor, fgOnAccent)
                .testTag("note_save_button"),
          ) {
            Text(stringResource(R.string.save))
          }
        }
      }
    }
  }
}

@Composable
fun SavedNotesDialog(
  notes: List<QuickNote>,
  accentColor: Color,
  onSelectNote: (QuickNote) -> Unit,
  onNewNote: () -> Unit,
  onDismiss: () -> Unit,
) {
  val onSurface = MaterialTheme.colorScheme.onSurface
  val muted = MaterialTheme.colorScheme.onSurfaceVariant
  val surfaceRow = MaterialTheme.colorScheme.surfaceContainer
  val outline = MaterialTheme.colorScheme.outline
  val fgOnAccent = MetroColorUtils.legibleForeground(accentColor)

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      contentColor = onSurface,
      border = BorderStroke(1.dp, outline),
      shape = RectangleShape,
      modifier = Modifier.fillMaxWidth(0.92f).heightIn(max = 520.dp).testTag("saved_notes_dialog"),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(stringResource(R.string.saved_notes), color = onSurface, style = MaterialTheme.typography.titleLarge)
          Button(
            onClick = onNewNote,
            shape = RectangleShape,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = fgOnAccent,
              ),
            modifier = Modifier.heightIn(min = 48.dp),
          ) {
            Text(stringResource(R.string.new_note))
          }
        }

        if (notes.isEmpty()) {
          Text(
            text = stringResource(R.string.no_saved_notes),
            color = muted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 24.dp),
          )
        } else {
          LazyColumn(
            modifier = Modifier.weight(1f, fill = false).heightIn(max = 340.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            items(notes, key = { it.id }) { note ->
              Column(
                modifier =
                  Modifier.fillMaxWidth()
                    .background(surfaceRow)
                    .border(1.dp, outline)
                    .metroFocusIndicator(accentColor, onSurface)
                    .clickable { onSelectNote(note) }
                    .padding(12.dp)
                    .testTag("saved_note_item_${note.id}"),
              ) {
                Text(
                  text = note.title.ifBlank { stringResource(R.string.quick_note) },
                  color = onSurface,
                  style = MaterialTheme.typography.titleMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                if (note.body.isNotBlank()) {
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = note.body,
                    color = muted,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onDismiss, shape = RectangleShape, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.cancel), color = onSurface)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TileColorSheet(
  currentCustomColorHex: Long?,
  globalAccentColor: Color,
  onSelectColor: (Long?) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var hexText by rememberSaveable {
    mutableStateOf(currentCustomColorHex?.let { MetroColorUtils.formatHexColor(it) } ?: "")
  }
  var hexInvalid by remember { mutableStateOf(false) }

  val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
  val onSurface = MaterialTheme.colorScheme.onSurface
  val muted = MaterialTheme.colorScheme.onSurfaceVariant
  val outline = MaterialTheme.colorScheme.outline
  val fgOnAccent = MetroColorUtils.legibleForeground(globalAccentColor)

  Surface(
    color = containerColor,
    contentColor = onSurface,
    border = BorderStroke(1.dp, outline),
    shape = RectangleShape,
    modifier = modifier.fillMaxWidth().testTag("tile_color_sheet"),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(stringResource(R.string.tile_color), color = onSurface, style = MaterialTheme.typography.titleMedium)
        TextButton(
          onClick = { onSelectColor(null) },
          shape = RectangleShape,
          modifier = Modifier.heightIn(min = 48.dp).testTag("use_theme_color_button"),
        ) {
          Text(stringResource(R.string.use_theme_color), color = onSurface)
        }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        MetroAccent.entries.forEach { preset ->
          val selected = currentCustomColorHex == preset.colorValue
          val fg = MetroColorUtils.legibleForeground(preset.color)
          Box(
            modifier =
              Modifier.size(48.dp)
                .background(preset.color)
                .then(if (selected) Modifier.border(2.dp, onSurface) else Modifier)
                .metroFocusIndicator(globalAccentColor, fg)
                .clickable(role = Role.RadioButton) { onSelectColor(preset.colorValue) }
                .semantics { contentDescription = preset.displayNamePt }
                .testTag("tile_color_preset_${preset.id}"),
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
          value = hexText,
          onValueChange = {
            hexText = it
            hexInvalid = false
          },
          placeholder = stringResource(R.string.hex_color_label),
          accentColor = globalAccentColor,
          modifier = Modifier.weight(1f),
          inputTestTag = "tile_color_hex_input",
        )
        Button(
          onClick = {
            val parsed = MetroColorUtils.parseHexColor(hexText)
            if (parsed != null) {
              onSelectColor(parsed)
            } else {
              hexInvalid = true
            }
          },
          shape = RectangleShape,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = globalAccentColor,
              contentColor = fgOnAccent,
            ),
          modifier = Modifier.heightIn(min = 48.dp).testTag("tile_color_hex_apply"),
        ) {
          Text(stringResource(R.string.apply_hex))
        }
      }

      if (hexInvalid) {
        Text(stringResource(R.string.invalid_hex), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
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
fun MetroTextInputBox(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  accentColor: Color,
  modifier: Modifier = Modifier,
  singleLine: Boolean = true,
  minHeightDp: Int = 48,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  inputTestTag: String? = null,
) {
  val boxBg = MaterialTheme.colorScheme.surface
  val textColor = MaterialTheme.colorScheme.onSurface
  val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant
  val borderColor = MaterialTheme.colorScheme.outline

  Box(
    modifier =
      modifier
        .heightIn(min = minHeightDp.dp)
        .background(boxBg)
        .border(1.dp, borderColor)
        .padding(horizontal = 12.dp, vertical = 10.dp),
    contentAlignment = Alignment.TopStart,
  ) {
    if (value.isEmpty()) {
      Text(
        text = placeholder,
        color = placeholderColor,
        style = MaterialTheme.typography.bodyLarge,
      )
    }
    BasicTextField(
      value = value,
      onValueChange = onValueChange,
      singleLine = singleLine,
      keyboardOptions = keyboardOptions,
      textStyle = TextStyle(color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Normal),
      cursorBrush = SolidColor(accentColor),
      modifier =
        Modifier.fillMaxWidth()
          .semantics { contentDescription = placeholder }
          .let { if (inputTestTag != null) it.testTag(inputTestTag) else it },
    )
  }
}
