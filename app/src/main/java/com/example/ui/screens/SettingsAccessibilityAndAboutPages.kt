package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.LauncherUiState
import com.example.R
import com.example.model.MetroColorUtils
import com.example.model.ProjectAboutConfig
import com.example.ui.components.MetroCodeIcon
import com.example.ui.theme.metroFocusIndicator

@Composable
fun SettingsAccessibilityPage(
  uiState: LauncherUiState,
  fgOnBg: Color,
  mutedOnBg: Color,
  accentColor: Color,
  onToggleVibration: (Boolean) -> Unit,
  onToggleReduceMotion: (Boolean) -> Unit,
  onOpenSystemAccessibilitySettings: () -> Unit,
  onOpenSystemCaptionSettings: () -> Unit,
) {
  val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
  val outline = MaterialTheme.colorScheme.outline

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    // Group 1: Screen Reader & Vision
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text(
        text = stringResource(R.string.a11y_group_screen_reader_vision),
        color = accentColor,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.testTag("a11y_group_vision"),
      )
      Text(
        text = stringResource(R.string.a11y_vision_summary),
        color = fgOnBg,
        style = MaterialTheme.typography.bodyLarge,
      )
      if (uiState.systemAccessibilityEnabled) {
        Text(
          text = stringResource(R.string.a11y_system_a11y_active),
          color = accentColor,
          style = MaterialTheme.typography.labelLarge,
          modifier = Modifier.testTag("a11y_system_detected_label"),
        )
      }
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .background(surfaceContainer)
            .border(2.dp, accentColor)
            .padding(12.dp),
      ) {
        Text(
          text = stringResource(R.string.a11y_focus_preview),
          color = MaterialTheme.colorScheme.onSurface,
          style = MaterialTheme.typography.bodyMedium,
        )
      }

      SystemShortcutCard(
        title = stringResource(R.string.a11y_open_screen_reader),
        description = stringResource(R.string.a11y_open_screen_reader_desc),
        icon = Icons.Outlined.RecordVoiceOver,
        accentColor = accentColor,
        fgOnBg = fgOnBg,
        mutedOnBg = mutedOnBg,
        onClick = onOpenSystemAccessibilitySettings,
        testTag = "a11y_open_screen_reader_button",
      )
    }

    HorizontalDivider(color = outline)

    // Group 2: Hearing & Alerts
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text(
        text = stringResource(R.string.a11y_group_hearing_alerts),
        color = accentColor,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.testTag("a11y_group_hearing"),
      )
      Text(
        text = stringResource(R.string.a11y_visual_alerts_info),
        color = fgOnBg,
        style = MaterialTheme.typography.bodyLarge,
      )
      PreferenceSwitchRow(
        title = stringResource(R.string.a11y_vibration_title),
        description = stringResource(R.string.a11y_vibration_desc),
        checked = uiState.vibrationEnabled,
        fgColor = fgOnBg,
        mutedColor = mutedOnBg,
        accentColor = accentColor,
        onCheckedChange = onToggleVibration,
        testTag = "a11y_vibration_switch",
      )

      SystemShortcutCard(
        title = stringResource(R.string.a11y_open_system_captions),
        description = stringResource(R.string.a11y_open_system_captions_desc),
        icon = Icons.Outlined.ClosedCaption,
        accentColor = accentColor,
        fgOnBg = fgOnBg,
        mutedOnBg = mutedOnBg,
        onClick = onOpenSystemCaptionSettings,
        testTag = "a11y_open_captions_button",
      )
    }

    HorizontalDivider(color = outline)

    // Group 3: Motion & Interaction
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text(
        text = stringResource(R.string.a11y_group_motion_interaction),
        color = accentColor,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.testTag("a11y_group_motion"),
      )
      PreferenceSwitchRow(
        title = stringResource(R.string.motion),
        description = stringResource(R.string.motion_description),
        checked = uiState.reduceMotion,
        fgColor = fgOnBg,
        mutedColor = mutedOnBg,
        accentColor = accentColor,
        onCheckedChange = onToggleReduceMotion,
        testTag = "a11y_reduce_motion_switch",
      )
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(surfaceContainer)
            .border(1.dp, outline)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Icon(Icons.Outlined.TouchApp, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
        Text(
          text = stringResource(R.string.a11y_motion_interaction_info),
          color = MaterialTheme.colorScheme.onSurface,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
private fun SystemShortcutCard(
  title: String,
  description: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  accentColor: Color,
  fgOnBg: Color,
  mutedOnBg: Color,
  onClick: () -> Unit,
  testTag: String,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .border(1.dp, MaterialTheme.colorScheme.outline)
        .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = stringResource(R.string.a11y_system_settings_badge),
      color = accentColor,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
    )
    Text(
      text = description,
      color = mutedOnBg,
      style = MaterialTheme.typography.bodyMedium,
    )
    OutlinedButton(
      onClick = onClick,
      shape = RectangleShape,
      modifier =
        Modifier.fillMaxWidth()
          .heightIn(min = 48.dp)
          .metroFocusIndicator(accentColor, fgOnBg)
          .testTag(testTag),
    ) {
      Icon(icon, contentDescription = null, tint = fgOnBg, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(title, color = fgOnBg, modifier = Modifier.weight(1f))
      Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = mutedOnBg, modifier = Modifier.size(16.dp))
    }
  }
}

@Composable
fun SettingsAboutPage(
  fgOnBg: Color,
  mutedOnBg: Color,
  accentColor: Color,
  onOpenRepositoryUrl: (String) -> Unit,
) {
  val fgOnAccent = MetroColorUtils.legibleForeground(accentColor)
  val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
  val outline = MaterialTheme.colorScheme.outline

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(surfaceContainer)
          .border(1.dp, outline)
          .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Box(
        modifier = Modifier.size(64.dp).background(accentColor),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = MetroCodeIcon,
          contentDescription = null,
          tint = fgOnAccent,
          modifier = Modifier.size(34.dp).testTag("about_code_icon"),
        )
      }
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = stringResource(R.string.app_name),
          color = MaterialTheme.colorScheme.onSurface,
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Light,
          modifier = Modifier.testTag("about_app_name"),
        )
        Text(
          text = stringResource(R.string.about_version_format, BuildConfig.VERSION_NAME),
          color = accentColor,
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.testTag("about_version"),
        )
        Text(
          text = stringResource(R.string.about_created_by),
          color = mutedOnBg,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.testTag("about_creator"),
        )
      }
    }

    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(surfaceContainer)
          .border(1.dp, outline)
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = ProjectAboutConfig.FREE_USE_STATEMENT_PT,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.testTag("about_statement"),
      )
    }

    val repoUrl = ProjectAboutConfig.repositoryUrl
    if (!repoUrl.isNullOrBlank()) {
      Button(
        onClick = { onOpenRepositoryUrl(repoUrl) },
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = fgOnAccent),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("about_repo_button"),
      ) {
        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.about_open_source))
      }
    }

    val licenseName = ProjectAboutConfig.licenseName
    val licenseText = ProjectAboutConfig.licenseText
    if (!licenseName.isNullOrBlank() || !licenseText.isNullOrBlank()) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(surfaceContainer)
            .border(1.dp, outline)
            .padding(16.dp)
            .testTag("about_license_card"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = stringResource(R.string.about_license_title),
          color = accentColor,
          style = MaterialTheme.typography.titleMedium,
        )
        if (!licenseName.isNullOrBlank()) {
          Text(text = licenseName, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
        }
        if (!licenseText.isNullOrBlank()) {
          Text(text = licenseText, color = mutedOnBg, style = MaterialTheme.typography.bodyMedium)
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}
