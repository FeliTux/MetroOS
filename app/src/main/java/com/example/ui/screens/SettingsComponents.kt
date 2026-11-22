package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.MetroColorUtils
import com.example.model.MetroHub
import com.example.ui.theme.metroFocusIndicator

@Composable
internal fun PreferenceSwitchRow(
  title: String,
  description: String?,
  checked: Boolean,
  fgColor: Color,
  mutedColor: Color,
  accentColor: Color,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .heightIn(min = 56.dp)
        .metroFocusIndicator(accentColor, fgColor)
        .clickable(role = Role.Switch) { onCheckedChange(!checked) }
        .padding(vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
      Text(text = title, color = fgColor, style = MaterialTheme.typography.titleMedium)
      if (!description.isNullOrBlank()) {
        Text(text = description, color = mutedColor, style = MaterialTheme.typography.bodyMedium)
      }
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors =
        SwitchDefaults.colors(
          checkedThumbColor = MetroColorUtils.legibleForeground(accentColor),
          checkedTrackColor = accentColor,
        ),
      modifier = Modifier.testTag(testTag),
    )
  }
}

@Composable
internal fun SettingsRadioChoiceRow(
  title: String,
  subtitle: String? = null,
  selected: Boolean,
  accentColor: Color,
  fgColor: Color,
  mutedColor: Color,
  onClick: () -> Unit,
  testTag: String,
) {
  val rowBg =
    if (selected) accentColor.copy(alpha = 0.22f)
    else MaterialTheme.colorScheme.surfaceContainer
  val outline =
    if (selected) accentColor
    else MaterialTheme.colorScheme.outline

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .heightIn(min = 52.dp)
        .background(rowBg)
        .border(if (selected) 2.dp else 1.dp, outline)
        .metroFocusIndicator(accentColor, fgColor)
        .clickable(role = Role.RadioButton, onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 10.dp)
        .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(
        text = title,
        color = fgColor,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
      )
      if (!subtitle.isNullOrBlank()) {
        Text(text = subtitle, color = mutedColor, style = MaterialTheme.typography.bodyMedium)
      }
    }
    if (selected) {
      Icon(Icons.Filled.Check, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
    }
  }
}

@Composable
internal fun HubManageRow(
  hub: MetroHub,
  canMoveUp: Boolean,
  canMoveDown: Boolean,
  fallbackAccent: Color,
  onOpen: () -> Unit,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
  onDelete: () -> Unit,
) {
  val hubColor = hub.accentHex?.let { Color(it) } ?: fallbackAccent
  val onSurface = MaterialTheme.colorScheme.onSurface
  val outline = MaterialTheme.colorScheme.outline
  val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .heightIn(min = 52.dp)
        .background(surfaceContainer)
        .border(1.dp, outline)
        .metroFocusIndicator(hubColor, onSurface)
        .clickable(role = Role.Button, onClick = onOpen)
        .padding(start = 12.dp, end = 4.dp)
        .testTag("settings_hub_row_${hub.id}"),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(modifier = Modifier.size(16.dp).background(hubColor))
    Spacer(modifier = Modifier.width(10.dp))
    Text(
      text = hub.name,
      color = onSurface,
      style = MaterialTheme.typography.bodyLarge,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )
    if (canMoveUp) {
      IconButton(onClick = onMoveUp, modifier = Modifier.size(48.dp)) {
        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.move_hub_earlier), tint = onSurface)
      }
    }
    if (canMoveDown) {
      IconButton(onClick = onMoveDown, modifier = Modifier.size(48.dp)) {
        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.move_hub_later), tint = onSurface)
      }
    }
    IconButton(onClick = onDelete, modifier = Modifier.size(48.dp).testTag("settings_hub_delete_${hub.id}")) {
      Icon(Icons.Filled.DeleteOutline, contentDescription = stringResource(R.string.delete_hub), tint = MaterialTheme.colorScheme.error)
    }
  }
}
