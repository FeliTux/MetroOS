package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.LauncherUiState
import com.example.R
import com.example.model.MetroColorUtils
import com.example.ui.components.ConfirmDestructiveDialog
import com.example.ui.components.ProfileAgeDialog
import com.example.ui.components.ProfileNameDialog
import com.example.ui.theme.metroFocusIndicator

@Composable
fun SettingsProfilePage(
  uiState: LauncherUiState,
  fgOnBg: Color,
  mutedOnBg: Color,
  accentColor: Color,
  onPickProfilePhoto: () -> Unit,
  onSaveProfileName: (String) -> Unit,
  onSaveProfileAge: (String) -> Boolean,
  onClearProfileAge: () -> Unit,
  onRemoveProfilePhoto: () -> Unit,
  onToggleShowOnStart: (Boolean) -> Unit,
) {
  var showNameDialog by remember { mutableStateOf(false) }
  var showAgeDialog by remember { mutableStateOf(false) }
  var showRemovePhotoConfirm by remember { mutableStateOf(false) }

  val profile = uiState.profile
  val hasAvatar = profile.avatarPath != null
  val fgOnAccent = MetroColorUtils.legibleForeground(accentColor)
  val cardBg = MaterialTheme.colorScheme.surfaceContainer
  val outline = MaterialTheme.colorScheme.outline

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    // Portrait + Preferred Name + Optional Age Header
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(cardBg)
          .border(1.dp, outline)
          .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.size(84.dp)
            .background(accentColor)
            .border(1.dp, outline)
            .testTag("profile_avatar_box"),
        contentAlignment = Alignment.Center,
      ) {
        val bmp = uiState.profileAvatarBitmap
        if (bmp != null) {
          Image(
            bitmap = bmp,
            contentDescription = stringResource(R.string.settings_section_profile),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().testTag("profile_avatar_image"),
          )
        } else {
          Icon(
            imageVector = Icons.Outlined.Person,
            contentDescription = stringResource(R.string.settings_section_profile),
            tint = fgOnAccent,
            modifier = Modifier.size(40.dp).testTag("profile_avatar_placeholder"),
          )
        }
      }

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val displayName = profile.name.trim().ifEmpty { stringResource(R.string.profile_add_name) }
        Text(
          text = displayName,
          color = fgOnBg,
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Light,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.testTag("profile_header_name"),
        )
        val ageValue = profile.age
        if (ageValue != null) {
          Text(
            text = stringResource(R.string.profile_age_years, ageValue),
            color = mutedOnBg,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag("profile_header_age"),
          )
        }
        Text(
          text = stringResource(R.string.profile_local_privacy_note),
          color = mutedOnBg,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }

    // Primary profile actions
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Button(
        onClick = onPickProfilePhoto,
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = fgOnAccent),
        modifier =
          Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .metroFocusIndicator(accentColor, fgOnAccent)
            .testTag("profile_choose_photo_button"),
      ) {
        Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.profile_choose_photo))
      }

      OutlinedButton(
        onClick = { showNameDialog = true },
        shape = RectangleShape,
        modifier =
          Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .metroFocusIndicator(accentColor, fgOnBg)
            .testTag("profile_edit_name_button"),
      ) {
        Icon(Icons.Outlined.Badge, contentDescription = null, tint = fgOnBg, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.profile_change_name), color = fgOnBg)
      }

      OutlinedButton(
        onClick = { showAgeDialog = true },
        shape = RectangleShape,
        modifier =
          Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .metroFocusIndicator(accentColor, fgOnBg)
            .testTag("profile_edit_age_button"),
      ) {
        Icon(Icons.Outlined.Cake, contentDescription = null, tint = fgOnBg, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.profile_edit_age), color = fgOnBg)
      }

      if (hasAvatar) {
        OutlinedButton(
          onClick = { showRemovePhotoConfirm = true },
          shape = RectangleShape,
          modifier =
            Modifier.fillMaxWidth()
              .heightIn(min = 48.dp)
              .metroFocusIndicator(accentColor, MaterialTheme.colorScheme.error)
              .testTag("profile_remove_photo_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.DeleteOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.profile_remove_photo), color = MaterialTheme.colorScheme.error)
        }
      }
    }

    HorizontalDivider(color = outline)

    PreferenceSwitchRow(
      title = stringResource(R.string.profile_show_on_start),
      description = stringResource(R.string.profile_show_on_start_desc),
      checked = profile.showOnStart,
      fgColor = fgOnBg,
      mutedColor = mutedOnBg,
      accentColor = accentColor,
      onCheckedChange = onToggleShowOnStart,
      testTag = "profile_show_on_start_switch",
    )

    Spacer(modifier = Modifier.height(16.dp))
  }

  if (showNameDialog) {
    ProfileNameDialog(
      initialName = profile.name,
      accentColor = accentColor,
      onConfirm = { newName ->
        showNameDialog = false
        onSaveProfileName(newName)
      },
      onDismiss = { showNameDialog = false },
    )
  }

  if (showAgeDialog) {
    ProfileAgeDialog(
      initialAge = profile.age,
      accentColor = accentColor,
      onConfirmRawAge = onSaveProfileAge,
      onClearAge = onClearProfileAge,
      onDismiss = { showAgeDialog = false },
    )
  }

  if (showRemovePhotoConfirm) {
    ConfirmDestructiveDialog(
      title = stringResource(R.string.profile_remove_photo_confirm),
      message = stringResource(R.string.profile_remove_photo_confirm_msg),
      confirmLabel = stringResource(R.string.profile_remove_photo),
      accentColor = accentColor,
      onConfirm = {
        showRemovePhotoConfirm = false
        onRemoveProfilePhoto()
      },
      onDismiss = { showRemovePhotoConfirm = false },
    )
  }
}
