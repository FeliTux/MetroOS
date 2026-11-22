package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.LauncherAlert
import com.example.R
import com.example.model.MetroColorUtils
import com.example.ui.theme.metroFocusIndicator
import kotlinx.coroutines.delay

@Composable
fun VisualAlertBanner(
  alert: LauncherAlert?,
  accentColor: Color,
  onAction: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (alert == null) return

  LaunchedEffect(alert.id, alert.isError) {
    if (!alert.isError) {
      delay(4_500L)
      onDismiss()
    }
  }

  val containerColor =
    if (alert.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceContainerHigh
  val contentColor =
    if (alert.isError) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface
  val borderColor =
    if (alert.isError) contentColor.copy(alpha = 0.7f) else accentColor
  val focusColor = MetroColorUtils.legibleForeground(containerColor)

  Surface(
    color = containerColor,
    contentColor = contentColor,
    border = BorderStroke(2.dp, borderColor),
    shape = RectangleShape,
    shadowElevation = 6.dp,
    modifier =
      modifier
        .widthIn(max = 560.dp)
        .fillMaxWidth()
        .semantics {
          liveRegion = if (alert.isError) LiveRegionMode.Assertive else LiveRegionMode.Polite
        }
        .testTag("visual_alert_banner"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = if (alert.isError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircleOutline,
        contentDescription = null,
        tint = if (alert.isError) contentColor else accentColor,
        modifier = Modifier.size(22.dp),
      )

      Text(
        text = alert.message,
        color = contentColor,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )

      val actionLabel = alert.actionLabel
      if (!actionLabel.isNullOrBlank()) {
        TextButton(
          onClick = onAction,
          shape = RectangleShape,
          modifier =
            Modifier.heightIn(min = 48.dp)
              .metroFocusIndicator(accentColor, focusColor)
              .testTag("visual_alert_action"),
        ) {
          Text(
            text = actionLabel,
            color = if (alert.isError) contentColor else accentColor,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }

      IconButton(
        onClick = onDismiss,
        modifier =
          Modifier.size(48.dp)
            .metroFocusIndicator(accentColor, focusColor)
            .testTag("visual_alert_dismiss"),
      ) {
        Icon(
          imageVector = Icons.Filled.Close,
          contentDescription = stringResource(R.string.a11y_dismiss_alert),
          tint = contentColor,
        )
      }
    }
  }
}
