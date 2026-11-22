package com.example

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppCatalogManager
import com.example.data.AppIconLoader
import com.example.data.AtomicJsonLayoutRepository
import com.example.data.LayoutRepository
import com.example.data.LoadedAppIcon
import com.example.data.PhotoStorageManager
import com.example.data.PreferencesRepository
import com.example.layout.MetroGridEditor
import com.example.live.BlocoNotificationListenerService
import com.example.live.LiveTileObserver
import com.example.model.AppRepairProposal
import com.example.model.AppTargetKey
import com.example.model.AppThemeMode
import com.example.model.DeepShortcutItem
import com.example.model.InstalledApp
import com.example.model.LauncherSurface
import com.example.model.LeftCommandSlot
import com.example.model.LightweightMode
import com.example.model.MetroAccent
import com.example.model.MetroFolder
import com.example.model.MetroHub
import com.example.model.PhotoCropMode
import com.example.model.PhotoDisplayMode
import com.example.model.QuickNote
import com.example.model.SettingsSection
import com.example.model.StartLayoutState
import com.example.model.StartTile
import com.example.model.TileKind
import com.example.model.TileSize
import com.example.widget.AppWidgetController
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LauncherViewModel(
  application: Application,
  private val layoutRepository: LayoutRepository,
  private val preferencesRepository: PreferencesRepository,
  private val appCatalogManager: AppCatalogManager,
  private val photoStorageManager: PhotoStorageManager,
  private val liveTileObserver: LiveTileObserver,
  val appWidgetController: AppWidgetController,
) : AndroidViewModel(application) {

  constructor(application: Application) : this(
    application = application,
    layoutRepository = AtomicJsonLayoutRepository(application),
    preferencesRepository = PreferencesRepository(application),
    appCatalogManager = AppCatalogManager(application),
    photoStorageManager = PhotoStorageManager(application),
    liveTileObserver = LiveTileObserver(application),
    appWidgetController = AppWidgetController(application),
  )

  private val internalState = MutableStateFlow(LauncherUiState())
  private var lastLaunchElapsedMs = 0L
  private var isForegroundVisible = false

  val uiState: StateFlow<LauncherUiState> =
    combine(
      internalState,
      preferencesRepository.preferencesFlow,
      liveTileObserver.state,
      liveTileObserver.systemReducedMotion,
      liveTileObserver.systemAccessibilityEnabled,
      appCatalogManager.iconLoader.cacheGeneration,
    ) { values ->
      val state = values[0] as LauncherUiState
      val prefs = values[1] as com.example.data.LauncherPreferences
      val live = values[2] as com.example.model.LiveTilesState
      val sysReducedMotion = values[3] as Boolean
      val sysA11yEnabled = values[4] as Boolean
      val cacheGen = values[5] as Long

      appCatalogManager.iconLoader.setLightweightMode(prefs.lightMode)
      liveTileObserver.configurePrivacyAndAccess(
        calendarAccessEnabled = prefs.calendarAccessEnabled,
        notificationsAccessEnabled = prefs.notificationsAccessEnabled,
        showPrivatePreviews = prefs.showPrivatePreviews,
      )

      val activePhotoPath =
        if (prefs.showHomePhotos && prefs.selectedPhotoPaths.isNotEmpty()) {
          val safeIndex = state.activePhotoIndex.coerceIn(0, prefs.selectedPhotoPaths.lastIndex)
          prefs.selectedPhotoPaths[safeIndex]
        } else {
          null
        }
      val photoExists = activePhotoPath != null && photoStorageManager.isPhotoAvailable(activePhotoPath)
      val isPhotoMissing =
        prefs.showHomePhotos && prefs.selectedPhotoPaths.isNotEmpty() && !photoExists

      state.copy(
        iconCacheGeneration = cacheGen,
        accent = prefs.accent,
        customAccentHex = prefs.customAccentHex,
        effectiveAccentColor = prefs.effectiveAccentColor,
        themeMode = prefs.themeMode,
        backgroundHex = prefs.backgroundHex,
        effectiveBackgroundColor = prefs.effectiveBackgroundColor,
        gutterDp = prefs.gutterDp,
        showLabels = prefs.showLabels,
        lockLayout = prefs.lockLayout,
        reduceMotion = prefs.reduceMotion || sysReducedMotion,
        lightweightMode = prefs.lightweightMode,
        lightMode = prefs.lightMode,
        isLowRamDevice = prefs.isLowRamDevice,
        leftCommandSlot = prefs.leftCommandSlot,
        themeThirdPartyIcons = prefs.themeThirdPartyIcons,
        profile = prefs.profile,
        profileAvatarBitmap = if (prefs.profile.avatarPath != null) state.profileAvatarBitmap else null,
        vibrationEnabled = prefs.vibrationEnabled,
        systemAccessibilityEnabled = sysA11yEnabled,
        showHomePhotos = prefs.showHomePhotos,
        selectedPhotoPaths = prefs.selectedPhotoPaths,
        photoDisplayMode = prefs.photoDisplayMode,
        photoCropMode = prefs.photoCropMode,
        showPhotoCaptions = prefs.showPhotoCaptions,
        calendarAccessEnabled = prefs.calendarAccessEnabled,
        notificationsAccessEnabled = prefs.notificationsAccessEnabled,
        showPrivatePreviews = prefs.showPrivatePreviews,
        photoBitmap = if (prefs.showHomePhotos) state.photoBitmap else null,
        liveTiles =
          live.copy(
            showHomePhotos = prefs.showHomePhotos,
            activePhotoPath = if (photoExists) activePhotoPath else null,
            selectedPhotoCount = prefs.selectedPhotoPaths.size,
            photoRevision = prefs.photoRevision,
            isPhotoMissing = isPhotoMissing,
          ),
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = LauncherUiState(),
    )

  init {
    initializeLauncher()
    observePreferencePhotoChanges()
    observeProfileAvatarChanges()
    observePhotoRotation()
  }

  private fun initializeLauncher() {
    viewModelScope.launch {
      // 1. Show saved layout immediately before scanning full package catalog
      val loaded = layoutRepository.loadLayout()
      if (loaded.initialized) {
        internalState.update { it.copy(isLoading = false, committedLayout = loaded) }
      }

      // 2. Discover installed apps off main thread and reconcile
      val apps = appCatalogManager.queryInstalledApps()
      val committed =
        if (!loaded.initialized) {
          val seededTiles = appCatalogManager.seedReferenceLayout(apps)
          val initial =
            StartLayoutState(
              schemaVersion = StartLayoutState.CURRENT_SCHEMA_VERSION,
              revision = 0L,
              initialized = true,
              tiles = seededTiles,
              folders = emptyList(),
              notes = emptyList(),
              hubs = emptyList(),
            )
          layoutRepository.saveLayout(initial).getOrElse { initial }
        } else {
          val (reconciled, repairedAuto) = reconcileFullLayout(loaded, apps)
          if (repairedAuto) {
            layoutRepository.saveLayout(reconciled.first).getOrElse { reconciled.first }
          } else {
            reconciled.first
          }
        }

      val (_, proposals) =
        if (loaded.initialized) {
          reconcileFullLayout(committed, apps).first
        } else {
          (committed to emptyMap())
        }

      internalState.update { current ->
        current.copy(
          isLoading = false,
          committedLayout = committed,
          installedApps = apps,
          filteredApps = filterApps(apps, current.searchQuery),
          repairProposals = proposals,
        )
      }
    }
  }

  private fun reconcileFullLayout(
    layout: StartLayoutState,
    apps: List<InstalledApp>,
  ): Pair<Pair<StartLayoutState, Map<String, AppRepairProposal>>, Boolean> {
    val startRec = appCatalogManager.reconcileTiles(layout.tiles, apps, hubId = null)
    val allProposals = startRec.repairProposals.toMutableMap()
    var anyAutoRepaired = startRec.repairedAutomatically

    val updatedHubs =
      layout.hubs.map { hub ->
        val hubRec = appCatalogManager.reconcileTiles(hub.tiles, apps, hubId = hub.id)
        allProposals.putAll(hubRec.repairProposals)
        if (hubRec.repairedAutomatically) anyAutoRepaired = true
        hub.copy(tiles = hubRec.tiles)
      }

    val updatedState = layout.copy(tiles = startRec.tiles, hubs = updatedHubs)
    return (updatedState to allProposals) to anyAutoRepaired
  }

  private fun observePreferencePhotoChanges() {
    var lastLoadedPath: String? = null
    var lastLoadedRev = -1L
    var lastLoadedEdge = -1
    viewModelScope.launch {
      combine(preferencesRepository.preferencesFlow, internalState) { prefs, st ->
        val path =
          if (prefs.showHomePhotos && prefs.selectedPhotoPaths.isNotEmpty()) {
            val safeIdx = st.activePhotoIndex.coerceIn(0, prefs.selectedPhotoPaths.lastIndex)
            prefs.selectedPhotoPaths[safeIdx]
          } else {
            null
          }
        Triple(path, prefs.photoRevision, st.photoTargetEdgePx)
      }
        .distinctUntilChanged()
        .collect { (path, rev, edgePx) ->
          if (path == null) {
            lastLoadedPath = null
            internalState.update { it.copy(photoBitmap = null) }
          } else if (path != lastLoadedPath || rev != lastLoadedRev || edgePx != lastLoadedEdge) {
            lastLoadedPath = path
            lastLoadedRev = rev
            lastLoadedEdge = edgePx
            val bmp = photoStorageManager.loadPhotoBitmap(path, edgePx)
            internalState.update { it.copy(photoBitmap = bmp) }
          }
        }
    }
  }

  private fun observeProfileAvatarChanges() {
    var lastPath: String? = null
    var lastRev = -1L
    viewModelScope.launch {
      preferencesRepository.preferencesFlow
        .map { it.profile.avatarPath to it.profile.avatarRevision }
        .distinctUntilChanged()
        .collect { (path, rev) ->
          if (path.isNullOrBlank()) {
            lastPath = null
            lastRev = -1L
            internalState.update { it.copy(profileAvatarBitmap = null) }
          } else if (path != lastPath || rev != lastRev) {
            lastPath = path
            lastRev = rev
            val bmp = photoStorageManager.loadProfileAvatar(path, PhotoStorageManager.PROFILE_AVATAR_PX)
            internalState.update { it.copy(profileAvatarBitmap = bmp) }
          }
        }
    }
  }

  private fun observePhotoRotation() {
    viewModelScope.launch {
      combine(preferencesRepository.preferencesFlow, internalState.map { it.surface to it.isEditing }) { prefs, (surface, editing) ->
        Triple(
          prefs.showHomePhotos && prefs.photoDisplayMode == PhotoDisplayMode.ROTATING && !prefs.lightMode,
          prefs.selectedPhotoPaths.size,
          surface is LauncherSurface.Start && !editing,
        )
      }
        .distinctUntilChanged()
        .collect { (rotationEnabled, count, onStart) ->
          if (rotationEnabled && count > 1 && onStart) {
            while (isActive) {
              delay(PHOTO_ROTATION_INTERVAL_MS)
              val powerManager = getApplication<Application>().getSystemService(PowerManager::class.java)
              val screenActive = runCatching { powerManager?.isInteractive != false }.getOrDefault(true)
              if (isForegroundVisible && screenActive) {
                internalState.update { state ->
                  val total = state.selectedPhotoPaths.size
                  if (total <= 1) state else state.copy(activePhotoIndex = (state.activePhotoIndex + 1) % total)
                }
              }
            }
          }
        }
    }
  }

  fun updatePhotoTargetEdgePx(edgePx: Int) {
    val normalized = edgePx.coerceIn(240, 1080)
    internalState.update { state ->
      if (kotlin.math.abs(state.photoTargetEdgePx - normalized) < 48) state
      else state.copy(photoTargetEdgePx = normalized)
    }
  }

  fun onStartLifecycle() {
    isForegroundVisible = true
    liveTileObserver.startObserving()
    appWidgetController.startListening()
    appCatalogManager.observeCatalogChanges { refreshCatalogAndAvailability() }
  }

  fun onStopLifecycle() {
    isForegroundVisible = false
    liveTileObserver.stopObserving()
    appWidgetController.stopListening()
    appCatalogManager.unregisterCatalogObserver()
  }

  fun onTrimMemory(level: Int) {
    appCatalogManager.iconLoader.onTrimMemory(level)
    if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
      level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE
    ) {
      internalState.update { it.copy(photoBitmap = null, profileAvatarBitmap = null) }
    }
  }

  fun onResumeLifecycle(isDefaultHome: Boolean) {
    liveTileObserver.refreshAll()
    internalState.update { it.copy(isDefaultHome = isDefaultHome) }
    refreshCatalogAndAvailability()
    refreshPhotoAvailability()
  }

  fun onHomeIntentReceived() {
    internalState.update { state ->
      state.copy(
        surface = LauncherSurface.Start,
        searchQuery = "",
        filteredApps = filterApps(state.installedApps, ""),
        expandedFolderId = null,
        showResizeSheet = false,
        showMoveSheet = false,
        showTileColorSheet = false,
        showUnsavedDialog = false,
        staleTilePrompt = null,
        activeRepairProposal = null,
        duplicatePinPrompt = null,
        showPhotoActionSheet = false,
        showCreateFolderDialog = false,
        showCreateHubDialog = false,
        showSavedNotesSheet = false,
        showAddToHubSheet = null,
        pinToHubTargetApp = null,
        appContextMenu = null,
        noteEditorSession = null,
        pendingProfileCrop = null,
      )
    }
  }

  fun refreshCatalogAndAvailability() {
    viewModelScope.launch {
      val apps = appCatalogManager.queryInstalledApps()
      internalState.update { current ->
        val (reconciledPair, _) = reconcileFullLayout(current.committedLayout, apps)
        val (updatedCommitted, proposals) = reconciledPair
        val updatedDraft =
          if (current.isEditing) {
            appCatalogManager.reconcileTiles(current.draftTiles, apps, current.editingHubId).tiles
          } else {
            current.draftTiles
          }
        current.copy(
          installedApps = apps,
          filteredApps = filterApps(apps, current.searchQuery),
          committedLayout = updatedCommitted,
          draftTiles = updatedDraft,
          repairProposals = proposals,
        )
      }
    }
  }

  private fun refreshPhotoAvailability() {
    viewModelScope.launch {
      val st = uiState.value
      val currentPath = st.liveTiles.activePhotoPath
      if (!st.showHomePhotos || currentPath == null || !photoStorageManager.isPhotoAvailable(currentPath)) {
        internalState.update { it.copy(photoBitmap = null) }
      } else if (internalState.value.photoBitmap == null) {
        val bmp = photoStorageManager.loadPhotoBitmap(currentPath, internalState.value.photoTargetEdgePx)
        internalState.update { it.copy(photoBitmap = bmp) }
      }

      val avatarPath = st.profile.avatarPath
      if (avatarPath == null || !photoStorageManager.isPhotoAvailable(avatarPath)) {
        internalState.update { it.copy(profileAvatarBitmap = null) }
      } else if (internalState.value.profileAvatarBitmap == null) {
        val bmp = photoStorageManager.loadProfileAvatar(avatarPath, PhotoStorageManager.PROFILE_AVATAR_PX)
        internalState.update { it.copy(profileAvatarBitmap = bmp) }
      }
    }
  }

  // --- Navigation ---
  fun saveStartScrollPosition(index: Int, offset: Int) {
    internalState.update { state ->
      if (state.startSavedScrollIndex == index && state.startSavedScrollOffset == offset) state
      else state.copy(startSavedScrollIndex = index, startSavedScrollOffset = offset)
    }
  }

  fun openApps(focusSearch: Boolean) {
    internalState.update { state ->
      state.copy(
        surface = LauncherSurface.Apps(focusSearch = focusSearch),
        expandedFolderId = null,
        filteredApps = filterApps(state.installedApps, state.searchQuery),
      )
    }
  }

  fun navigateBackToStart() {
    internalState.update { state ->
      state.copy(
        surface = LauncherSurface.Start,
        expandedFolderId = null,
        isEditing = false,
        editingHubId = null,
        selectedTileId = null,
        searchQuery = "",
        filteredApps = filterApps(state.installedApps, ""),
      )
    }
  }

  fun openHub(hubId: String) {
    if (uiState.value.committedLayout.hubs.none { it.id == hubId }) return
    internalState.update { state ->
      state.copy(
        surface = LauncherSurface.Hub(hubId),
        expandedFolderId = null,
        isEditing = false,
        editingHubId = null,
        selectedTileId = null,
      )
    }
  }

  fun openSettings(section: SettingsSection = SettingsSection.LANDING) {
    internalState.update { state ->
      val returnTarget =
        (state.surface as? LauncherSurface.Settings)?.returnTo ?: state.surface
      state.copy(
        surface = LauncherSurface.Settings(returnTo = returnTarget, section = section)
      )
    }
  }

  fun navigateSettingsSection(section: SettingsSection) {
    internalState.update { state ->
      val current = state.surface as? LauncherSurface.Settings
      val returnTarget = current?.returnTo ?: LauncherSurface.Start
      state.copy(surface = LauncherSurface.Settings(returnTo = returnTarget, section = section))
    }
  }

  fun navigateBackFromSettings(toLandingFirst: Boolean = true) {
    internalState.update { state ->
      val current = state.surface
      if (current is LauncherSurface.Settings) {
        if (toLandingFirst && current.section != SettingsSection.LANDING) {
          state.copy(surface = current.copy(section = SettingsSection.LANDING))
        } else {
          state.copy(surface = current.returnTo)
        }
      } else {
        state.copy(surface = LauncherSurface.Start)
      }
    }
  }

  fun updateSearchQuery(query: String) {
    internalState.update { state ->
      state.copy(
        searchQuery = query,
        filteredApps = filterApps(state.installedApps, query),
      )
    }
  }

  fun clearSearchQuery() = updateSearchQuery("")

  // --- Icons, Launch, Deep Shortcuts & App Repair ---
  suspend fun loadAppIcon(key: AppTargetKey, maxPx: Int = AppIconLoader.DEFAULT_ICON_PX): LoadedAppIcon {
    return appCatalogManager.iconLoader.loadIcon(key, maxPx)
  }

  fun rebuildIconCache() {
    appCatalogManager.iconLoader.rebuildCache()
    showToast(getApplication<Application>().getString(R.string.reload_icons_done))
  }

  fun launchApp(app: InstalledApp) {
    if (app.isProfilePaused) {
      showToast(getApplication<Application>().getString(R.string.work_profile_paused))
      return
    }
    launchAppByKey(app.key)
  }

  fun launchAppByKey(key: AppTargetKey) {
    val now = SystemClock.elapsedRealtime()
    if (now - lastLaunchElapsedMs < LAUNCH_DEBOUNCE_MS) return
    lastLaunchElapsedMs = now

    if (!appCatalogManager.launchApp(key)) {
      refreshCatalogAndAvailability()
      showToast(getApplication<Application>().getString(R.string.unavailable))
    }
  }

  fun openAppInfo(key: AppTargetKey) {
    appCatalogManager.openAppInfo(key)
    dismissAppContextMenu()
  }

  fun openAppContextMenu(app: InstalledApp) {
    internalState.update {
      it.copy(
        appContextMenu =
          AppContextMenuState(app = app, deepShortcuts = emptyList(), isLoadingShortcuts = true)
      )
    }
    viewModelScope.launch {
      val shortcuts = appCatalogManager.queryDeepShortcuts(app.key)
      internalState.update { current ->
        val activeMenu = current.appContextMenu
        if (activeMenu?.app?.key == app.key) {
          current.copy(
            appContextMenu =
              activeMenu.copy(deepShortcuts = shortcuts, isLoadingShortcuts = false)
          )
        } else {
          current
        }
      }
    }
  }

  fun dismissAppContextMenu() {
    internalState.update { it.copy(appContextMenu = null) }
  }

  fun launchDeepShortcut(shortcut: DeepShortcutItem) {
    dismissAppContextMenu()
    appCatalogManager.launchDeepShortcut(shortcut)
  }

  fun resolveAmbiguousTile(proposal: AppRepairProposal, chosen: InstalledApp) {
    viewModelScope.launch {
      val current = internalState.value.committedLayout
      fun patchTiles(tiles: List<StartTile>): List<StartTile> =
        tiles.map { t ->
          if (t.id == proposal.tileId) {
            t.copy(
              target = chosen.key,
              fallbackLabel = chosen.label,
              isUnavailable = chosen.isProfilePaused,
              needsRepairChoice = false,
            )
          } else {
            t
          }
        }

      val next =
        if (proposal.hubId == null) {
          current.copy(tiles = patchTiles(current.tiles))
        } else {
          current.copy(
            hubs =
              current.hubs.map { h ->
                if (h.id == proposal.hubId) h.copy(tiles = patchTiles(h.tiles)) else h
              }
          )
        }
      layoutRepository.saveLayout(next).onSuccess { saved ->
        internalState.update {
          it.copy(
            committedLayout = saved,
            repairProposals = it.repairProposals - proposal.tileId,
            activeRepairProposal = null,
          )
        }
      }
    }
  }

  fun dismissRepairProposal() {
    internalState.update { it.copy(activeRepairProposal = null) }
  }

  fun onLeftCommandAction() {
    when (uiState.value.leftCommandSlot) {
      LeftCommandSlot.SEARCH -> openApps(focusSearch = true)
      LeftCommandSlot.QUICK_NOTE -> openBlankNoteEditor()
      LeftCommandSlot.FOLDERS -> {
        val firstFolder = uiState.value.visibleFolders.firstOrNull()
        if (firstFolder != null) {
          val folderTile = uiState.value.visibleTiles.firstOrNull { it.folderId == firstFolder.id }
          internalState.update {
            it.copy(expandedFolderId = firstFolder.id, scrollToTileId = folderTile?.id)
          }
        } else {
          openCreateFolderDialog()
        }
      }
    }
  }

  // --- Tile Taps & Actions ---
  fun onTileTap(tile: StartTile, onRequestPhotoPicker: () -> Unit) {
    if (internalState.value.isEditing) {
      selectTile(tile.id)
      return
    }
    val now = SystemClock.elapsedRealtime()
    if (now - lastLaunchElapsedMs < LAUNCH_DEBOUNCE_MS) return
    lastLaunchElapsedMs = now

    when (tile.kind) {
      TileKind.HUB -> {
        val hubId = tile.hubId ?: return
        openHub(hubId)
      }
      TileKind.FOLDER -> {
        val folderId = tile.folderId ?: return
        internalState.update { state ->
          val nextExpanded = if (state.expandedFolderId == folderId) null else folderId
          state.copy(
            expandedFolderId = nextExpanded,
            scrollToTileId = if (nextExpanded != null) tile.id else null,
          )
        }
      }
      TileKind.NOTE -> {
        val noteId = tile.noteId
        if (noteId != null) openNoteEditor(noteId) else openBlankNoteEditor()
      }
      TileKind.PHOTO -> {
        val live = uiState.value.liveTiles
        if (!uiState.value.showHomePhotos) {
          onRequestPhotoPicker()
        } else if (live.activePhotoPath == null || live.isPhotoMissing || internalState.value.photoBitmap == null) {
          onRequestPhotoPicker()
        } else {
          internalState.update { it.copy(showPhotoActionSheet = true) }
        }
      }
      TileKind.NOTIFICATIONS -> {
        if (!BlocoNotificationListenerService.isAccessGranted(getApplication())) {
          showToast(getApplication<Application>().getString(R.string.notifications_permission_needed))
        }
      }
      TileKind.CLOCK,
      TileKind.BATTERY,
      TileKind.CALENDAR,
      TileKind.MUSIC -> {
        val launched = appCatalogManager.launchUtilityTile(tile, internalState.value.installedApps)
        if (!launched) {
          showToast(getApplication<Application>().getString(R.string.unavailable))
        }
      }
      TileKind.SYSTEM_WIDGET -> Unit
      TileKind.APP_SHORTCUT -> {
        val repair = internalState.value.repairProposals[tile.id]
        if (tile.needsRepairChoice && repair != null) {
          internalState.update { it.copy(activeRepairProposal = repair) }
          return
        }
        val target = tile.target
        if (target == null) {
          internalState.update { it.copy(staleTilePrompt = tile) }
          return
        }
        val appRecord = internalState.value.installedApps.firstOrNull { it.key == target }
        if (appRecord?.isProfilePaused == true) {
          showToast(getApplication<Application>().getString(R.string.work_profile_paused))
          return
        }
        val launched = appCatalogManager.launchApp(target)
        if (!launched) {
          refreshCatalogAndAvailability()
          internalState.update { state ->
            state.copy(staleTilePrompt = tile.copy(isUnavailable = true))
          }
        }
      }
    }
  }

  fun onMediaPlayPause() = BlocoNotificationListenerService.togglePlayPause()
  fun onMediaPrevious() = BlocoNotificationListenerService.skipPrevious()
  fun onMediaNext() = BlocoNotificationListenerService.skipNext()

  // --- Folders ---
  fun collapseExpandedFolder() {
    internalState.update { state ->
      val expandedId = state.expandedFolderId ?: return@update state
      val folderTileId = state.visibleTiles.firstOrNull { it.folderId == expandedId }?.id
      state.copy(expandedFolderId = null, restoreFocusTileId = folderTileId)
    }
  }

  fun openCreateFolderDialog() {
    if (uiState.value.lockLayout) {
      showToast(getApplication<Application>().getString(R.string.layout_locked_notice))
      return
    }
    internalState.update { it.copy(showCreateFolderDialog = true) }
  }

  fun dismissCreateFolderDialog() {
    internalState.update { it.copy(showCreateFolderDialog = false) }
  }

  fun createFolderFromSelection(name: String, appKeys: List<AppTargetKey>) {
    val cleanName =
      name.trim().ifEmpty { getApplication<Application>().getString(R.string.folder_default_name) }
    val folderId = "folder_${UUID.randomUUID()}"
    val folder = MetroFolder(id = folderId, name = cleanName, appKeys = appKeys.distinct())
    val currentHubId = (internalState.value.surface as? LauncherSurface.Hub)?.hubId

    if (internalState.value.isEditing) {
      internalState.update { state ->
        val (col, row) = MetroGridEditor.findFirstOpenPosition(state.draftTiles, 2, 2)
        val tile =
          StartTile(
            id = "tile_$folderId",
            kind = TileKind.FOLDER,
            col = col,
            row = row,
            colSpan = 2,
            rowSpan = 2,
            fallbackLabel = cleanName,
            folderId = folderId,
          )
        state.copy(
          draftFolders = state.draftFolders + folder,
          draftTiles = state.draftTiles + tile,
          hasPendingChanges = true,
          showCreateFolderDialog = false,
          selectedTileId = tile.id,
        )
      }
    } else {
      viewModelScope.launch {
        val next =
          updateSurfaceTilesAndFolders(
            current = internalState.value.committedLayout,
            hubId = currentHubId,
          ) { tiles, folders ->
            val (col, row) = MetroGridEditor.findFirstOpenPosition(tiles, 2, 2)
            val tile =
              StartTile(
                id = "tile_$folderId",
                kind = TileKind.FOLDER,
                col = col,
                row = row,
                colSpan = 2,
                rowSpan = 2,
                fallbackLabel = cleanName,
                folderId = folderId,
              )
            (tiles + tile) to (folders + folder)
          }
        layoutRepository.saveLayout(next).onSuccess { saved ->
          internalState.update {
            it.copy(
              committedLayout = saved,
              showCreateFolderDialog = false,
              expandedFolderId = folderId,
              scrollToTileId = "tile_$folderId",
            )
          }
        }
      }
    }
  }

  fun confirmMergeShortcutsIntoFolder(folderName: String) {
    val pair = internalState.value.pendingDragMergePair ?: return
    val (draggedId, targetId) = pair
    val cleanName =
      folderName.trim().ifEmpty { getApplication<Application>().getString(R.string.folder_default_name) }

    internalState.update { state ->
      val dragged = state.draftTiles.firstOrNull { it.id == draggedId } ?: return@update state.copy(pendingDragMergePair = null)
      val target = state.draftTiles.firstOrNull { it.id == targetId } ?: return@update state.copy(pendingDragMergePair = null)
      val draggedKey = dragged.target ?: return@update state.copy(pendingDragMergePair = null)
      val targetKey = target.target ?: return@update state.copy(pendingDragMergePair = null)

      val folderId = "folder_${UUID.randomUUID()}"
      val newFolder = MetroFolder(id = folderId, name = cleanName, appKeys = listOf(targetKey, draggedKey).distinct())
      val folderCol = target.col.coerceIn(0, 4)
      val folderTile =
        StartTile(
          id = "tile_$folderId",
          kind = TileKind.FOLDER,
          col = folderCol,
          row = target.row,
          colSpan = 2,
          rowSpan = 2,
          fallbackLabel = cleanName,
          folderId = folderId,
          customColorHex = target.customColorHex,
        )
      val withoutPair = state.draftTiles.filterNot { it.id == draggedId || it.id == targetId }
      val mergedTiles = MetroGridEditor.moveTileWithReflow(withoutPair + folderTile, folderTile.id, folderCol, target.row)
      state.copy(
        draftTiles = mergedTiles,
        draftFolders = state.draftFolders + newFolder,
        selectedTileId = folderTile.id,
        hasPendingChanges = true,
        pendingDragMergePair = null,
      )
    }
  }

  fun dismissPendingMergeDialog() {
    internalState.update { it.copy(pendingDragMergePair = null) }
  }

  fun openRenameFolder(folder: MetroFolder) {
    internalState.update { it.copy(renameFolderTarget = folder) }
  }

  fun dismissRenameFolder() {
    internalState.update { it.copy(renameFolderTarget = null) }
  }

  fun renameFolder(folderId: String, newName: String) {
    val clean = newName.trim().ifEmpty { return }
    val currentHubId = (internalState.value.surface as? LauncherSurface.Hub)?.hubId
    if (internalState.value.isEditing) {
      internalState.update { state ->
        state.copy(
          draftFolders = state.draftFolders.map { if (it.id == folderId) it.copy(name = clean) else it },
          draftTiles = state.draftTiles.map { if (it.folderId == folderId) it.copy(fallbackLabel = clean) else it },
          hasPendingChanges = true,
          renameFolderTarget = null,
        )
      }
    } else {
      viewModelScope.launch {
        val next =
          updateSurfaceTilesAndFolders(internalState.value.committedLayout, currentHubId) { tiles, folders ->
            tiles.map { if (it.folderId == folderId) it.copy(fallbackLabel = clean) else it } to
              folders.map { if (it.id == folderId) it.copy(name = clean) else it }
          }
        layoutRepository.saveLayout(next).onSuccess { saved ->
          internalState.update { it.copy(committedLayout = saved, renameFolderTarget = null) }
        }
      }
    }
  }

  fun openAddAppsToFolder(folder: MetroFolder) {
    internalState.update { it.copy(addAppsFolderTarget = folder) }
  }

  fun dismissAddAppsToFolder() {
    internalState.update { it.copy(addAppsFolderTarget = null) }
  }

  fun setFolderApps(folderId: String, appKeys: List<AppTargetKey>) {
    val distinctKeys = appKeys.distinct()
    val currentHubId = (internalState.value.surface as? LauncherSurface.Hub)?.hubId
    if (internalState.value.isEditing) {
      internalState.update { state ->
        state.copy(
          draftFolders = state.draftFolders.map { if (it.id == folderId) it.copy(appKeys = distinctKeys) else it },
          hasPendingChanges = true,
          addAppsFolderTarget = null,
        )
      }
    } else {
      viewModelScope.launch {
        val next =
          updateSurfaceTilesAndFolders(internalState.value.committedLayout, currentHubId) { tiles, folders ->
            tiles to folders.map { if (it.id == folderId) it.copy(appKeys = distinctKeys) else it }
          }
        layoutRepository.saveLayout(next).onSuccess { saved ->
          internalState.update { it.copy(committedLayout = saved, addAppsFolderTarget = null) }
        }
      }
    }
  }

  fun reorderFolderApp(folderId: String, index: Int, delta: Int) {
    val targetIndex = index + delta
    val currentHubId = (internalState.value.surface as? LauncherSurface.Hub)?.hubId
    fun reorderList(list: List<AppTargetKey>): List<AppTargetKey> {
      if (index !in list.indices || targetIndex !in list.indices) return list
      val mutable = list.toMutableList()
      val item = mutable.removeAt(index)
      mutable.add(targetIndex, item)
      return mutable
    }
    viewModelScope.launch {
      val next =
        updateSurfaceTilesAndFolders(internalState.value.committedLayout, currentHubId) { tiles, folders ->
          tiles to folders.map { if (it.id == folderId) it.copy(appKeys = reorderList(it.appKeys)) else it }
        }
      layoutRepository.saveLayout(next).onSuccess { saved ->
        internalState.update { it.copy(committedLayout = saved) }
      }
    }
  }

  fun moveFolderAppToStart(folderId: String, appKey: AppTargetKey) {
    val currentHubId = (internalState.value.surface as? LauncherSurface.Hub)?.hubId
    viewModelScope.launch {
      val current = internalState.value.committedLayout
      val label = internalState.value.installedApps.firstOrNull { it.key == appKey }?.label ?: appKey.packageName
      val afterFolderRemoval =
        updateSurfaceTilesAndFolders(current, currentHubId) { tiles, folders ->
          tiles to folders.map { f ->
            if (f.id == folderId) f.copy(appKeys = f.appKeys.filterNot { it == appKey }) else f
          }
        }
      val alreadyOnStart = afterFolderRemoval.tiles.any { it.target == appKey }
      val finalLayout =
        if (alreadyOnStart) {
          afterFolderRemoval
        } else {
          val (col, row) = MetroGridEditor.findFirstOpenPosition(afterFolderRemoval.tiles, 2, 2)
          afterFolderRemoval.copy(
            tiles = afterFolderRemoval.tiles + StartTile(
              id = "pin_${UUID.randomUUID()}",
              kind = TileKind.APP_SHORTCUT,
              col = col,
              row = row,
              colSpan = 2,
              rowSpan = 2,
              target = appKey,
              fallbackLabel = label,
            )
          )
        }
      layoutRepository.saveLayout(finalLayout).onSuccess { saved ->
        internalState.update { it.copy(committedLayout = saved) }
      }
    }
  }

  fun removeAppFromFolder(folderId: String, appKey: AppTargetKey) {
    val currentHubId = (internalState.value.surface as? LauncherSurface.Hub)?.hubId
    viewModelScope.launch {
      val next =
        updateSurfaceTilesAndFolders(internalState.value.committedLayout, currentHubId) { tiles, folders ->
          tiles to folders.map { f ->
            if (f.id == folderId) f.copy(appKeys = f.appKeys.filterNot { it == appKey }) else f
          }
        }
      layoutRepository.saveLayout(next).onSuccess { saved ->
        internalState.update { it.copy(committedLayout = saved) }
      }
    }
  }

  fun dissolveFolder(folderId: String) {
    val currentHubId = (internalState.value.surface as? LauncherSurface.Hub)?.hubId
    if (internalState.value.isEditing) {
      internalState.update { state ->
        val folder = state.draftFolders.firstOrNull { it.id == folderId } ?: return@update state
        val remainingTiles = state.draftTiles.filterNot { it.folderId == folderId }
        val released = buildReleasedShortcutTiles(folder, remainingTiles, state.installedApps)
        state.copy(
          draftTiles = MetroGridEditor.placeTilesAfterDissolve(remainingTiles, released),
          draftFolders = state.draftFolders.filterNot { it.id == folderId },
          selectedTileId = null,
          expandedFolderId = null,
          hasPendingChanges = true,
        )
      }
    } else {
      viewModelScope.launch {
        val next =
          updateSurfaceTilesAndFolders(internalState.value.committedLayout, currentHubId) { tiles, folders ->
            val folder = folders.firstOrNull { it.id == folderId } ?: return@updateSurfaceTilesAndFolders tiles to folders
            val remaining = tiles.filterNot { it.folderId == folderId }
            val released = buildReleasedShortcutTiles(folder, remaining, internalState.value.installedApps)
            MetroGridEditor.placeTilesAfterDissolve(remaining, released) to folders.filterNot { it.id == folderId }
          }
        layoutRepository.saveLayout(next).onSuccess { saved ->
          internalState.update { it.copy(committedLayout = saved, expandedFolderId = null) }
        }
      }
    }
  }

  private fun buildReleasedShortcutTiles(
    folder: MetroFolder,
    existingTiles: List<StartTile>,
    apps: List<InstalledApp>,
  ): List<StartTile> {
    val existingTargets = existingTiles.mapNotNull { it.target }.toSet()
    return folder.appKeys
      .filterNot { it in existingTargets }
      .map { key ->
        val label = apps.firstOrNull { it.key == key }?.label ?: key.packageName
        StartTile(
          id = "pin_${UUID.randomUUID()}",
          kind = TileKind.APP_SHORTCUT,
          col = 0,
          row = 0,
          colSpan = 2,
          rowSpan = 2,
          target = key,
          fallbackLabel = label,
          useOriginalIcon = true,
        )
      }
  }

  // --- Metro Hubs Management ---
  fun openCreateHubDialog() {
    if (uiState.value.lockLayout) {
      showToast(getApplication<Application>().getString(R.string.layout_locked_notice))
      return
    }
    internalState.update { it.copy(showCreateHubDialog = true) }
  }

  fun dismissCreateHubDialog() {
    internalState.update { it.copy(showCreateHubDialog = false) }
  }

  fun createHub(name: String, accentHex: Long?, pinShortcutToStart: Boolean = true) {
    val cleanName =
      name.trim().ifEmpty { getApplication<Application>().getString(R.string.hub_default_name) }
    val hubId = "hub_${UUID.randomUUID()}"
    val hub = MetroHub(id = hubId, name = cleanName, accentHex = accentHex)

    viewModelScope.launch {
      val current = internalState.value.committedLayout
      val nextTiles =
        if (pinShortcutToStart) {
          val (col, row) = MetroGridEditor.findFirstOpenPosition(current.tiles, 2, 2)
          current.tiles +
            StartTile(
              id = "tile_$hubId",
              kind = TileKind.HUB,
              col = col,
              row = row,
              colSpan = 2,
              rowSpan = 2,
              fallbackLabel = cleanName,
              hubId = hubId,
              customColorHex = accentHex,
            )
        } else {
          current.tiles
        }
      val next = current.copy(hubs = current.hubs + hub, tiles = nextTiles)
      layoutRepository.saveLayout(next).onSuccess { saved ->
        internalState.update {
          it.copy(
            committedLayout = saved,
            showCreateHubDialog = false,
            surface = LauncherSurface.Hub(hubId),
          )
        }
      }
    }
  }

  fun openRenameHub(hub: MetroHub) {
    internalState.update { it.copy(renameHubTarget = hub) }
  }

  fun dismissRenameHub() {
    internalState.update { it.copy(renameHubTarget = null) }
  }

  fun renameHub(hubId: String, newName: String) {
    val clean = newName.trim().ifEmpty { return }
    viewModelScope.launch {
      val current = internalState.value.committedLayout
      val next =
        current.copy(
          hubs = current.hubs.map { if (it.id == hubId) it.copy(name = clean) else it },
          tiles = current.tiles.map { if (it.hubId == hubId) it.copy(fallbackLabel = clean) else it },
        )
      layoutRepository.saveLayout(next).onSuccess { saved ->
        internalState.update { it.copy(committedLayout = saved, renameHubTarget = null) }
      }
    }
  }

  fun openHubColorSheet(hub: MetroHub) {
    internalState.update { it.copy(hubColorTarget = hub) }
  }

  fun dismissHubColorSheet() {
    internalState.update { it.copy(hubColorTarget = null) }
  }

  fun setHubAccentHex(hubId: String, hex: Long?) {
    viewModelScope.launch {
      val current = internalState.value.committedLayout
      val next =
        current.copy(
          hubs = current.hubs.map { if (it.id == hubId) it.copy(accentHex = hex) else it },
          tiles = current.tiles.map { if (it.hubId == hubId) it.copy(customColorHex = hex) else it },
        )
      layoutRepository.saveLayout(next).onSuccess { saved ->
        internalState.update { it.copy(committedLayout = saved, hubColorTarget = null) }
      }
    }
  }

  fun reorderHub(hubId: String, delta: Int) {
    viewModelScope.launch {
      val current = internalState.value.committedLayout
      val idx = current.hubs.indexOfFirst { it.id == hubId }
      val targetIdx = idx + delta
      if (idx !in current.hubs.indices || targetIdx !in current.hubs.indices) return@launch
      val mutable = current.hubs.toMutableList()
      val item = mutable.removeAt(idx)
      mutable.add(targetIdx, item)
      layoutRepository.saveLayout(current.copy(hubs = mutable)).onSuccess { saved ->
        internalState.update { it.copy(committedLayout = saved) }
      }
    }
  }

  fun pinHubToStart(hub: MetroHub) {
    viewModelScope.launch {
      val current = internalState.value.committedLayout
      if (current.tiles.any { it.hubId == hub.id }) {
        showToast(getApplication<Application>().getString(R.string.already_pinned))
        return@launch
      }
      val (col, row) = MetroGridEditor.findFirstOpenPosition(current.tiles, 2, 2)
      val tile =
        StartTile(
          id = "tile_${hub.id}_${UUID.randomUUID().toString().take(4)}",
          kind = TileKind.HUB,
          col = col,
          row = row,
          colSpan = 2,
          rowSpan = 2,
          fallbackLabel = hub.name,
          hubId = hub.id,
          customColorHex = hub.accentHex,
        )
      layoutRepository.saveLayout(current.copy(tiles = current.tiles + tile)).onSuccess { saved ->
        internalState.update { it.copy(committedLayout = saved) }
        showToast(getApplication<Application>().getString(R.string.pinned_confirmation))
      }
    }
  }

  fun deleteHub(hubId: String) {
    viewModelScope.launch {
      val current = internalState.value.committedLayout
      val hub = current.hubs.firstOrNull { it.id == hubId } ?: return@launch
      hub.tiles.mapNotNull { it.appWidgetId }.forEach { appWidgetController.deleteAppWidgetId(it) }
      val next =
        current.copy(
          hubs = current.hubs.filterNot { it.id == hubId },
          tiles = current.tiles.filterNot { it.hubId == hubId },
          // Installed apps & saved notes remain preserved!
        )
      layoutRepository.saveLayout(next).onSuccess { saved ->
        internalState.update { state ->
          val nextSurface =
            if ((state.surface as? LauncherSurface.Hub)?.hubId == hubId) LauncherSurface.Start else state.surface
          state.copy(
            committedLayout = saved,
            surface = nextSurface,
            isEditing = false,
            editingHubId = null,
          )
        }
      }
    }
  }

  fun openAddToHubSheet(hubId: String) {
    internalState.update { it.copy(showAddToHubSheet = hubId) }
  }

  fun dismissAddToHubSheet() {
    internalState.update { it.copy(showAddToHubSheet = null) }
  }

  fun addLiveTileToSurface(kind: TileKind, hubId: String? = null) {
    val label =
      when (kind) {
        TileKind.CLOCK -> getApplication<Application>().getString(R.string.clock)
        TileKind.BATTERY -> getApplication<Application>().getString(R.string.battery)
        TileKind.CALENDAR -> getApplication<Application>().getString(R.string.calendar)
        TileKind.PHOTO -> getApplication<Application>().getString(R.string.photo)
        TileKind.MUSIC -> getApplication<Application>().getString(R.string.music_tile)
        TileKind.NOTIFICATIONS -> getApplication<Application>().getString(R.string.notifications_tile)
        else -> ""
      }
    val size = if (kind == TileKind.CALENDAR || kind == TileKind.MUSIC) TileSize.WIDE else TileSize.MEDIUM
    viewModelScope.launch {
      val next =
        updateSurfaceTilesAndFolders(internalState.value.committedLayout, hubId) { tiles, folders ->
          val (col, row) = MetroGridEditor.findFirstOpenPosition(tiles, size.colSpan, size.rowSpan)
          val tile =
            StartTile(
              id = "live_${kind.name.lowercase()}_${UUID.randomUUID().toString().take(6)}",
              kind = kind,
              col = col,
              row = row,
              colSpan = size.colSpan,
              rowSpan = size.rowSpan,
              fallbackLabel = label,
            )
          (tiles + tile) to folders
        }
      layoutRepository.saveLayout(next).onSuccess { saved ->
        internalState.update { it.copy(committedLayout = saved, showAddToHubSheet = null) }
        showToast(getApplication<Application>().getString(R.string.pinned_confirmation))
      }
    }
  }

  fun addNoteToHub(hubId: String, note: QuickNote) {
    viewModelScope.launch {
      val next =
        updateSurfaceTilesAndFolders(internalState.value.committedLayout, hubId) { tiles, folders ->
          if (tiles.any { it.noteId == note.id }) {
            tiles to folders
          } else {
            val (col, row) = MetroGridEditor.findFirstOpenPosition(tiles, 4, 2)
            val tile =
              StartTile(
                id = "hub_note_${note.id}_${UUID.randomUUID().toString().take(4)}",
                kind = TileKind.NOTE,
                col = col,
                row = row,
                colSpan = 4,
                rowSpan = 2,
                fallbackLabel = note.title.ifBlank { getApplication<Application>().getString(R.string.quick_note) },
                noteId = note.id,
              )
            (tiles + tile) to folders
          }
        }
      layoutRepository.saveLayout(next).onSuccess { saved ->
        internalState.update { it.copy(committedLayout = saved, showAddToHubSheet = null) }
      }
    }
  }

  fun addSystemWidgetTile(appWidgetId: Int, label: String, hubId: String? = null) {
    viewModelScope.launch {
      val next =
        updateSurfaceTilesAndFolders(internalState.value.committedLayout, hubId) { tiles, folders ->
          val (col, row) = MetroGridEditor.findFirstOpenPosition(tiles, 4, 2)
          val tile =
            StartTile(
              id = "widget_${appWidgetId}_${UUID.randomUUID().toString().take(4)}",
              kind = TileKind.SYSTEM_WIDGET,
              col = col,
              row = row,
              colSpan = 4,
              rowSpan = 2,
              fallbackLabel = label.ifBlank { getApplication<Application>().getString(R.string.system_widget) },
              appWidgetId = appWidgetId,
            )
          (tiles + tile) to folders
        }
      layoutRepository.saveLayout(next).onSuccess { saved ->
        internalState.update { it.copy(committedLayout = saved) }
      }
    }
  }

  // --- Quick Notes ---
  fun openBlankNoteEditor(targetHubId: String? = (internalState.value.surface as? LauncherSurface.Hub)?.hubId) {
    internalState.update {
      it.copy(
        noteEditorSession =
          NoteEditorSession(
            noteId = null,
            title = "",
            body = "",
            isChecklist = false,
            isPinnedToStart = true,
            targetHubId = targetHubId,
            preferredSize = TileSize.MEDIUM,
          ),
        showSavedNotesSheet = false,
      )
    }
  }

  fun openNoteEditor(noteId: String) {
    val state = internalState.value
    val note = state.visibleNotes.firstOrNull { it.id == noteId } ?: return
    val currentHubId = (state.surface as? LauncherSurface.Hub)?.hubId
    val existingTile = state.visibleTiles.firstOrNull { it.noteId == noteId }
    internalState.update {
      it.copy(
        noteEditorSession =
          NoteEditorSession(
            noteId = note.id,
            title = note.title,
            body = note.body,
            isChecklist = note.isChecklist,
            isPinnedToStart = existingTile != null,
            targetHubId = currentHubId,
            preferredSize = existingTile?.size ?: TileSize.MEDIUM,
          ),
        showSavedNotesSheet = false,
      )
    }
  }

  fun dismissNoteEditor() {
    internalState.update { it.copy(noteEditorSession = null) }
  }

  fun openSavedNotesSheet() {
    internalState.update { it.copy(showSavedNotesSheet = true) }
  }

  fun dismissSavedNotesSheet() {
    internalState.update { it.copy(showSavedNotesSheet = false) }
  }

  fun saveNote(
    existingNoteId: String?,
    title: String,
    body: String,
    isChecklist: Boolean,
    pinToSurface: Boolean,
    targetHubId: String?,
    preferredSize: TileSize,
  ) {
    val noteId = existingNoteId ?: "note_${UUID.randomUUID()}"
    val normalizedSize = if (preferredSize == TileSize.WIDE) TileSize.WIDE else TileSize.MEDIUM
    val updatedNote =
      QuickNote(
        id = noteId,
        title = title.trim(),
        body = body,
        isChecklist = isChecklist,
        updatedAtMs = System.currentTimeMillis(),
      )

    viewModelScope.launch {
      val current = internalState.value.committedLayout
      val nextNotes =
        if (current.notes.any { it.id == noteId }) {
          current.notes.map { if (it.id == noteId) updatedNote else it }
        } else {
          listOf(updatedNote) + current.notes
        }

      val withUpdatedNotes = current.copy(notes = nextNotes)
      val nextLayout =
        updateSurfaceTilesAndFolders(withUpdatedNotes, targetHubId) { tiles, folders ->
          val existingTile = tiles.firstOrNull { it.noteId == noteId }
          val nextTiles =
            when {
              pinToSurface && existingTile != null -> {
                if (existingTile.size != normalizedSize) {
                  MetroGridEditor.resizeTileWithReflow(tiles, existingTile.id, normalizedSize)
                } else {
                  tiles
                }
              }
              pinToSurface && existingTile == null -> {
                val (col, row) = MetroGridEditor.findFirstOpenPosition(tiles, normalizedSize.colSpan, normalizedSize.rowSpan)
                tiles +
                  StartTile(
                    id = "tile_${targetHubId ?: "start"}_$noteId",
                    kind = TileKind.NOTE,
                    col = col,
                    row = row,
                    colSpan = normalizedSize.colSpan,
                    rowSpan = normalizedSize.rowSpan,
                    fallbackLabel = updatedNote.title.ifBlank { getApplication<Application>().getString(R.string.quick_note) },
                    noteId = noteId,
                  )
              }
              !pinToSurface && existingTile != null -> tiles.filterNot { it.noteId == noteId }
              else -> tiles
            }
          nextTiles to folders
        }

      layoutRepository.saveLayout(nextLayout).onSuccess { saved ->
        internalState.update { state ->
          state.copy(
            committedLayout = saved,
            noteEditorSession = null,
          )
        }
        showVisualConfirmation(getApplication<Application>().getString(R.string.note_saved))
      }.onFailure {
        showErrorAlert(getApplication<Application>().getString(R.string.save_failed))
      }
    }
  }

  fun removeNoteFromStart(noteId: String, hubId: String? = null) {
    viewModelScope.launch {
      val next =
        updateSurfaceTilesAndFolders(internalState.value.committedLayout, hubId) { tiles, folders ->
          tiles.filterNot { it.noteId == noteId } to folders
        }
      layoutRepository.saveLayout(next).onSuccess { saved ->
        internalState.update { state ->
          state.copy(
            committedLayout = saved,
            draftTiles = if (state.isEditing) state.draftTiles.filterNot { it.noteId == noteId } else state.draftTiles,
            noteEditorSession = null,
          )
        }
      }
    }
  }

  fun deleteNoteWithUndo(noteId: String) {
    viewModelScope.launch {
      val current = internalState.value.committedLayout
      val note = current.notes.firstOrNull { it.id == noteId } ?: return@launch
      val removedStart = current.tiles.filter { it.noteId == noteId }
      val removedHubMap =
        current.hubs.associate { h -> h.id to h.tiles.filter { it.noteId == noteId } }
          .filterValues { it.isNotEmpty() }

      val next =
        current.copy(
          notes = current.notes.filterNot { it.id == noteId },
          tiles = current.tiles.filterNot { it.noteId == noteId },
          hubs = current.hubs.map { h -> h.copy(tiles = h.tiles.filterNot { it.noteId == noteId }) },
        )

      layoutRepository.saveLayout(next).onSuccess { saved ->
        internalState.update { state ->
          state.copy(
            committedLayout = saved,
            draftTiles = if (state.isEditing) state.draftTiles.filterNot { it.noteId == noteId } else state.draftTiles,
            noteEditorSession = null,
            deletedNoteSnapshot = DeletedNoteSnapshot(note, removedStart, removedHubMap),
            visibleAlert =
              LauncherAlert(
                message = getApplication<Application>().getString(R.string.note_deleted),
                isError = false,
                actionLabel = getApplication<Application>().getString(R.string.undo),
                isUndoNoteDelete = true,
              ),
          )
        }
      }
    }
  }

  fun undoDeleteNote() {
    val snapshot = internalState.value.deletedNoteSnapshot ?: return
    viewModelScope.launch {
      val current = internalState.value.committedLayout
      val restoredNotes = listOf(snapshot.note) + current.notes.filterNot { it.id == snapshot.note.id }

      fun restoreList(base: List<StartTile>, deleted: List<StartTile>): List<StartTile> {
        var acc = base
        for (tile in deleted) {
          val free = acc.none { MetroGridEditor.overlaps(it, tile) }
          acc =
            if (free) acc + tile
            else {
              val (c, r) = MetroGridEditor.findFirstOpenPosition(acc, tile.colSpan, tile.rowSpan)
              acc + tile.copy(col = c, row = r)
            }
        }
        return acc
      }

      val next =
        current.copy(
          notes = restoredNotes,
          tiles = restoreList(current.tiles, snapshot.removedStartTiles),
          hubs =
            current.hubs.map { hub ->
              val hubDeleted = snapshot.removedHubTiles[hub.id].orEmpty()
              if (hubDeleted.isEmpty()) hub else hub.copy(tiles = restoreList(hub.tiles, hubDeleted))
            },
        )
      layoutRepository.saveLayout(next).onSuccess { saved ->
        internalState.update {
          it.copy(committedLayout = saved, deletedNoteSnapshot = null, visibleAlert = null, snackbarPrompt = null)
        }
      }
    }
  }

  // --- Stale tile & Optional Photos ---
  fun dismissStaleTilePrompt() {
    internalState.update { it.copy(staleTilePrompt = null) }
  }

  fun removeStaleTile(tile: StartTile) {
    val currentHubId = (internalState.value.surface as? LauncherSurface.Hub)?.hubId
    viewModelScope.launch {
      val next =
        updateSurfaceTilesAndFolders(internalState.value.committedLayout, currentHubId) { tiles, folders ->
          MetroGridEditor.removeTile(tiles, tile.id) to folders
        }
      layoutRepository.saveLayout(next).onSuccess { saved ->
        internalState.update { it.copy(committedLayout = saved, staleTilePrompt = null) }
      }
    }
  }

  fun dismissPhotoActionSheet() {
    internalState.update { it.copy(showPhotoActionSheet = false) }
  }

  fun onPhotosPicked(uris: List<Uri>) {
    if (uris.isEmpty()) return
    val previousPaths = uiState.value.selectedPhotoPaths
    val targetEdge = internalState.value.photoTargetEdgePx
    viewModelScope.launch {
      photoStorageManager.importMultipleBoundedPhotos(uris, previousPaths).onSuccess { (paths, rev) ->
        preferencesRepository.setSelectedPhotos(paths, rev, enableDisplay = true)
        val bmp = photoStorageManager.loadPhotoBitmap(paths.firstOrNull(), targetEdge)
        internalState.update {
          it.copy(activePhotoIndex = 0, photoBitmap = bmp, showPhotoActionSheet = false)
        }
      }.onFailure {
        internalState.update { it.copy(showPhotoActionSheet = false) }
        showToast(getApplication<Application>().getString(R.string.missing_photo))
      }
    }
  }

  fun onPhotoPicked(uri: Uri?) {
    if (uri != null) onPhotosPicked(listOf(uri))
  }

  fun setShowHomePhotos(show: Boolean) {
    if (!show) {
      // Immediately strip photo bitmap from UI memory across transitions
      internalState.update { it.copy(photoBitmap = null) }
    }
    viewModelScope.launch {
      preferencesRepository.setShowHomePhotos(show)
      if (show) refreshPhotoAvailability()
    }
  }

  fun forgetSelectedPhotos() {
    val oldPaths = uiState.value.selectedPhotoPaths
    internalState.update { it.copy(photoBitmap = null, activePhotoIndex = 0) }
    viewModelScope.launch {
      preferencesRepository.forgetSelectedPhotos()
      photoStorageManager.deletePhotoFiles(oldPaths)
      showToast(getApplication<Application>().getString(R.string.photos_forgotten))
    }
  }

  fun setPhotoDisplayMode(mode: PhotoDisplayMode) {
    viewModelScope.launch { preferencesRepository.setPhotoDisplayMode(mode) }
  }

  fun setPhotoCropMode(crop: PhotoCropMode) {
    viewModelScope.launch { preferencesRepository.setPhotoCropMode(crop) }
  }

  fun setShowPhotoCaptions(show: Boolean) {
    viewModelScope.launch { preferencesRepository.setShowPhotoCaptions(show) }
  }

  // --- Pinning to Start or Hub ---
  fun pinAppToStart(app: InstalledApp) = pinAppToSurface(app, hubId = null)

  fun openPinToHubDialog(app: InstalledApp) {
    internalState.update { it.copy(pinToHubTargetApp = app, appContextMenu = null) }
  }

  fun dismissPinToHubDialog() {
    internalState.update { it.copy(pinToHubTargetApp = null) }
  }

  fun pinAppToHub(app: InstalledApp, hubId: String) {
    internalState.update { it.copy(pinToHubTargetApp = null) }
    pinAppToSurface(app, hubId = hubId)
  }

  private fun pinAppToSurface(app: InstalledApp, hubId: String?) {
    val current = internalState.value.committedLayout
    val currentTiles =
      if (hubId == null) current.tiles
      else current.hubs.firstOrNull { it.id == hubId }?.tiles.orEmpty()
    val existing = currentTiles.firstOrNull { it.target == app.key }
    if (existing != null && hubId == null) {
      internalState.update { it.copy(duplicatePinPrompt = existing.id to app.label) }
      return
    }
    viewModelScope.launch {
      val next =
        updateSurfaceTilesAndFolders(current, hubId) { tiles, folders ->
          val (col, row) = MetroGridEditor.findFirstOpenPosition(tiles, 2, 2)
          val newTile =
            StartTile(
              id = "pin_${UUID.randomUUID()}",
              kind = TileKind.APP_SHORTCUT,
              col = col,
              row = row,
              colSpan = 2,
              rowSpan = 2,
              target = app.key,
              fallbackLabel = app.label,
              useOriginalIcon = true,
            )
          (tiles + newTile) to folders
        }
      layoutRepository.saveLayout(next).onSuccess { saved ->
        internalState.update { it.copy(committedLayout = saved) }
        showVisualConfirmation(getApplication<Application>().getString(R.string.pinned_confirmation))
      }.onFailure {
        showErrorAlert(getApplication<Application>().getString(R.string.save_failed))
      }
    }
  }

  fun dismissDuplicatePinPrompt() {
    internalState.update { it.copy(duplicatePinPrompt = null) }
  }

  fun locateExistingTile(tileId: String) {
    internalState.update {
      it.copy(
        duplicatePinPrompt = null,
        surface = LauncherSurface.Start,
        searchQuery = "",
        scrollToTileId = tileId,
        highlightedTileId = tileId,
      )
    }
  }

  fun clearScrollAndHighlight() {
    internalState.update { it.copy(scrollToTileId = null, highlightedTileId = null) }
  }

  fun clearRestoreFocusTileId() {
    internalState.update { it.copy(restoreFocusTileId = null) }
  }

  // --- Shared Edit Mode (Start & Hubs) ---
  fun enterEditMode(initialSelectedTileId: String? = null) {
    if (uiState.value.lockLayout) {
      showToast(getApplication<Application>().getString(R.string.layout_locked_notice))
      return
    }
    internalState.update { state ->
      val currentHubId = (state.surface as? LauncherSurface.Hub)?.hubId
      state.copy(
        isEditing = true,
        editingHubId = currentHubId,
        expandedFolderId = null,
        draftTiles = state.visibleTiles,
        draftFolders = state.visibleFolders,
        selectedTileId = initialSelectedTileId,
        hasPendingChanges = false,
        showResizeSheet = false,
        showMoveSheet = false,
        showTileColorSheet = false,
        showUnsavedDialog = false,
        saveErrorMessage = null,
      )
    }
  }

  fun selectTile(tileId: String?) {
    internalState.update { state ->
      if (!state.isEditing) state else state.copy(selectedTileId = tileId)
    }
  }

  fun openResizeSheet() {
    internalState.update { state ->
      if (state.isEditing && state.selectedTile != null) {
        state.copy(showResizeSheet = true, showMoveSheet = false, showTileColorSheet = false)
      } else {
        state
      }
    }
  }

  fun dismissResizeSheet() = internalState.update { it.copy(showResizeSheet = false) }

  fun openMoveSheet() {
    internalState.update { state ->
      if (state.isEditing && state.selectedTile != null) {
        state.copy(showMoveSheet = true, showResizeSheet = false, showTileColorSheet = false)
      } else {
        state
      }
    }
  }

  fun dismissMoveSheet() = internalState.update { it.copy(showMoveSheet = false) }

  fun openTileColorSheet() {
    internalState.update { state ->
      if (state.isEditing && state.selectedTile != null) {
        state.copy(showTileColorSheet = true, showResizeSheet = false, showMoveSheet = false)
      } else {
        state
      }
    }
  }

  fun dismissTileColorSheet() = internalState.update { it.copy(showTileColorSheet = false) }

  fun setSelectedTileColor(argb: Long?) {
    mutateSelectedDraftTile { it.copy(customColorHex = argb) }
    internalState.update { it.copy(showTileColorSheet = false) }
  }

  fun toggleSelectedTileOriginalIcon() {
    mutateSelectedDraftTile { it.copy(useOriginalIcon = !it.useOriginalIcon) }
  }

  fun toggleSelectedTileLiveMode() {
    mutateSelectedDraftTile { it.copy(isLiveEnabled = !it.isLiveEnabled) }
  }

  private fun mutateSelectedDraftTile(transform: (StartTile) -> StartTile) {
    internalState.update { state ->
      val selectedId = state.selectedTileId ?: return@update state
      val updated = state.draftTiles.map { if (it.id == selectedId) transform(it) else it }
      state.copy(
        draftTiles = updated,
        hasPendingChanges = hasDraftChanged(updated, state.draftFolders, state.editingHubId, state.committedLayout),
      )
    }
  }

  fun resizeSelectedTile(size: TileSize) {
    internalState.update { state ->
      val selectedId = state.selectedTileId ?: return@update state
      val tile = state.draftTiles.firstOrNull { it.id == selectedId } ?: return@update state
      if (tile.kind == TileKind.NOTE && size == TileSize.SMALL) {
        return@update state.copy(showResizeSheet = false)
      }
      val updated = MetroGridEditor.resizeTileWithReflow(state.draftTiles, selectedId, size)
      state.copy(
        draftTiles = updated,
        hasPendingChanges = hasDraftChanged(updated, state.draftFolders, state.editingHubId, state.committedLayout),
        showResizeSheet = false,
      )
    }
  }

  fun moveTileOrMerge(tileId: String, targetCol: Int, targetRow: Int) {
    internalState.update { state ->
      val dragged = state.draftTiles.firstOrNull { it.id == tileId } ?: return@update state
      val mergeTarget = MetroGridEditor.findMergeTargetAtCell(state.draftTiles, dragged, targetCol, targetRow)

      if (mergeTarget != null) {
        if (mergeTarget.kind == TileKind.FOLDER && mergeTarget.folderId != null && dragged.target != null) {
          val folderId = mergeTarget.folderId
          val updatedFolders =
            state.draftFolders.map { folder ->
              if (folder.id == folderId && dragged.target !in folder.appKeys) {
                folder.copy(appKeys = folder.appKeys + dragged.target)
              } else {
                folder
              }
            }
          val updatedTiles = state.draftTiles.filterNot { it.id == dragged.id }
          return@update state.copy(
            draftTiles = updatedTiles,
            draftFolders = updatedFolders,
            selectedTileId = mergeTarget.id,
            hasPendingChanges = hasDraftChanged(updatedTiles, updatedFolders, state.editingHubId, state.committedLayout),
          )
        } else if (mergeTarget.kind == TileKind.APP_SHORTCUT && mergeTarget.target != null && dragged.target != null) {
          return@update state.copy(pendingDragMergePair = dragged.id to mergeTarget.id)
        }
      }

      val updated = MetroGridEditor.moveTileWithReflow(state.draftTiles, tileId, targetCol, targetRow)
      state.copy(
        draftTiles = updated,
        hasPendingChanges = hasDraftChanged(updated, state.draftFolders, state.editingHubId, state.committedLayout),
      )
    }
  }

  fun stepMoveSelectedTile(deltaCol: Int, deltaRow: Int) {
    internalState.update { state ->
      val selectedId = state.selectedTileId ?: return@update state
      val updated = MetroGridEditor.stepMoveTile(state.draftTiles, selectedId, deltaCol, deltaRow)
      state.copy(
        draftTiles = updated,
        hasPendingChanges = hasDraftChanged(updated, state.draftFolders, state.editingHubId, state.committedLayout),
      )
    }
  }

  fun unpinSelectedTile() {
    internalState.update { state ->
      val selectedId = state.selectedTileId ?: return@update state
      val targetTile = state.draftTiles.firstOrNull { it.id == selectedId }
      val updated = MetroGridEditor.removeTile(state.draftTiles, selectedId)
      val nextFolders =
        if (targetTile?.folderId != null) {
          state.draftFolders.filterNot { it.id == targetTile.folderId }
        } else {
          state.draftFolders
        }
      state.copy(
        draftTiles = updated,
        draftFolders = nextFolders,
        selectedTileId = null,
        showResizeSheet = false,
        showMoveSheet = false,
        showTileColorSheet = false,
        hasPendingChanges = hasDraftChanged(updated, nextFolders, state.editingHubId, state.committedLayout),
      )
    }
  }

  fun commitEdits() {
    val snapshot = internalState.value
    if (!snapshot.isEditing || snapshot.isSaving) return
    internalState.update { it.copy(isSaving = true, saveErrorMessage = null, showUnsavedDialog = false) }
    val hubId = snapshot.editingHubId

    viewModelScope.launch {
      val currentCommitted = snapshot.committedLayout
      val prevWidgetIds =
        (if (hubId == null) currentCommitted.tiles else currentCommitted.hubs.firstOrNull { it.id == hubId }?.tiles.orEmpty())
          .mapNotNull { it.appWidgetId }.toSet()
      val nextWidgetIds = snapshot.draftTiles.mapNotNull { it.appWidgetId }.toSet()
      (prevWidgetIds - nextWidgetIds).forEach { appWidgetController.deleteAppWidgetId(it) }

      val candidateLayout =
        updateSurfaceTilesAndFolders(currentCommitted, hubId) { _, _ ->
          snapshot.draftTiles to snapshot.draftFolders
        }
      layoutRepository.saveLayout(candidateLayout).onSuccess { saved ->
        internalState.update { state ->
          val focusTarget = state.selectedTileId?.takeIf { id -> state.draftTiles.any { it.id == id } }
          state.copy(
            isEditing = false,
            editingHubId = null,
            isSaving = false,
            committedLayout = saved,
            draftTiles = emptyList(),
            draftFolders = emptyList(),
            selectedTileId = null,
            hasPendingChanges = false,
            showResizeSheet = false,
            showMoveSheet = false,
            showTileColorSheet = false,
            saveErrorMessage = null,
            restoreFocusTileId = focusTarget,
          )
        }
      }.onFailure { err ->
        internalState.update {
          it.copy(isSaving = false, saveErrorMessage = err.message ?: getApplication<Application>().getString(R.string.save_failed))
        }
      }
    }
  }

  fun dismissSaveError() = internalState.update { it.copy(saveErrorMessage = null) }

  fun handleEditBackPressed() {
    val state = internalState.value
    when {
      !state.isEditing -> Unit
      state.showResizeSheet -> dismissResizeSheet()
      state.showMoveSheet -> dismissMoveSheet()
      state.showTileColorSheet -> dismissTileColorSheet()
      state.showUnsavedDialog -> dismissUnsavedDialog()
      state.hasPendingChanges -> internalState.update { it.copy(showUnsavedDialog = true) }
      else -> discardEdits()
    }
  }

  fun dismissUnsavedDialog() = internalState.update { it.copy(showUnsavedDialog = false) }

  fun discardEdits() {
    internalState.update { state ->
      val focusTarget = state.selectedTileId?.takeIf { id -> state.visibleTiles.any { it.id == id } }
      state.copy(
        isEditing = false,
        editingHubId = null,
        draftTiles = emptyList(),
        draftFolders = emptyList(),
        selectedTileId = null,
        hasPendingChanges = false,
        showResizeSheet = false,
        showMoveSheet = false,
        showTileColorSheet = false,
        showUnsavedDialog = false,
        saveErrorMessage = null,
        pendingDragMergePair = null,
        restoreFocusTileId = focusTarget,
      )
    }
  }

  // --- Accessible Tile Actions (works without drag or long-press) ---
  fun openAccessibleMoveForTile(tile: StartTile) {
    if (uiState.value.lockLayout) {
      showErrorAlert(getApplication<Application>().getString(R.string.layout_locked_notice))
      return
    }
    if (!internalState.value.isEditing) {
      enterEditMode(initialSelectedTileId = tile.id)
    } else {
      selectTile(tile.id)
    }
    openMoveSheet()
  }

  fun openAccessibleResizeForTile(tile: StartTile) {
    if (uiState.value.lockLayout) {
      showErrorAlert(getApplication<Application>().getString(R.string.layout_locked_notice))
      return
    }
    if (!internalState.value.isEditing) {
      enterEditMode(initialSelectedTileId = tile.id)
    } else {
      selectTile(tile.id)
    }
    openResizeSheet()
  }

  fun unpinTileAccessible(tile: StartTile) {
    if (uiState.value.lockLayout) {
      showErrorAlert(getApplication<Application>().getString(R.string.layout_locked_notice))
      return
    }
    if (internalState.value.isEditing) {
      selectTile(tile.id)
      unpinSelectedTile()
      return
    }
    val currentHubId = (internalState.value.surface as? LauncherSurface.Hub)?.hubId
    viewModelScope.launch {
      tile.appWidgetId?.let { appWidgetController.deleteAppWidgetId(it) }
      val next =
        updateSurfaceTilesAndFolders(internalState.value.committedLayout, currentHubId) { tiles, folders ->
          val updatedTiles = MetroGridEditor.removeTile(tiles, tile.id)
          val updatedFolders =
            if (tile.folderId != null) folders.filterNot { it.id == tile.folderId } else folders
          updatedTiles to updatedFolders
        }
      layoutRepository.saveLayout(next).onSuccess { saved ->
        internalState.update { it.copy(committedLayout = saved) }
      }.onFailure {
        showErrorAlert(getApplication<Application>().getString(R.string.save_failed))
      }
    }
  }

  // --- Personalization, Theme, Performance, Profile & Accessibility ---
  fun setAccent(accent: MetroAccent) { viewModelScope.launch { preferencesRepository.setAccent(accent) } }
  fun setCustomAccentHex(argb: Long) { viewModelScope.launch { preferencesRepository.setCustomAccentHex(argb) } }
  fun setThemeMode(mode: AppThemeMode) { viewModelScope.launch { preferencesRepository.setThemeMode(mode) } }
  fun setBackgroundHex(argb: Long) { viewModelScope.launch { preferencesRepository.setBackgroundHex(argb) } }
  fun setGutterDp(dp: Int) { viewModelScope.launch { preferencesRepository.setGutterDp(dp) } }
  fun setShowLabels(show: Boolean) { viewModelScope.launch { preferencesRepository.setShowLabels(show) } }
  fun setLockLayout(locked: Boolean) { viewModelScope.launch { preferencesRepository.setLockLayout(locked) } }
  fun setReduceMotion(enabled: Boolean) { viewModelScope.launch { preferencesRepository.setReduceMotion(enabled) } }
  fun setLightweightMode(mode: LightweightMode) { viewModelScope.launch { preferencesRepository.setLightweightMode(mode) } }
  fun setLightMode(enabled: Boolean) { viewModelScope.launch { preferencesRepository.setLightMode(enabled) } }
  fun setLeftCommandSlot(slot: LeftCommandSlot) { viewModelScope.launch { preferencesRepository.setLeftCommandSlot(slot) } }
  fun setThemeThirdPartyIcons(enabled: Boolean) { viewModelScope.launch { preferencesRepository.setThemeThirdPartyIcons(enabled) } }
  fun setCalendarAccessEnabled(enabled: Boolean) { viewModelScope.launch { preferencesRepository.setCalendarAccessEnabled(enabled) } }
  fun setNotificationsAccessEnabled(enabled: Boolean) { viewModelScope.launch { preferencesRepository.setNotificationsAccessEnabled(enabled) } }
  fun setShowPrivatePreviews(show: Boolean) { viewModelScope.launch { preferencesRepository.setShowPrivatePreviews(show) } }
  fun setVibrationEnabled(enabled: Boolean) { viewModelScope.launch { preferencesRepository.setVibrationEnabled(enabled) } }

  // --- Local Profile ---
  fun setProfileName(name: String) {
    viewModelScope.launch {
      preferencesRepository.setProfileName(name)
      showVisualConfirmation(getApplication<Application>().getString(R.string.profile_saved))
    }
  }

  fun setProfileAge(rawAge: String): Boolean {
    val trimmed = rawAge.trim()
    if (trimmed.isEmpty()) {
      viewModelScope.launch {
        preferencesRepository.setProfileAge(null)
        showVisualConfirmation(getApplication<Application>().getString(R.string.profile_saved))
      }
      return true
    }
    val parsed = trimmed.toIntOrNull()
    if (parsed == null || parsed !in 1..120) {
      showErrorAlert(getApplication<Application>().getString(R.string.profile_invalid_age))
      return false
    }
    viewModelScope.launch {
      preferencesRepository.setProfileAge(parsed)
      showVisualConfirmation(getApplication<Application>().getString(R.string.profile_saved))
    }
    return true
  }

  fun clearProfileAge() {
    viewModelScope.launch {
      preferencesRepository.setProfileAge(null)
      showVisualConfirmation(getApplication<Application>().getString(R.string.profile_saved))
    }
  }

  fun setShowProfileOnStart(show: Boolean) {
    viewModelScope.launch { preferencesRepository.setShowProfileOnStart(show) }
  }

  fun onProfilePhotoPicked(uri: Uri?) {
    if (uri == null) {
      showVisualConfirmation(getApplication<Application>().getString(R.string.profile_photo_cancelled))
      return
    }
    internalState.update { it.copy(pendingProfileCrop = PendingProfileCrop(uri = uri, previewBitmap = null)) }
    viewModelScope.launch {
      val preview = photoStorageManager.loadCropPreviewBitmap(uri)
      if (preview == null) {
        internalState.update { it.copy(pendingProfileCrop = null) }
        showErrorAlert(getApplication<Application>().getString(R.string.missing_photo))
      } else {
        internalState.update { state ->
          val active = state.pendingProfileCrop
          if (active?.uri == uri) state.copy(pendingProfileCrop = active.copy(previewBitmap = preview))
          else state
        }
      }
    }
  }

  fun confirmProfileCrop(normCenterX: Float, normCenterY: Float, zoom: Float) {
    val pending = internalState.value.pendingProfileCrop ?: return
    val previousPath = uiState.value.profile.avatarPath
    internalState.update { it.copy(pendingProfileCrop = pending.copy(isSaving = true)) }
    viewModelScope.launch {
      photoStorageManager
        .importCroppedProfileAvatar(
          uri = pending.uri,
          normCenterX = normCenterX,
          normCenterY = normCenterY,
          zoom = zoom,
          targetPx = PhotoStorageManager.PROFILE_AVATAR_PX,
          previousPath = previousPath,
        )
        .onSuccess { (path, rev) ->
          preferencesRepository.setProfileAvatar(path, rev)
          val avatarBmp = photoStorageManager.loadProfileAvatar(path, PhotoStorageManager.PROFILE_AVATAR_PX)
          internalState.update {
            it.copy(profileAvatarBitmap = avatarBmp, pendingProfileCrop = null)
          }
          showVisualConfirmation(getApplication<Application>().getString(R.string.profile_saved))
        }
        .onFailure {
          internalState.update { it.copy(pendingProfileCrop = null) }
          showErrorAlert(getApplication<Application>().getString(R.string.missing_photo))
        }
    }
  }

  fun cancelProfileCrop() {
    internalState.update { it.copy(pendingProfileCrop = null) }
    showVisualConfirmation(getApplication<Application>().getString(R.string.profile_photo_cancelled))
  }

  fun removeProfilePhoto() {
    val oldPath = uiState.value.profile.avatarPath
    internalState.update { it.copy(profileAvatarBitmap = null, pendingProfileCrop = null) }
    viewModelScope.launch {
      preferencesRepository.setProfileAvatar(null, System.currentTimeMillis())
      photoStorageManager.deleteProfileAvatar(oldPath)
      showVisualConfirmation(getApplication<Application>().getString(R.string.profile_photo_removed))
    }
  }

  fun dismissAlert() = internalState.update { it.copy(visibleAlert = null, snackbarPrompt = null) }
  fun clearSnackbarPrompt() = dismissAlert()

  fun resolveTileLabel(tile: StartTile, installedApps: List<InstalledApp>, context: Context): String {
    if (tile.kind == TileKind.FOLDER) {
      val folder = uiState.value.visibleFolders.firstOrNull { it.id == tile.folderId }
      return folder?.name ?: tile.fallbackLabel.ifBlank { context.getString(R.string.folders) }
    }
    if (tile.kind == TileKind.NOTE) {
      val note = uiState.value.visibleNotes.firstOrNull { it.id == tile.noteId }
      return note?.title?.takeIf { it.isNotBlank() } ?: context.getString(R.string.quick_note)
    }
    if (tile.kind == TileKind.HUB) {
      val hub = uiState.value.committedLayout.hubs.firstOrNull { it.id == tile.hubId }
      return hub?.name ?: tile.fallbackLabel.ifBlank { context.getString(R.string.hubs) }
    }
    if (tile.kind == TileKind.APP_SHORTCUT && tile.target != null) {
      val app = installedApps.firstOrNull { it.key == tile.target }
      if (app != null) {
        return if (app.isWorkProfile) "${app.label} (${context.getString(R.string.work_profile_badge)})" else app.label
      }
    }
    return when (tile.kind) {
      TileKind.CLOCK -> context.getString(R.string.clock)
      TileKind.BATTERY -> context.getString(R.string.battery)
      TileKind.PHOTO -> context.getString(R.string.photo)
      TileKind.CALENDAR -> context.getString(R.string.calendar)
      TileKind.MUSIC -> context.getString(R.string.music_tile)
      TileKind.NOTIFICATIONS -> context.getString(R.string.notifications_tile)
      TileKind.SYSTEM_WIDGET -> tile.fallbackLabel.ifBlank { context.getString(R.string.system_widget) }
      TileKind.APP_SHORTCUT -> tile.fallbackLabel
      TileKind.FOLDER -> context.getString(R.string.folders)
      TileKind.NOTE -> context.getString(R.string.quick_note)
      TileKind.HUB -> context.getString(R.string.hubs)
    }
  }

  private fun showVisualConfirmation(message: String) {
    vibrateConfirmationIfEnabled()
    internalState.update {
      it.copy(
        visibleAlert = LauncherAlert(message = message, isError = false),
        snackbarPrompt = null,
      )
    }
  }

  private fun showErrorAlert(message: String) {
    internalState.update {
      it.copy(
        visibleAlert = LauncherAlert(message = message, isError = true),
        snackbarPrompt = null,
      )
    }
  }

  private fun showToast(message: String) {
    internalState.update {
      it.copy(
        visibleAlert = LauncherAlert(message = message, isError = false),
        snackbarPrompt = null,
      )
    }
  }

  private fun vibrateConfirmationIfEnabled() {
    if (!uiState.value.vibrationEnabled) return
    runCatching {
      val appContext = getApplication<Application>()
      val vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          appContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
          @Suppress("DEPRECATION")
          appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
      if (vibrator?.hasVibrator() == true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator.vibrate(VibrationEffect.createOneShot(25L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
          @Suppress("DEPRECATION")
          vibrator.vibrate(25L)
        }
      }
    }
  }

  private fun updateSurfaceTilesAndFolders(
    current: StartLayoutState,
    hubId: String?,
    block: (List<StartTile>, List<MetroFolder>) -> Pair<List<StartTile>, List<MetroFolder>>,
  ): StartLayoutState {
    return if (hubId == null) {
      val (nextTiles, nextFolders) = block(current.tiles, current.folders)
      current.copy(tiles = nextTiles, folders = nextFolders)
    } else {
      current.copy(
        hubs =
          current.hubs.map { hub ->
            if (hub.id == hubId) {
              val (nt, nf) = block(hub.tiles, hub.folders)
              hub.copy(tiles = nt, folders = nf)
            } else {
              hub
            }
          }
      )
    }
  }

  private fun hasDraftChanged(
    tiles: List<StartTile>,
    folders: List<MetroFolder>,
    editingHubId: String?,
    committed: StartLayoutState,
  ): Boolean {
    return if (editingHubId == null) {
      tiles != committed.tiles || folders != committed.folders
    } else {
      val hub = committed.hubs.firstOrNull { it.id == editingHubId }
      tiles != (hub?.tiles ?: emptyList<StartTile>()) || folders != (hub?.folders ?: emptyList<MetroFolder>())
    }
  }

  private fun filterApps(apps: List<InstalledApp>, query: String): List<InstalledApp> {
    if (query.isBlank()) return apps
    val normalizedQuery = AppCatalogManager.normalizeForSearch(query)
    if (normalizedQuery.isBlank()) return apps
    return apps.filter { it.normalizedLabel.contains(normalizedQuery) }
  }

  companion object {
    private const val LAUNCH_DEBOUNCE_MS = 450L
    private const val PHOTO_ROTATION_INTERVAL_MS = 12_000L
  }
}
