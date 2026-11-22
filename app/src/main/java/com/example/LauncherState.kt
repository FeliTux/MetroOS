package com.example

import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.example.model.AppRepairProposal
import com.example.model.AppTargetKey
import com.example.model.AppThemeMode
import com.example.model.DeepShortcutItem
import com.example.model.InstalledApp
import com.example.model.LauncherSurface
import com.example.model.LeftCommandSlot
import com.example.model.LightweightMode
import com.example.model.LiveTilesState
import com.example.model.LocalUserProfile
import com.example.model.MetroAccent
import com.example.model.MetroFolder
import com.example.model.MetroHub
import com.example.model.PhotoCropMode
import com.example.model.PhotoDisplayMode
import com.example.model.QuickNote
import com.example.model.StartLayoutState
import com.example.model.StartTile
import com.example.model.TileSize

data class NoteEditorSession(
  val noteId: String? = null,
  val title: String = "",
  val body: String = "",
  val isChecklist: Boolean = false,
  val isPinnedToStart: Boolean = true,
  val targetHubId: String? = null,
  val preferredSize: TileSize = TileSize.MEDIUM,
)

data class DeletedNoteSnapshot(
  val note: QuickNote,
  val removedStartTiles: List<StartTile>,
  val removedHubTiles: Map<String, List<StartTile>>,
)

data class SnackbarPrompt(
  val message: String,
  val actionLabel: String? = null,
  val isUndoNoteDelete: Boolean = false,
)

data class LauncherAlert(
  val id: Long = System.currentTimeMillis(),
  val message: String,
  val isError: Boolean = false,
  val actionLabel: String? = null,
  val isUndoNoteDelete: Boolean = false,
)

data class PendingProfileCrop(
  val uri: Uri,
  val previewBitmap: ImageBitmap? = null,
  val isSaving: Boolean = false,
)

data class AppContextMenuState(
  val app: InstalledApp,
  val deepShortcuts: List<DeepShortcutItem> = emptyList(),
  val isLoadingShortcuts: Boolean = false,
)

data class LauncherUiState(
  val isLoading: Boolean = true,
  val surface: LauncherSurface = LauncherSurface.Start,
  val committedLayout: StartLayoutState = StartLayoutState(),
  val installedApps: List<InstalledApp> = emptyList(),
  val searchQuery: String = "",
  val filteredApps: List<InstalledApp> = emptyList(),
  val iconCacheGeneration: Long = 1L,
  // Personalization, theme & performance preferences
  val accent: MetroAccent = MetroAccent.METRO_BLUE,
  val customAccentHex: Long? = null,
  val effectiveAccentColor: Color = MetroAccent.METRO_BLUE.color,
  val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  val backgroundHex: Long = 0xFF000000L,
  val effectiveBackgroundColor: Color = Color.Black,
  val gutterDp: Int = 4,
  val showLabels: Boolean = true,
  val lockLayout: Boolean = false,
  val reduceMotion: Boolean = false,
  val lightweightMode: LightweightMode = LightweightMode.AUTO,
  val lightMode: Boolean = false,
  val isLowRamDevice: Boolean = false,
  val leftCommandSlot: LeftCommandSlot = LeftCommandSlot.SEARCH,
  val themeThirdPartyIcons: Boolean = false,
  // Local Profile (strictly offline & separate from Start photos)
  val profile: LocalUserProfile = LocalUserProfile(),
  val profileAvatarBitmap: ImageBitmap? = null,
  val pendingProfileCrop: PendingProfileCrop? = null,
  // Accessibility & Alerts
  val vibrationEnabled: Boolean = false,
  val systemAccessibilityEnabled: Boolean = false,
  val visibleAlert: LauncherAlert? = null,
  // Photos (off by default on fresh install)
  val showHomePhotos: Boolean = false,
  val selectedPhotoPaths: List<String> = emptyList(),
  val activePhotoIndex: Int = 0,
  val photoDisplayMode: PhotoDisplayMode = PhotoDisplayMode.STATIC,
  val photoCropMode: PhotoCropMode = PhotoCropMode.CENTER,
  val showPhotoCaptions: Boolean = true,
  val photoBitmap: ImageBitmap? = null,
  val photoTargetEdgePx: Int = 720,
  // Optional permissions / privacy
  val calendarAccessEnabled: Boolean = false,
  val notificationsAccessEnabled: Boolean = false,
  val showPrivatePreviews: Boolean = false,
  // Live tile state
  val liveTiles: LiveTilesState = LiveTilesState(),
  // Edit mode state (shared for Start or active Hub)
  val isEditing: Boolean = false,
  val editingHubId: String? = null,
  val draftTiles: List<StartTile> = emptyList(),
  val draftFolders: List<MetroFolder> = emptyList(),
  val selectedTileId: String? = null,
  val hasPendingChanges: Boolean = false,
  val isSaving: Boolean = false,
  val showResizeSheet: Boolean = false,
  val showMoveSheet: Boolean = false,
  val showTileColorSheet: Boolean = false,
  val showUnsavedDialog: Boolean = false,
  val saveErrorMessage: String? = null,
  // Folders state
  val expandedFolderId: String? = null,
  val showCreateFolderDialog: Boolean = false,
  val pendingDragMergePair: Pair<String, String>? = null,
  val renameFolderTarget: MetroFolder? = null,
  val addAppsFolderTarget: MetroFolder? = null,
  // Hubs state
  val showCreateHubDialog: Boolean = false,
  val renameHubTarget: MetroHub? = null,
  val hubColorTarget: MetroHub? = null,
  val showAddToHubSheet: String? = null,
  val pinToHubTargetApp: InstalledApp? = null,
  val startSavedScrollIndex: Int = 0,
  val startSavedScrollOffset: Int = 0,
  // Notes state
  val noteEditorSession: NoteEditorSession? = null,
  val showSavedNotesSheet: Boolean = false,
  val deletedNoteSnapshot: DeletedNoteSnapshot? = null,
  // Shortcut Repair & Contextual Dialogs
  val repairProposals: Map<String, AppRepairProposal> = emptyMap(),
  val activeRepairProposal: AppRepairProposal? = null,
  val staleTilePrompt: StartTile? = null,
  val duplicatePinPrompt: Pair<String, String>? = null,
  val showPhotoActionSheet: Boolean = false,
  val appContextMenu: AppContextMenuState? = null,
  val scrollToTileId: String? = null,
  val highlightedTileId: String? = null,
  val restoreFocusTileId: String? = null,
  val isDefaultHome: Boolean = false,
  val snackbarPrompt: SnackbarPrompt? = null,
) {
  val activeHub: MetroHub?
    get() {
      val hubId = (surface as? LauncherSurface.Hub)?.hubId ?: return null
      return committedLayout.hubs.firstOrNull { it.id == hubId }
    }

  val visibleStartTiles: List<StartTile>
    get() = if (isEditing && editingHubId == null) draftTiles else committedLayout.tiles

  val visibleStartFolders: List<MetroFolder>
    get() = if (isEditing && editingHubId == null) draftFolders else committedLayout.folders

  val visibleTiles: List<StartTile>
    get() {
      val currentHubId = (surface as? LauncherSurface.Hub)?.hubId
      return if (currentHubId == null) {
        visibleStartTiles
      } else if (isEditing && editingHubId == currentHubId) {
        draftTiles
      } else {
        committedLayout.hubs.firstOrNull { it.id == currentHubId }?.tiles.orEmpty()
      }
    }

  val visibleFolders: List<MetroFolder>
    get() {
      val currentHubId = (surface as? LauncherSurface.Hub)?.hubId
      return if (currentHubId == null) {
        visibleStartFolders
      } else if (isEditing && editingHubId == currentHubId) {
        draftFolders
      } else {
        committedLayout.hubs.firstOrNull { it.id == currentHubId }?.folders.orEmpty()
      }
    }

  val visibleNotes: List<QuickNote>
    get() = committedLayout.notes

  val selectedTile: StartTile?
    get() = visibleTiles.firstOrNull { it.id == selectedTileId }

  val effectiveReducedMotion: Boolean
    get() = reduceMotion || lightMode

  val effectivePhotoMode: PhotoDisplayMode
    get() = if (lightMode) PhotoDisplayMode.STATIC else photoDisplayMode

  val effectiveAccentForActiveSurface: Color
    get() = activeHub?.accentHex?.let { Color(it) } ?: effectiveAccentColor
}
