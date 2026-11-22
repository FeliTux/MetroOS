package com.example

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppThemeMode
import com.example.model.LauncherSurface
import com.example.model.SettingsSection
import com.example.ui.components.AddLiveTileDialog
import com.example.ui.components.AddToHubDialog
import com.example.ui.components.AppContextMenuDialog
import com.example.ui.components.AppRepairDialog
import com.example.ui.components.CreateHubDialog
import com.example.ui.components.CreateOrEditFolderAppsDialog
import com.example.ui.components.FolderNameDialog
import com.example.ui.components.HubColorDialog
import com.example.ui.components.NoteEditorDialog
import com.example.ui.components.PinToHubDialog
import com.example.ui.components.ProfileCropDialog
import com.example.ui.components.RenameHubDialog
import com.example.ui.components.SavedNotesDialog
import com.example.ui.components.VisualAlertBanner
import com.example.ui.screens.AppsScreen
import com.example.ui.screens.HubScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StartScreen
import com.example.ui.theme.MetroHomeTheme

class MainActivity : ComponentActivity() {

  private val viewModel: LauncherViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
    )
    super.onCreate(savedInstanceState)

    setContent {
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()
      val context = LocalContext.current
      val lifecycleOwner = LocalLifecycleOwner.current

      var pendingWidgetId by remember { mutableStateOf<Int?>(null) }
      var pendingWidgetHubId by remember { mutableStateOf<String?>(null) }
      var showStartLiveTilePicker by remember { mutableStateOf(false) }

      val systemDark = isSystemInDarkTheme()
      val isDarkTheme =
        when (uiState.themeMode) {
          AppThemeMode.LIGHT -> false
          AppThemeMode.DARK -> true
          AppThemeMode.SYSTEM -> systemDark
        }

      LaunchedEffect(isDarkTheme) {
        enableEdgeToEdge(
          statusBarStyle =
            if (isDarkTheme) {
              SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
              SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            },
          navigationBarStyle =
            if (isDarkTheme) {
              SystemBarStyle.dark(android.graphics.Color.BLACK)
            } else {
              SystemBarStyle.light(
                android.graphics.Color.parseColor("#FFF6F6F6"),
                android.graphics.Color.parseColor("#FFE0E0E0"),
              )
            },
        )
      }

      val singlePhotoPickerLauncher =
        rememberLauncherForActivityResult(
          contract = ActivityResultContracts.PickVisualMedia(),
          onResult = { uri -> viewModel.onPhotoPicked(uri) },
        )

      val multiPhotoPickerLauncher =
        rememberLauncherForActivityResult(
          contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 12),
          onResult = { uris -> viewModel.onPhotosPicked(uris) },
        )

      val profilePhotoPickerLauncher =
        rememberLauncherForActivityResult(
          contract = ActivityResultContracts.PickVisualMedia(),
          onResult = { uri -> viewModel.onProfilePhotoPicked(uri) },
        )

      val profilePhotoFallbackLauncher =
        rememberLauncherForActivityResult(
          contract = ActivityResultContracts.GetContent(),
          onResult = { uri -> viewModel.onProfilePhotoPicked(uri) },
        )

      val calendarPermissionLauncher =
        rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission(),
          onResult = { granted -> viewModel.setCalendarAccessEnabled(granted) },
        )

      val homeRoleLauncher =
        rememberLauncherForActivityResult(
          contract = ActivityResultContracts.StartActivityForResult(),
          onResult = { viewModel.onResumeLifecycle(isDefaultHomeApp()) },
        )

      val widgetConfigureLauncher =
        rememberLauncherForActivityResult(
          contract = ActivityResultContracts.StartActivityForResult(),
          onResult = { result ->
            val widgetId = pendingWidgetId
            val targetHub = pendingWidgetHubId
            pendingWidgetId = null
            pendingWidgetHubId = null
            if (widgetId != null) {
              if (result.resultCode == Activity.RESULT_OK) {
                val label =
                  viewModel.appWidgetController.getProviderInfo(widgetId)?.loadLabel(packageManager).orEmpty()
                viewModel.addSystemWidgetTile(widgetId, label, targetHub)
              } else {
                viewModel.appWidgetController.deleteAppWidgetId(widgetId)
              }
            }
          },
        )

      val widgetPickLauncher =
        rememberLauncherForActivityResult(
          contract = ActivityResultContracts.StartActivityForResult(),
          onResult = { result ->
            val allocated = pendingWidgetId
            val targetHub = pendingWidgetHubId
            val returnedId =
              result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                ?: allocated
                ?: AppWidgetManager.INVALID_APPWIDGET_ID
            if (result.resultCode == Activity.RESULT_OK && returnedId != AppWidgetManager.INVALID_APPWIDGET_ID) {
              val configureIntent = viewModel.appWidgetController.buildConfigureIntent(returnedId)
              if (configureIntent != null) {
                pendingWidgetId = returnedId
                pendingWidgetHubId = targetHub
                runCatching { widgetConfigureLauncher.launch(configureIntent) }.onFailure {
                  val label = viewModel.appWidgetController.getProviderInfo(returnedId)?.loadLabel(packageManager).orEmpty()
                  viewModel.addSystemWidgetTile(returnedId, label, targetHub)
                  pendingWidgetId = null
                  pendingWidgetHubId = null
                }
              } else {
                val label = viewModel.appWidgetController.getProviderInfo(returnedId)?.loadLabel(packageManager).orEmpty()
                viewModel.addSystemWidgetTile(returnedId, label, targetHub)
                pendingWidgetId = null
                pendingWidgetHubId = null
              }
            } else {
              if (returnedId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                viewModel.appWidgetController.deleteAppWidgetId(returnedId)
              } else if (allocated != null) {
                viewModel.appWidgetController.deleteAppWidgetId(allocated)
              }
              pendingWidgetId = null
              pendingWidgetHubId = null
            }
          },
        )

      DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
          when (event) {
            Lifecycle.Event.ON_START -> viewModel.onStartLifecycle()
            Lifecycle.Event.ON_RESUME -> viewModel.onResumeLifecycle(isDefaultHomeApp())
            Lifecycle.Event.ON_STOP -> viewModel.onStopLifecycle()
            else -> Unit
          }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
      }

      val requestHomeRole: () -> Unit = {
        val launchedRole =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (
              roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
              runCatching {
                homeRoleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
                true
              }.getOrDefault(false)
            } else {
              false
            }
          } else {
            false
          }
        if (!launchedRole) {
          openSystemHomeSettings()
        }
      }

      val launchSinglePhotoPicker: () -> Unit = {
        singlePhotoPickerLauncher.launch(
          PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
      }

      val launchMultiPhotoPicker: () -> Unit = {
        multiPhotoPickerLauncher.launch(
          PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
      }

      val launchProfilePhotoPicker: () -> Unit = {
        val canUsePhotoPicker =
          runCatching { ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context) }.getOrDefault(true)
        if (canUsePhotoPicker) {
          runCatching {
            profilePhotoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          }.onFailure {
            runCatching { profilePhotoFallbackLauncher.launch("image/*") }
              .onFailure { viewModel.onProfilePhotoPicked(null) }
          }
        } else {
          runCatching { profilePhotoFallbackLauncher.launch("image/*") }
            .onFailure { viewModel.onProfilePhotoPicked(null) }
        }
      }

      val launchWidgetPicker: (String?) -> Unit = { hubId ->
        val widgetId = viewModel.appWidgetController.allocateAppWidgetId()
        if (widgetId != null) {
          pendingWidgetId = widgetId
          pendingWidgetHubId = hubId
          runCatching {
            widgetPickLauncher.launch(viewModel.appWidgetController.buildPickWidgetIntent(widgetId))
          }.onFailure {
            viewModel.appWidgetController.deleteAppWidgetId(widgetId)
            pendingWidgetId = null
            pendingWidgetHubId = null
          }
        }
      }

      val requestCalendarToggle: (Boolean) -> Unit = { enable ->
        if (!enable) {
          viewModel.setCalendarAccessEnabled(false)
        } else {
          val granted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
              PackageManager.PERMISSION_GRANTED
          if (granted) {
            viewModel.setCalendarAccessEnabled(true)
          } else {
            calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
          }
        }
      }

      MetroHomeTheme(
        isDarkTheme = isDarkTheme,
        accentColor = uiState.effectiveAccentColor,
        backgroundColor = uiState.effectiveBackgroundColor,
      ) {
        MetroLauncherRoot(
          uiState = uiState,
          viewModel = viewModel,
          showStartLiveTilePicker = showStartLiveTilePicker,
          onShowStartLiveTilePicker = { showStartLiveTilePicker = it },
          onLaunchSinglePhotoPicker = launchSinglePhotoPicker,
          onLaunchMultiPhotoPicker = launchMultiPhotoPicker,
          onLaunchProfilePhotoPicker = launchProfilePhotoPicker,
          onRequestCalendarToggle = requestCalendarToggle,
          onOpenSystemNotificationAccess = { openNotificationAccessSettings() },
          onOpenSystemAccessibilitySettings = { openAccessibilitySettings() },
          onOpenSystemCaptionSettings = { openCaptionSettings() },
          onOpenRepositoryUrl = { url -> openExternalUrl(url) },
          onLaunchWidgetPicker = launchWidgetPicker,
          onRequestHomeRole = requestHomeRole,
          onOpenSystemHomeSettings = { openSystemHomeSettings() },
          resolveTileLabel = { tile ->
            viewModel.resolveTileLabel(tile, uiState.installedApps, context)
          },
        )
      }
    }
  }

  override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    viewModel.onTrimMemory(level)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (intent.hasCategory(Intent.CATEGORY_HOME)) {
      viewModel.onHomeIntentReceived()
    }
  }

  private fun isDefaultHomeApp(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val roleManager = getSystemService(RoleManager::class.java)
      if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
        return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
      }
    }
    val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolved = packageManager.resolveActivity(homeIntent, 0)
    return resolved?.activityInfo?.packageName == packageName
  }

  private fun openSystemHomeSettings() {
    val homeSettings = Intent(Settings.ACTION_HOME_SETTINGS)
    val fallbackSettings = Intent(Settings.ACTION_SETTINGS)
    runCatching { startActivity(homeSettings) }
      .onFailure { runCatching { startActivity(fallbackSettings) } }
  }

  private fun openNotificationAccessSettings() {
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    val fallback = Intent(Settings.ACTION_SETTINGS)
    runCatching { startActivity(intent) }
      .onFailure { runCatching { startActivity(fallback) } }
  }

  private fun openAccessibilitySettings() {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    val fallback = Intent(Settings.ACTION_SETTINGS)
    runCatching { startActivity(intent) }
      .onFailure { runCatching { startActivity(fallback) } }
  }

  private fun openCaptionSettings() {
    val intent = Intent(Settings.ACTION_CAPTIONING_SETTINGS)
    val a11yFallback = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    val settingsFallback = Intent(Settings.ACTION_SETTINGS)
    runCatching { startActivity(intent) }
      .onFailure {
        runCatching { startActivity(a11yFallback) }
          .onFailure { runCatching { startActivity(settingsFallback) } }
      }
  }

  private fun openExternalUrl(url: String) {
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
  }
}

@Composable
private fun MetroLauncherRoot(
  uiState: LauncherUiState,
  viewModel: LauncherViewModel,
  showStartLiveTilePicker: Boolean,
  onShowStartLiveTilePicker: (Boolean) -> Unit,
  onLaunchSinglePhotoPicker: () -> Unit,
  onLaunchMultiPhotoPicker: () -> Unit,
  onLaunchProfilePhotoPicker: () -> Unit,
  onRequestCalendarToggle: (Boolean) -> Unit,
  onOpenSystemNotificationAccess: () -> Unit,
  onOpenSystemAccessibilitySettings: () -> Unit,
  onOpenSystemCaptionSettings: () -> Unit,
  onOpenRepositoryUrl: (String) -> Unit,
  onLaunchWidgetPicker: (String?) -> Unit,
  onRequestHomeRole: () -> Unit,
  onOpenSystemHomeSettings: () -> Unit,
  resolveTileLabel: (com.example.model.StartTile) -> String,
) {
  BackHandler(enabled = (uiState.surface is LauncherSurface.Start || uiState.surface is LauncherSurface.Hub) && uiState.isEditing) {
    viewModel.handleEditBackPressed()
  }

  Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    val durationMs = if (uiState.effectiveReducedMotion) 0 else 160
    AnimatedContent(
      targetState = uiState.surface,
      transitionSpec = {
        if (durationMs == 0) {
          fadeIn(tween(0)) togetherWith fadeOut(tween(0))
        } else if (targetState is LauncherSurface.Apps && initialState is LauncherSurface.Start) {
          (slideInHorizontally(tween(durationMs)) { it / 2 } + fadeIn(tween(durationMs))) togetherWith
            (slideOutHorizontally(tween(durationMs)) { -it / 3 } + fadeOut(tween(durationMs)))
        } else if (targetState is LauncherSurface.Start && initialState is LauncherSurface.Apps) {
          (slideInHorizontally(tween(durationMs)) { -it / 2 } + fadeIn(tween(durationMs))) togetherWith
            (slideOutHorizontally(tween(durationMs)) { it / 3 } + fadeOut(tween(durationMs)))
        } else {
          fadeIn(tween(durationMs)) togetherWith fadeOut(tween(durationMs))
        }
      },
      label = "launcher_surface",
    ) { surface ->
      when (surface) {
        LauncherSurface.Start -> {
          StartScreen(
            uiState = uiState,
            appWidgetController = viewModel.appWidgetController,
            loadAppIcon = { key -> viewModel.loadAppIcon(key) },
            resolveTileLabel = resolveTileLabel,
            onSaveStartScrollPosition = { idx, offset -> viewModel.saveStartScrollPosition(idx, offset) },
            onPhotoTargetEdgeMeasured = { edge -> viewModel.updatePhotoTargetEdgePx(edge) },
            onTileTap = { tile -> viewModel.onTileTap(tile, onLaunchSinglePhotoPicker) },
            onTileLongPress = { tile ->
              if (!uiState.isEditing) {
                viewModel.enterEditMode(initialSelectedTileId = tile.id)
              } else {
                viewModel.selectTile(tile.id)
                viewModel.openMoveSheet()
              }
            },
            onOpenAccessibleMove = { tile -> viewModel.openAccessibleMoveForTile(tile) },
            onOpenAccessibleResize = { tile -> viewModel.openAccessibleResizeForTile(tile) },
            onUnpinAccessible = { tile -> viewModel.unpinTileAccessible(tile) },
            onLeftCommandClick = { viewModel.onLeftCommandAction() },
            onAppsClick = { viewModel.openApps(focusSearch = false) },
            onEditClick = { viewModel.enterEditMode() },
            onResizeClick = { viewModel.openResizeSheet() },
            onUnpinClick = { viewModel.unpinSelectedTile() },
            onDoneClick = { viewModel.commitEdits() },
            onSelectSize = { size -> viewModel.resizeSelectedTile(size) },
            onDismissResizeSheet = { viewModel.dismissResizeSheet() },
            onOpenMoveSheet = { viewModel.openMoveSheet() },
            onStepMove = { dCol, dRow -> viewModel.stepMoveSelectedTile(dCol, dRow) },
            onMoveTileTo = { id, col, row -> viewModel.moveTileOrMerge(id, col, row) },
            onDismissMoveSheet = { viewModel.dismissMoveSheet() },
            onOpenTileColorSheet = { viewModel.openTileColorSheet() },
            onSelectTileColor = { hex -> viewModel.setSelectedTileColor(hex) },
            onDismissTileColorSheet = { viewModel.dismissTileColorSheet() },
            onToggleOriginalIcon = { viewModel.toggleSelectedTileOriginalIcon() },
            onToggleLiveMode = { viewModel.toggleSelectedTileLiveMode() },
            onMediaPlayPause = { viewModel.onMediaPlayPause() },
            onMediaPrevious = { viewModel.onMediaPrevious() },
            onMediaNext = { viewModel.onMediaNext() },
            onSaveAndExitEdit = { viewModel.commitEdits() },
            onDiscardEdit = { viewModel.discardEdits() },
            onKeepEditing = { viewModel.dismissUnsavedDialog() },
            onDismissSaveError = { viewModel.dismissSaveError() },
            onRemoveStaleTile = { tile -> viewModel.removeStaleTile(tile) },
            onDismissStalePrompt = { viewModel.dismissStaleTilePrompt() },
            onPickPhoto = onLaunchSinglePhotoPicker,
            onDismissPhotoSheet = { viewModel.dismissPhotoActionSheet() },
            onLocateHandled = { viewModel.clearScrollAndHighlight() },
            onRestoreFocusHandled = { viewModel.clearRestoreFocusTileId() },
            onCollapseFolder = { viewModel.collapseExpandedFolder() },
            onLaunchFolderApp = { key -> viewModel.launchAppByKey(key) },
            onReorderFolderApp = { fid, idx, delta -> viewModel.reorderFolderApp(fid, idx, delta) },
            onMoveFolderAppToStart = { fid, key -> viewModel.moveFolderAppToStart(fid, key) },
            onRemoveAppFromFolder = { fid, key -> viewModel.removeAppFromFolder(fid, key) },
            onRenameFolder = { folder -> viewModel.openRenameFolder(folder) },
            onAddAppsToFolder = { folder -> viewModel.openAddAppsToFolder(folder) },
            onDissolveFolder = { fid -> viewModel.dissolveFolder(fid) },
            onCreateFolderClick = { viewModel.openCreateFolderDialog() },
            onCreateHubClick = { viewModel.openCreateHubDialog() },
            onAddLiveTileClick = { onShowStartLiveTilePicker(true) },
            onAddWidgetClick = { onLaunchWidgetPicker(null) },
            onQuickNoteClick = { viewModel.openBlankNoteEditor(targetHubId = null) },
            onSavedNotesClick = { viewModel.openSavedNotesSheet() },
            onOpenPersonalization = { viewModel.openSettings() },
            onOpenProfile = { viewModel.openSettings(SettingsSection.PROFILE) },
            onChangeLeftSlot = { slot -> viewModel.setLeftCommandSlot(slot) },
          )
        }
        is LauncherSurface.Hub -> {
          val hub = uiState.committedLayout.hubs.firstOrNull { it.id == surface.hubId }
          if (hub != null) {
            HubScreen(
              hub = hub,
              uiState = uiState,
              appWidgetController = viewModel.appWidgetController,
              loadAppIcon = { key -> viewModel.loadAppIcon(key) },
              resolveTileLabel = resolveTileLabel,
              onPhotoTargetEdgeMeasured = { edge -> viewModel.updatePhotoTargetEdgePx(edge) },
              onTileTap = { tile -> viewModel.onTileTap(tile, onLaunchSinglePhotoPicker) },
              onTileLongPress = { tile ->
                if (!uiState.isEditing) {
                  viewModel.enterEditMode(initialSelectedTileId = tile.id)
                } else {
                  viewModel.selectTile(tile.id)
                  viewModel.openMoveSheet()
                }
              },
              onOpenAccessibleMove = { tile -> viewModel.openAccessibleMoveForTile(tile) },
              onOpenAccessibleResize = { tile -> viewModel.openAccessibleResizeForTile(tile) },
              onUnpinAccessible = { tile -> viewModel.unpinTileAccessible(tile) },
              onBackToStart = { viewModel.navigateBackToStart() },
              onAddToHub = { viewModel.openAddToHubSheet(hub.id) },
              onEditClick = { viewModel.enterEditMode() },
              onResizeClick = { viewModel.openResizeSheet() },
              onUnpinClick = { viewModel.unpinSelectedTile() },
              onDoneClick = { viewModel.commitEdits() },
              onSelectSize = { size -> viewModel.resizeSelectedTile(size) },
              onDismissResizeSheet = { viewModel.dismissResizeSheet() },
              onOpenMoveSheet = { viewModel.openMoveSheet() },
              onStepMove = { dCol, dRow -> viewModel.stepMoveSelectedTile(dCol, dRow) },
              onMoveTileTo = { id, col, row -> viewModel.moveTileOrMerge(id, col, row) },
              onDismissMoveSheet = { viewModel.dismissMoveSheet() },
              onOpenTileColorSheet = { viewModel.openTileColorSheet() },
              onSelectTileColor = { hex -> viewModel.setSelectedTileColor(hex) },
              onDismissTileColorSheet = { viewModel.dismissTileColorSheet() },
              onToggleOriginalIcon = { viewModel.toggleSelectedTileOriginalIcon() },
              onToggleLiveMode = { viewModel.toggleSelectedTileLiveMode() },
              onMediaPlayPause = { viewModel.onMediaPlayPause() },
              onMediaPrevious = { viewModel.onMediaPrevious() },
              onMediaNext = { viewModel.onMediaNext() },
              onLocateHandled = { viewModel.clearScrollAndHighlight() },
              onRestoreFocusHandled = { viewModel.clearRestoreFocusTileId() },
              onCollapseFolder = { viewModel.collapseExpandedFolder() },
              onLaunchFolderApp = { key -> viewModel.launchAppByKey(key) },
              onReorderFolderApp = { fid, idx, delta -> viewModel.reorderFolderApp(fid, idx, delta) },
              onMoveFolderAppToStart = { fid, key -> viewModel.moveFolderAppToStart(fid, key) },
              onRemoveAppFromFolder = { fid, key -> viewModel.removeAppFromFolder(fid, key) },
              onRenameFolder = { folder -> viewModel.openRenameFolder(folder) },
              onAddAppsToFolder = { folder -> viewModel.openAddAppsToFolder(folder) },
              onDissolveFolder = { fid -> viewModel.dissolveFolder(fid) },
              onCreateFolderClick = { viewModel.openCreateFolderDialog() },
              onQuickNoteClick = { viewModel.openBlankNoteEditor(targetHubId = hub.id) },
              onSavedNotesClick = { viewModel.openSavedNotesSheet() },
              onAddLiveTileClick = { viewModel.openAddToHubSheet(hub.id) },
              onAddWidgetClick = { onLaunchWidgetPicker(hub.id) },
              onOpenPersonalization = { viewModel.openSettings() },
              onOpenProfile = { viewModel.openSettings(SettingsSection.PROFILE) },
              onRenameHub = { viewModel.openRenameHub(hub) },
              onChangeHubColor = { viewModel.openHubColorSheet(hub) },
              onPinHubToStart = { viewModel.pinHubToStart(hub) },
              onDeleteHub = { viewModel.deleteHub(hub.id) },
              onHandleEditBack = { viewModel.handleEditBackPressed() },
            )
          }
        }
        is LauncherSurface.Apps -> {
          AppsScreen(
            uiState = uiState,
            focusSearchOnOpen = surface.focusSearch,
            loadAppIcon = { key -> viewModel.loadAppIcon(key) },
            onQueryChange = { viewModel.updateSearchQuery(it) },
            onClearQuery = { viewModel.clearSearchQuery() },
            onLaunchApp = { viewModel.launchApp(it) },
            onPinApp = { viewModel.pinAppToStart(it) },
            onOpenContextMenu = { viewModel.openAppContextMenu(it) },
            onLocateExistingTile = { viewModel.locateExistingTile(it) },
            onDismissDuplicatePin = { viewModel.dismissDuplicatePinPrompt() },
            onOpenSettings = { viewModel.openSettings() },
            onOpenProfile = { viewModel.openSettings(SettingsSection.PROFILE) },
            onBack = { viewModel.navigateBackToStart() },
          )
        }
        is LauncherSurface.Settings -> {
          SettingsScreen(
            uiState = uiState,
            onSelectSection = { viewModel.navigateSettingsSection(it) },
            onSelectThemeMode = { viewModel.setThemeMode(it) },
            onSelectLightweightMode = { viewModel.setLightweightMode(it) },
            onPickProfilePhoto = onLaunchProfilePhotoPicker,
            onSaveProfileName = { viewModel.setProfileName(it) },
            onSaveProfileAge = { viewModel.setProfileAge(it) },
            onClearProfileAge = { viewModel.clearProfileAge() },
            onRemoveProfilePhoto = { viewModel.removeProfilePhoto() },
            onToggleShowProfileOnStart = { viewModel.setShowProfileOnStart(it) },
            onToggleVibration = { viewModel.setVibrationEnabled(it) },
            onOpenSystemAccessibilitySettings = onOpenSystemAccessibilitySettings,
            onOpenSystemCaptionSettings = onOpenSystemCaptionSettings,
            onOpenRepositoryUrl = onOpenRepositoryUrl,
            onRequestHomeRole = onRequestHomeRole,
            onOpenSystemHomeSettings = onOpenSystemHomeSettings,
            onSelectAccent = { viewModel.setAccent(it) },
            onApplyCustomAccentHex = { viewModel.setCustomAccentHex(it) },
            onSelectBackgroundHex = { viewModel.setBackgroundHex(it) },
            onSelectGutterDp = { viewModel.setGutterDp(it) },
            onToggleShowLabels = { viewModel.setShowLabels(it) },
            onToggleThemeThirdPartyIcons = { viewModel.setThemeThirdPartyIcons(it) },
            onReloadIcons = { viewModel.rebuildIconCache() },
            onToggleShowHomePhotos = { viewModel.setShowHomePhotos(it) },
            onPickSinglePhoto = onLaunchSinglePhotoPicker,
            onPickMultiplePhotos = onLaunchMultiPhotoPicker,
            onSelectPhotoDisplayMode = { viewModel.setPhotoDisplayMode(it) },
            onSelectPhotoCropMode = { viewModel.setPhotoCropMode(it) },
            onToggleShowPhotoCaptions = { viewModel.setShowPhotoCaptions(it) },
            onForgetSelectedPhotos = { viewModel.forgetSelectedPhotos() },
            onToggleShowPrivatePreviews = { viewModel.setShowPrivatePreviews(it) },
            onRequestCalendarAccess = onRequestCalendarToggle,
            onToggleNotificationsAccess = { viewModel.setNotificationsAccessEnabled(it) },
            onOpenSystemNotificationAccess = onOpenSystemNotificationAccess,
            onCreateHubClick = { viewModel.openCreateHubDialog() },
            onOpenHub = { viewModel.openHub(it) },
            onReorderHub = { id, delta -> viewModel.reorderHub(id, delta) },
            onDeleteHub = { viewModel.deleteHub(it) },
            onToggleLockLayout = { viewModel.setLockLayout(it) },
            onToggleReduceMotion = { viewModel.setReduceMotion(it) },
            onSelectLeftCommandSlot = { viewModel.setLeftCommandSlot(it) },
            onBack = { viewModel.navigateBackFromSettings() },
          )
        }
      }
    }

    // Folder, Note, Hub, Profile Crop & Repair Modal Dialogs
    val pendingCrop = uiState.pendingProfileCrop
    if (pendingCrop != null) {
      ProfileCropDialog(
        pending = pendingCrop,
        accentColor = uiState.effectiveAccentColor,
        onConfirm = { nx, ny, zoom -> viewModel.confirmProfileCrop(nx, ny, zoom) },
        onCancel = { viewModel.cancelProfileCrop() },
      )
    }

    if (uiState.showCreateFolderDialog) {
      CreateOrEditFolderAppsDialog(
        title = stringResource(R.string.create_folder),
        showNameField = true,
        initialName = "",
        initialSelectedKeys = emptyList(),
        installedApps = uiState.installedApps,
        accentColor = uiState.effectiveAccentForActiveSurface,
        onConfirm = { name, keys -> viewModel.createFolderFromSelection(name, keys) },
        onDismiss = { viewModel.dismissCreateFolderDialog() },
      )
    }

    if (uiState.pendingDragMergePair != null) {
      FolderNameDialog(
        title = stringResource(R.string.create_folder),
        initialName = stringResource(R.string.folder_default_name),
        accentColor = uiState.effectiveAccentForActiveSurface,
        onConfirm = { name -> viewModel.confirmMergeShortcutsIntoFolder(name) },
        onDismiss = { viewModel.dismissPendingMergeDialog() },
      )
    }

    val renameTarget = uiState.renameFolderTarget
    if (renameTarget != null) {
      FolderNameDialog(
        title = stringResource(R.string.rename_folder),
        initialName = renameTarget.name,
        accentColor = uiState.effectiveAccentForActiveSurface,
        onConfirm = { name -> viewModel.renameFolder(renameTarget.id, name) },
        onDismiss = { viewModel.dismissRenameFolder() },
      )
    }

    val addAppsTarget = uiState.addAppsFolderTarget
    if (addAppsTarget != null) {
      CreateOrEditFolderAppsDialog(
        title = stringResource(R.string.add_apps_to_folder),
        showNameField = false,
        initialName = addAppsTarget.name,
        initialSelectedKeys = addAppsTarget.appKeys,
        installedApps = uiState.installedApps,
        accentColor = uiState.effectiveAccentForActiveSurface,
        onConfirm = { _, keys -> viewModel.setFolderApps(addAppsTarget.id, keys) },
        onDismiss = { viewModel.dismissAddAppsToFolder() },
      )
    }

    val noteSession = uiState.noteEditorSession
    if (noteSession != null) {
      NoteEditorDialog(
        session = noteSession,
        accentColor = uiState.effectiveAccentForActiveSurface,
        onSave = { title, body, isChecklist, pinToSurface, size ->
          viewModel.saveNote(
            existingNoteId = noteSession.noteId,
            title = title,
            body = body,
            isChecklist = isChecklist,
            pinToSurface = pinToSurface,
            targetHubId = noteSession.targetHubId,
            preferredSize = size,
          )
        },
        onRemoveFromStart = { noteId ->
          viewModel.removeNoteFromStart(noteId, noteSession.targetHubId)
        },
        onDeleteNote = { noteId -> viewModel.deleteNoteWithUndo(noteId) },
        onDismiss = { viewModel.dismissNoteEditor() },
      )
    }

    if (uiState.showSavedNotesSheet) {
      SavedNotesDialog(
        notes = uiState.visibleNotes,
        accentColor = uiState.effectiveAccentForActiveSurface,
        onSelectNote = { note -> viewModel.openNoteEditor(note.id) },
        onNewNote = { viewModel.openBlankNoteEditor() },
        onDismiss = { viewModel.dismissSavedNotesSheet() },
      )
    }

    if (uiState.showCreateHubDialog) {
      CreateHubDialog(
        accentColor = uiState.effectiveAccentColor,
        onConfirm = { name, accentHex, pinToStart ->
          viewModel.createHub(name, accentHex, pinToStart)
        },
        onDismiss = { viewModel.dismissCreateHubDialog() },
      )
    }

    val renameHubTarget = uiState.renameHubTarget
    if (renameHubTarget != null) {
      RenameHubDialog(
        initialName = renameHubTarget.name,
        accentColor = uiState.effectiveAccentForActiveSurface,
        onConfirm = { newName -> viewModel.renameHub(renameHubTarget.id, newName) },
        onDismiss = { viewModel.dismissRenameHub() },
      )
    }

    val hubColorTarget = uiState.hubColorTarget
    if (hubColorTarget != null) {
      HubColorDialog(
        currentAccentHex = hubColorTarget.accentHex,
        globalAccentColor = uiState.effectiveAccentColor,
        onSelectColor = { hex -> viewModel.setHubAccentHex(hubColorTarget.id, hex) },
        onDismiss = { viewModel.dismissHubColorSheet() },
      )
    }

    val addToHubId = uiState.showAddToHubSheet
    if (addToHubId != null) {
      val hub = uiState.committedLayout.hubs.firstOrNull { it.id == addToHubId }
      if (hub != null) {
        AddToHubDialog(
          hubName = hub.name,
          savedNotes = uiState.visibleNotes,
          accentColor = uiState.effectiveAccentForActiveSurface,
          onAddLiveTile = { kind -> viewModel.addLiveTileToSurface(kind, hub.id) },
          onAddSavedNote = { note -> viewModel.addNoteToHub(hub.id, note) },
          onCreateNote = {
            viewModel.dismissAddToHubSheet()
            viewModel.openBlankNoteEditor(targetHubId = hub.id)
          },
          onCreateFolder = {
            viewModel.dismissAddToHubSheet()
            viewModel.openCreateFolderDialog()
          },
          onAddWidget = {
            viewModel.dismissAddToHubSheet()
            onLaunchWidgetPicker(hub.id)
          },
          onOpenAppsToPin = {
            viewModel.dismissAddToHubSheet()
            viewModel.openApps(focusSearch = false)
          },
          onDismiss = { viewModel.dismissAddToHubSheet() },
        )
      }
    }

    if (showStartLiveTilePicker) {
      AddLiveTileDialog(
        accentColor = uiState.effectiveAccentColor,
        onSelectLiveTile = { kind ->
          onShowStartLiveTilePicker(false)
          viewModel.addLiveTileToSurface(kind, hubId = null)
        },
        onDismiss = { onShowStartLiveTilePicker(false) },
      )
    }

    val pinToHubApp = uiState.pinToHubTargetApp
    if (pinToHubApp != null) {
      PinToHubDialog(
        app = pinToHubApp,
        hubs = uiState.committedLayout.hubs,
        accentColor = uiState.effectiveAccentColor,
        onSelectHub = { hubId -> viewModel.pinAppToHub(pinToHubApp, hubId) },
        onCreateNewHub = {
          viewModel.dismissPinToHubDialog()
          viewModel.openCreateHubDialog()
        },
        onDismiss = { viewModel.dismissPinToHubDialog() },
      )
    }

    val repairProposal = uiState.activeRepairProposal
    if (repairProposal != null) {
      AppRepairDialog(
        proposal = repairProposal,
        iconCacheGeneration = uiState.iconCacheGeneration,
        loadAppIcon = { key -> viewModel.loadAppIcon(key) },
        accentColor = uiState.effectiveAccentForActiveSurface,
        onSelectCandidate = { chosen -> viewModel.resolveAmbiguousTile(repairProposal, chosen) },
        onDismiss = { viewModel.dismissRepairProposal() },
      )
    }

    val appMenu = uiState.appContextMenu
    if (appMenu != null) {
      AppContextMenuDialog(
        menuState = appMenu,
        accentColor = uiState.effectiveAccentColor,
        onPinToStart = {
          viewModel.dismissAppContextMenu()
          viewModel.pinAppToStart(appMenu.app)
        },
        onPinToHub = { viewModel.openPinToHubDialog(appMenu.app) },
        onLaunchDeepShortcut = { shortcut -> viewModel.launchDeepShortcut(shortcut) },
        onOpenAppInfo = { viewModel.openAppInfo(appMenu.app.key) },
        onDismiss = { viewModel.dismissAppContextMenu() },
      )
    }

    VisualAlertBanner(
      alert = uiState.visibleAlert,
      accentColor = uiState.effectiveAccentForActiveSurface,
      onAction = {
        if (uiState.visibleAlert?.isUndoNoteDelete == true) {
          viewModel.undoDeleteNote()
        } else {
          viewModel.dismissAlert()
        }
      },
      onDismiss = { viewModel.dismissAlert() },
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .padding(horizontal = 16.dp, vertical = 74.dp),
    )
  }
}
