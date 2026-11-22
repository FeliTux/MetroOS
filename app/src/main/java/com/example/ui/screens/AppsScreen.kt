package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LauncherUiState
import com.example.R
import com.example.data.LoadedAppIcon
import com.example.model.AppTargetKey
import com.example.model.InstalledApp
import com.example.model.MetroColorUtils
import com.example.ui.components.AppIconImage
import com.example.ui.theme.metroFocusIndicator
import kotlinx.coroutines.launch

private sealed interface CatalogRow {
  data class SectionHeader(val letter: String, val itemIndexHint: Int) : CatalogRow
  data class AppEntry(val app: InstalledApp) : CatalogRow
}

@Composable
fun AppsScreen(
  uiState: LauncherUiState,
  focusSearchOnOpen: Boolean,
  loadAppIcon: suspend (AppTargetKey) -> LoadedAppIcon,
  onQueryChange: (String) -> Unit,
  onClearQuery: () -> Unit,
  onLaunchApp: (InstalledApp) -> Unit,
  onPinApp: (InstalledApp) -> Unit,
  onOpenContextMenu: (InstalledApp) -> Unit,
  onLocateExistingTile: (String) -> Unit,
  onDismissDuplicatePin: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenProfile: () -> Unit = onOpenSettings,
  onBack: () -> Unit,
) {
  val searchFocusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()

  var showOverflowMenu by remember { mutableStateOf(false) }
  var showAlphabetJump by remember { mutableStateOf(false) }

  val bgColor = MaterialTheme.colorScheme.background
  val fgOnBg = MetroColorUtils.legibleForeground(bgColor)
  val mutedOnBg = MetroColorUtils.mutedForeground(bgColor)
  val accentColor = uiState.effectiveAccentColor
  val fgOnAccent = MetroColorUtils.legibleForeground(accentColor)
  val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
  val menuSurface = MaterialTheme.colorScheme.surfaceContainerHigh
  val onSurface = MaterialTheme.colorScheme.onSurface
  val mutedOnSurface = MaterialTheme.colorScheme.onSurfaceVariant
  val outline = MaterialTheme.colorScheme.outline

  BackHandler {
    when {
      showAlphabetJump -> showAlphabetJump = false
      else -> {
        keyboardController?.hide()
        onBack()
      }
    }
  }

  LaunchedEffect(focusSearchOnOpen) {
    if (focusSearchOnOpen) {
      runCatching { searchFocusRequester.requestFocus() }
      keyboardController?.show()
    } else {
      keyboardController?.hide()
    }
  }

  val catalogRows =
    remember(uiState.filteredApps) {
      val rows = mutableListOf<CatalogRow>()
      var currentSection: String? = null
      uiState.filteredApps.forEach { app ->
        if (app.sortSection != currentSection) {
          currentSection = app.sortSection
          rows.add(CatalogRow.SectionHeader(app.sortSection, rows.size))
        }
        rows.add(CatalogRow.AppEntry(app))
      }
      rows
    }

  val availableSections =
    remember(catalogRows) {
      catalogRows.filterIsInstance<CatalogRow.SectionHeader>()
    }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(bgColor)
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
        .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
        .padding(horizontal = 16.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.apps),
        color = fgOnBg,
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.Light,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f).testTag("apps_title"),
      )

      Box {
        IconButton(
          onClick = { showOverflowMenu = true },
          modifier =
            Modifier.size(48.dp)
              .metroFocusIndicator(accentColor, fgOnBg)
              .testTag("apps_overflow_button"),
        ) {
          Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options), tint = fgOnBg)
        }
        DropdownMenu(
          expanded = showOverflowMenu,
          onDismissRequest = { showOverflowMenu = false },
          modifier = Modifier.background(menuSurface),
        ) {
          DropdownMenuItem(
            text = { Text(stringResource(R.string.settings_section_profile), color = onSurface) },
            onClick = {
              showOverflowMenu = false
              keyboardController?.hide()
              onOpenProfile()
            },
            modifier = Modifier.testTag("menu_profile_item"),
          )
          DropdownMenuItem(
            text = { Text(stringResource(R.string.settings), color = onSurface) },
            onClick = {
              showOverflowMenu = false
              keyboardController?.hide()
              onOpenSettings()
            },
            modifier = Modifier.testTag("menu_settings_item"),
          )
        }
      }
    }

    Row(
      modifier =
        Modifier.fillMaxWidth()
          .heightIn(min = 48.dp)
          .background(surfaceColor)
          .border(1.dp, outline)
          .padding(start = 12.dp, end = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(Icons.Filled.Search, contentDescription = null, tint = mutedOnSurface, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.width(10.dp))
      Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
        if (uiState.searchQuery.isEmpty()) {
          Text(
            text = stringResource(R.string.search_hint),
            color = mutedOnSurface,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        BasicTextField(
          value = uiState.searchQuery,
          onValueChange = onQueryChange,
          singleLine = true,
          textStyle = TextStyle(color = onSurface, fontSize = 16.sp, fontWeight = FontWeight.Normal),
          cursorBrush = SolidColor(accentColor),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions =
            KeyboardActions(
              onSearch = {
                uiState.filteredApps.firstOrNull()?.let { firstApp -> onLaunchApp(firstApp) }
              }
            ),
          modifier =
            Modifier.fillMaxWidth()
              .focusRequester(searchFocusRequester)
              .semantics { contentDescription = "Buscar aplicativos" }
              .testTag("apps_search_field"),
        )
      }
      if (uiState.searchQuery.isNotEmpty()) {
        IconButton(
          onClick = {
            onClearQuery()
            runCatching { searchFocusRequester.requestFocus() }
          },
          modifier = Modifier.size(48.dp).testTag("clear_search_button"),
        ) {
          Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear), tint = onSurface)
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      if (uiState.filteredApps.isEmpty()) {
        Column(
          modifier = Modifier.align(Alignment.TopStart).padding(top = 24.dp).testTag("no_results_container"),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = stringResource(R.string.no_results),
            color = mutedOnBg,
            style = MaterialTheme.typography.titleMedium,
          )
          if (uiState.searchQuery.isNotEmpty()) {
            Button(
              onClick = {
                onClearQuery()
                runCatching { searchFocusRequester.requestFocus() }
              },
              shape = RectangleShape,
              colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = fgOnAccent),
              modifier = Modifier.heightIn(min = 48.dp).testTag("empty_clear_search_button"),
            ) {
              Text(stringResource(R.string.clear))
            }
          }
        }
      } else {
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize().testTag("apps_catalog_list"),
          contentPadding = PaddingValues(bottom = 24.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          items(
            items = catalogRows,
            key = { row ->
              when (row) {
                is CatalogRow.SectionHeader -> "sec_${row.letter}"
                is CatalogRow.AppEntry -> "app_${row.app.key.stableKey}"
              }
            },
          ) { row ->
            when (row) {
              is CatalogRow.SectionHeader -> {
                SectionBadgeRow(
                  letter = row.letter,
                  accentColor = accentColor,
                  focusContrast = fgOnBg,
                  onClick = { showAlphabetJump = true },
                )
              }
              is CatalogRow.AppEntry -> {
                val app = row.app
                AppCatalogRow(
                  app = app,
                  accentColor = accentColor,
                  fgOnAccent = fgOnAccent,
                  fgOnBg = fgOnBg,
                  mutedOnBg = mutedOnBg,
                  iconCacheGeneration = uiState.iconCacheGeneration,
                  loadAppIcon = loadAppIcon,
                  onLaunch = { onLaunchApp(app) },
                  onOpenOptions = { onOpenContextMenu(app) },
                  onPinAction = { onPinApp(app) },
                )
              }
            }
          }
        }
      }

      if (showAlphabetJump && availableSections.isNotEmpty()) {
        AlphabetJumpOverlay(
          sections = availableSections,
          accentColor = accentColor,
          fgOnAccent = fgOnAccent,
          backgroundColor = bgColor,
          onSelectSection = { header ->
            showAlphabetJump = false
            scope.launch { listState.scrollToItem(header.itemIndexHint) }
          },
          onDismiss = { showAlphabetJump = false },
        )
      }
    }
  }

  val duplicatePrompt = uiState.duplicatePinPrompt
  if (duplicatePrompt != null) {
    val (tileId, appLabel) = duplicatePrompt
    AlertDialog(
      onDismissRequest = onDismissDuplicatePin,
      shape = RectangleShape,
      containerColor = menuSurface,
      titleContentColor = onSurface,
      textContentColor = mutedOnSurface,
      title = { Text(appLabel) },
      text = { Text(stringResource(R.string.already_pinned)) },
      confirmButton = {
        Button(
          onClick = { onLocateExistingTile(tileId) },
          shape = RectangleShape,
          colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = fgOnAccent),
          modifier = Modifier.heightIn(min = 48.dp).testTag("locate_tile_button"),
        ) {
          Text(stringResource(R.string.find_tile))
        }
      },
      dismissButton = {
        TextButton(onClick = onDismissDuplicatePin, shape = RectangleShape, modifier = Modifier.heightIn(min = 48.dp)) {
          Text(stringResource(R.string.cancel), color = onSurface)
        }
      },
    )
  }
}

@Composable
private fun SectionBadgeRow(
  letter: String,
  accentColor: Color,
  focusContrast: Color,
  onClick: () -> Unit,
) {
  val jumpDescription = stringResource(R.string.jump_to_letter, letter)
  Box(
    modifier =
      Modifier.padding(top = 10.dp, bottom = 4.dp)
        .size(48.dp)
        .metroFocusIndicator(accentColor, focusContrast)
        .clickable(role = Role.Button, onClick = onClick)
        .semantics { contentDescription = jumpDescription }
        .testTag("section_badge_$letter"),
    contentAlignment = Alignment.CenterStart,
  ) {
    Box(
      modifier = Modifier.size(36.dp).border(1.dp, accentColor),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = letter,
        color = accentColor,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Light,
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppCatalogRow(
  app: InstalledApp,
  accentColor: Color,
  fgOnAccent: Color,
  fgOnBg: Color,
  mutedOnBg: Color,
  iconCacheGeneration: Long,
  loadAppIcon: suspend (AppTargetKey) -> LoadedAppIcon,
  onLaunch: () -> Unit,
  onOpenOptions: () -> Unit,
  onPinAction: () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pinLabel = stringResource(R.string.pin)
  val moreOptionsLabel = stringResource(R.string.more_options)
  val workBadgeText = stringResource(R.string.work_profile_badge)
  val pausedText = stringResource(R.string.work_profile_paused)
  val fullA11y =
    when {
      app.isProfilePaused -> "${app.label}, $pausedText"
      app.isWorkProfile -> "${app.label}, $workBadgeText"
      else -> app.label
    }

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .heightIn(min = 56.dp)
        .alpha(if (app.isProfilePaused) 0.55f else 1f)
        .metroFocusIndicator(accentColor, fgOnBg)
        .onPreviewKeyEvent { keyEvent ->
          if (
            keyEvent.type == KeyEventType.KeyUp &&
              (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
          ) {
            onLaunch()
            true
          } else {
            false
          }
        }
        .combinedClickable(
          interactionSource = interactionSource,
          indication = ripple(bounded = true, color = fgOnBg),
          role = Role.Button,
          onClick = onLaunch,
          onLongClick = onOpenOptions,
        )
        .semantics {
          contentDescription = fullA11y
          customActions =
            listOf(
              CustomAccessibilityAction(pinLabel) {
                onPinAction()
                true
              },
              CustomAccessibilityAction(moreOptionsLabel) {
                onOpenOptions()
                true
              },
            )
        }
        .padding(vertical = 6.dp)
        .testTag("app_row_${app.key.stableKey}"),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier.size(44.dp).background(accentColor),
      contentAlignment = Alignment.Center,
    ) {
      AppIconImage(
        targetKey = app.key,
        fallbackLabel = app.label,
        iconCacheGeneration = iconCacheGeneration,
        loadAppIcon = loadAppIcon,
        iconSize = 34.dp,
        foregroundFallbackColor = fgOnAccent,
        testTagPrefix = "app_row_icon",
      )
    }
    Spacer(modifier = Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = app.label,
        color = fgOnBg,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Normal,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      if (app.isWorkProfile) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(Icons.Outlined.WorkOutline, contentDescription = null, tint = mutedOnBg, modifier = Modifier.size(13.dp))
          Text(
            text = if (app.isProfilePaused) pausedText else workBadgeText,
            color = mutedOnBg,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
    IconButton(
      onClick = onOpenOptions,
      modifier =
        Modifier.size(48.dp)
          .metroFocusIndicator(accentColor, fgOnBg)
          .testTag("app_row_more_${app.key.stableKey}"),
    ) {
      Icon(
        imageVector = Icons.Filled.MoreVert,
        contentDescription = "$moreOptionsLabel: ${app.label}",
        tint = mutedOnBg,
      )
    }
  }
}

@Composable
private fun AlphabetJumpOverlay(
  sections: List<CatalogRow.SectionHeader>,
  accentColor: Color,
  fgOnAccent: Color,
  backgroundColor: Color,
  onSelectSection: (CatalogRow.SectionHeader) -> Unit,
  onDismiss: () -> Unit,
) {
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(backgroundColor.copy(alpha = 0.96f))
        .clickable(onClick = onDismiss)
        .padding(16.dp)
        .testTag("alphabet_jump_overlay")
  ) {
    LazyVerticalGrid(
      columns = GridCells.Adaptive(minSize = 64.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.align(Alignment.Center),
    ) {
      items(sections, key = { it.letter }) { header ->
        Box(
          modifier =
            Modifier.size(64.dp)
              .background(accentColor)
              .metroFocusIndicator(MaterialTheme.colorScheme.onBackground, fgOnAccent)
              .clickable(role = Role.Button) { onSelectSection(header) },
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = header.letter,
            color = fgOnAccent,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Light,
          )
        }
      }
    }
  }
}
