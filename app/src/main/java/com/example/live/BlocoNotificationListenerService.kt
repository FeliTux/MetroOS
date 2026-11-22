package com.example.live

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.ui.graphics.asImageBitmap
import com.example.model.MediaSessionState
import com.example.model.NotificationSummaryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BlocoNotificationListenerService : NotificationListenerService() {

  private var sessionListener: MediaSessionManager.OnActiveSessionsChangedListener? = null
  private var activeController: MediaController? = null
  private val controllerCallback =
    object : MediaController.Callback() {
      override fun onPlaybackStateChanged(state: PlaybackState?) {
        publishMediaFromController(activeController)
      }

      override fun onMetadataChanged(metadata: MediaMetadata?) {
        publishMediaFromController(activeController)
      }
    }

  override fun onListenerConnected() {
    super.onListenerConnected()
    isConnectedFlag = true
    refreshNotifications()
    registerMediaSessionWatcher()
  }

  override fun onListenerDisconnected() {
    super.onListenerDisconnected()
    isConnectedFlag = false
    unregisterMediaSessionWatcher()
    _notificationsState.value = NotificationSummaryState(hasPermission = isAccessGranted(this))
    _mediaState.value = MediaSessionState()
  }

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    refreshNotifications()
  }

  override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    refreshNotifications()
  }

  private fun refreshNotifications() {
    val active = runCatching { activeNotifications?.toList().orEmpty() }.getOrDefault(emptyList())
    val filtered =
      active.filter { sbn ->
        sbn.packageName != packageName && !sbn.isOngoing
      }
    val latest = filtered.maxByOrNull { it.postTime }
    val latestTitle =
      latest?.notification?.extras?.getCharSequence("android.title")?.toString()?.trim()?.takeIf { it.isNotEmpty() }
    val latestApp =
      latest?.packageName?.let { pkg ->
        runCatching {
          val appInfo = packageManager.getApplicationInfo(pkg, 0)
          packageManager.getApplicationLabel(appInfo).toString()
        }.getOrNull() ?: pkg
      }

    _notificationsState.value =
      NotificationSummaryState(
        hasPermission = true,
        activeCount = filtered.size,
        latestTitle = latestTitle,
        latestAppName = latestApp,
      )
  }

  private fun registerMediaSessionWatcher() {
    val manager = getSystemService(MediaSessionManager::class.java) ?: return
    val component = ComponentName(this, BlocoNotificationListenerService::class.java)
    val listener =
      MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        bindPrimaryController(controllers.orEmpty())
      }
    sessionListener = listener
    runCatching {
      manager.addOnActiveSessionsChangedListener(listener, component)
      bindPrimaryController(manager.getActiveSessions(component))
    }
  }

  private fun unregisterMediaSessionWatcher() {
    val manager = getSystemService(MediaSessionManager::class.java)
    sessionListener?.let { listener ->
      runCatching { manager?.removeOnActiveSessionsChangedListener(listener) }
    }
    sessionListener = null
    runCatching { activeController?.unregisterCallback(controllerCallback) }
    activeController = null
    activeMediaControllerRef = null
  }

  private fun bindPrimaryController(controllers: List<MediaController>) {
    runCatching { activeController?.unregisterCallback(controllerCallback) }
    val chosen =
      controllers.firstOrNull { c ->
        c.playbackState?.state == PlaybackState.STATE_PLAYING
      } ?: controllers.firstOrNull()
    activeController = chosen
    activeMediaControllerRef = chosen
    if (chosen != null) {
      runCatching { chosen.registerCallback(controllerCallback) }
      publishMediaFromController(chosen)
    } else {
      _mediaState.value = MediaSessionState()
    }
  }

  private fun publishMediaFromController(controller: MediaController?) {
    if (controller == null) {
      _mediaState.value = MediaSessionState()
      return
    }
    val meta = controller.metadata
    val playback = controller.playbackState
    val state = playback?.state ?: PlaybackState.STATE_NONE
    val isPlaying = state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
    val actions = playback?.actions ?: 0L
    val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
    val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
    val pkg = controller.packageName
    val playerLabel =
      runCatching {
        val ai = packageManager.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
        packageManager.getApplicationLabel(ai).toString()
      }.getOrElse {
        runCatching {
          @Suppress("DEPRECATION")
          packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)
      }

    val rawArt =
      meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
    val boundedArt =
      rawArt?.let { src ->
        val maxPx = 192
        if (src.width <= maxPx && src.height <= maxPx) {
          src.asImageBitmap()
        } else {
          val scale = minOf(maxPx.toFloat() / src.width, maxPx.toFloat() / src.height)
          val w = (src.width * scale).toInt().coerceAtLeast(1)
          val h = (src.height * scale).toInt().coerceAtLeast(1)
          Bitmap.createScaledBitmap(src, w, h, true).asImageBitmap()
        }
      }

    val hasContent = title.isNotBlank() || isPlaying || state == PlaybackState.STATE_PAUSED
    _mediaState.value =
      MediaSessionState(
        isActive = hasContent,
        isPlaying = isPlaying,
        playerLabel = playerLabel,
        trackTitle = title.ifBlank { playerLabel },
        artist = artist,
        packageName = pkg,
        albumArt = boundedArt,
        canPlayPause =
          (actions and (PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE)) != 0L,
        canSkipPrevious = (actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0L,
        canSkipNext = (actions and PlaybackState.ACTION_SKIP_TO_NEXT) != 0L,
      )
  }

  companion object {
    @Volatile
    private var isConnectedFlag = false

    @Volatile
    private var activeMediaControllerRef: MediaController? = null

    private val _notificationsState = MutableStateFlow(NotificationSummaryState())
    val notificationsState: StateFlow<NotificationSummaryState> = _notificationsState.asStateFlow()

    private val _mediaState = MutableStateFlow(MediaSessionState())
    val mediaState: StateFlow<MediaSessionState> = _mediaState.asStateFlow()

    fun isAccessGranted(context: Context): Boolean {
      if (isConnectedFlag) return true
      val enabledListeners =
        runCatching {
          Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        }.getOrNull().orEmpty()
      val flat = ComponentName(context, BlocoNotificationListenerService::class.java).flattenToString()
      return enabledListeners.split(':').any { it.equals(flat, ignoreCase = true) }
    }

    fun togglePlayPause() {
      val ctrl = activeMediaControllerRef ?: return
      val playing = ctrl.playbackState?.state == PlaybackState.STATE_PLAYING
      runCatching {
        if (playing) ctrl.transportControls.pause() else ctrl.transportControls.play()
      }
    }

    fun skipPrevious() {
      val ctrl = activeMediaControllerRef ?: return
      runCatching { ctrl.transportControls.skipToPrevious() }
    }

    fun skipNext() {
      val ctrl = activeMediaControllerRef ?: return
      runCatching { ctrl.transportControls.skipToNext() }
    }
  }
}
