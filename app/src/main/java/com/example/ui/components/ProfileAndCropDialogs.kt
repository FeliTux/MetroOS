package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.PendingProfileCrop
import com.example.R
import com.example.model.MetroColorUtils
import com.example.ui.theme.metroFocusIndicator
import kotlin.math.max

@Composable
fun ProfileCropDialog(
  pending: PendingProfileCrop,
  accentColor: Color,
  onConfirm: (normCenterX: Float, normCenterY: Float, zoom: Float) -> Unit,
  onCancel: () -> Unit,
) {
  var zoom by rememberSaveable(pending.uri.toString()) { mutableFloatStateOf(1f) }
  var normCenterX by rememberSaveable(pending.uri.toString()) { mutableFloatStateOf(0.5f) }
  var normCenterY by rememberSaveable(pending.uri.toString()) { mutableFloatStateOf(0.5f) }

  val fgOnAccent = MetroColorUtils.legibleForeground(accentColor)
  val surfaceBg = MaterialTheme.colorScheme.surfaceContainerHigh
  val onSurface = MaterialTheme.colorScheme.onSurface
  val muted = MaterialTheme.colorScheme.onSurfaceVariant

  Dialog(
    onDismissRequest = onCancel,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      color = surfaceBg,
      contentColor = onSurface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      shape = RectangleShape,
      modifier =
        Modifier.fillMaxWidth(0.92f)
          .heightIn(max = 620.dp)
          .imePadding()
          .testTag("profile_crop_dialog"),
    ) {
      Column(
        modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = stringResource(R.string.profile_crop_title),
          color = onSurface,
          style = MaterialTheme.typography.titleLarge,
        )
        Text(
          text = stringResource(R.string.profile_crop_hint),
          color = muted,
          style = MaterialTheme.typography.bodyMedium,
        )

        Box(
          modifier =
            Modifier.size(220.dp)
              .align(Alignment.CenterHorizontally)
              .background(MaterialTheme.colorScheme.surfaceContainer)
              .border(2.dp, accentColor)
              .clipToBounds()
              .testTag("profile_crop_preview"),
          contentAlignment = Alignment.Center,
        ) {
          val bmp = pending.previewBitmap
          if (bmp != null) {
            val baseScale =
              if (bmp.width > 0 && bmp.height > 0) {
                max(1f, max(bmp.width, bmp.height).toFloat() / minOf(bmp.width, bmp.height).toFloat())
              } else {
                1f
              }
            val effectiveZoom = zoom * baseScale
            Image(
              bitmap = bmp,
              contentDescription = stringResource(R.string.profile_crop_title),
              contentScale = ContentScale.Crop,
              modifier =
                Modifier.fillMaxSize().graphicsLayer {
                  scaleX = zoom
                  scaleY = zoom
                  translationX = (0.5f - normCenterX) * size.width * (effectiveZoom - 1f).coerceAtLeast(0.4f)
                  translationY = (0.5f - normCenterY) * size.height * (effectiveZoom - 1f).coerceAtLeast(0.4f)
                },
            )
          } else {
            CircularProgressIndicator(color = accentColor)
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = stringResource(R.string.profile_crop_zoom),
            color = onSurface,
            style = MaterialTheme.typography.labelLarge,
          )
          Slider(
            value = zoom,
            onValueChange = { zoom = it },
            valueRange = 1f..2.5f,
            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
            modifier = Modifier.testTag("profile_crop_zoom_slider"),
          )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = stringResource(R.string.profile_crop_horizontal),
            color = onSurface,
            style = MaterialTheme.typography.labelLarge,
          )
          Slider(
            value = normCenterX,
            onValueChange = { normCenterX = it },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
            modifier = Modifier.testTag("profile_crop_x_slider"),
          )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = stringResource(R.string.profile_crop_vertical),
            color = onSurface,
            style = MaterialTheme.typography.labelLarge,
          )
          Slider(
            value = normCenterY,
            onValueChange = { normCenterY = it },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
            modifier = Modifier.testTag("profile_crop_y_slider"),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(
            onClick = onCancel,
            enabled = !pending.isSaving,
            shape = RectangleShape,
            modifier =
              Modifier.heightIn(min = 48.dp)
                .metroFocusIndicator(accentColor, onSurface)
                .testTag("profile_crop_cancel"),
          ) {
            Text(stringResource(R.string.cancel), color = muted)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = { onConfirm(normCenterX, normCenterY, zoom) },
            enabled = !pending.isSaving && pending.previewBitmap != null,
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = fgOnAccent),
            modifier =
              Modifier.heightIn(min = 48.dp)
                .metroFocusIndicator(accentColor, fgOnAccent)
                .testTag("profile_crop_confirm"),
          ) {
            Text(stringResource(R.string.profile_crop_confirm))
          }
        }
      }
    }
  }
}

@Composable
fun ProfileNameDialog(
  initialName: String,
  accentColor: Color,
  onConfirm: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var name by rememberSaveable { mutableStateOf(initialName) }
  val fgOnAccent = MetroColorUtils.legibleForeground(accentColor)
  val onSurface = MaterialTheme.colorScheme.onSurface

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RectangleShape,
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    titleContentColor = onSurface,
    textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    title = { Text(stringResource(R.string.profile_change_name)) },
    text = {
      MetroTextInputBox(
        value = name,
        onValueChange = { name = it },
        placeholder = stringResource(R.string.profile_name_label),
        accentColor = accentColor,
        modifier = Modifier.fillMaxWidth(),
        inputTestTag = "profile_name_input",
      )
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(name) },
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = fgOnAccent),
        modifier =
          Modifier.heightIn(min = 48.dp)
            .metroFocusIndicator(accentColor, fgOnAccent)
            .testTag("profile_name_confirm"),
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
fun ProfileAgeDialog(
  initialAge: Int?,
  accentColor: Color,
  onConfirmRawAge: (String) -> Boolean,
  onClearAge: () -> Unit,
  onDismiss: () -> Unit,
) {
  var ageText by rememberSaveable { mutableStateOf(initialAge?.toString().orEmpty()) }
  var showInvalid by remember { mutableStateOf(false) }
  val fgOnAccent = MetroColorUtils.legibleForeground(accentColor)
  val onSurface = MaterialTheme.colorScheme.onSurface

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RectangleShape,
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    titleContentColor = onSurface,
    textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    title = { Text(stringResource(R.string.profile_edit_age)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MetroTextInputBox(
          value = ageText,
          onValueChange = {
            ageText = it.filter { ch -> ch.isDigit() }.take(3)
            showInvalid = false
          },
          placeholder = stringResource(R.string.profile_age_label),
          accentColor = accentColor,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
          inputTestTag = "profile_age_input",
        )
        if (showInvalid) {
          Text(
            text = stringResource(R.string.profile_invalid_age),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelMedium,
          )
        }
        if (initialAge != null || ageText.isNotEmpty()) {
          OutlinedButton(
            onClick = {
              onClearAge()
              onDismiss()
            },
            shape = RectangleShape,
            modifier =
              Modifier.heightIn(min = 48.dp)
                .metroFocusIndicator(accentColor, onSurface)
                .testTag("profile_age_clear_button"),
          ) {
            Text(stringResource(R.string.profile_clear_age), color = onSurface)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (onConfirmRawAge(ageText)) {
            onDismiss()
          } else {
            showInvalid = true
          }
        },
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = fgOnAccent),
        modifier =
          Modifier.heightIn(min = 48.dp)
            .metroFocusIndicator(accentColor, fgOnAccent)
            .testTag("profile_age_confirm"),
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
fun ConfirmDestructiveDialog(
  title: String,
  message: String,
  confirmLabel: String,
  accentColor: Color,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  val onSurface = MaterialTheme.colorScheme.onSurface
  val errContainer = MaterialTheme.colorScheme.error
  val onErrContainer = MaterialTheme.colorScheme.onError

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RectangleShape,
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    titleContentColor = onSurface,
    textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    title = { Text(title) },
    text = { Text(message) },
    confirmButton = {
      Button(
        onClick = onConfirm,
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(containerColor = errContainer, contentColor = onErrContainer),
        modifier =
          Modifier.heightIn(min = 48.dp)
            .metroFocusIndicator(accentColor, onErrContainer)
            .testTag("confirm_destructive_accept"),
      ) {
        Text(confirmLabel)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        shape = RectangleShape,
        modifier =
          Modifier.heightIn(min = 48.dp)
            .metroFocusIndicator(accentColor, onSurface)
            .testTag("confirm_destructive_cancel"),
      ) {
        Text(stringResource(R.string.cancel), color = onSurface)
      }
    },
  )
}
