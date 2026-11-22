package com.example.live

import android.Manifest
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.provider.CalendarContract
import android.provider.Settings
import android.text.format.DateFormat
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.example.model.LiveTilesState
import com.example.model.MediaSessionState
import com.example.model.NotificationSummaryState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LiveTileObserver(private val context: Context) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  private val _state = MutableStateFlow(LiveTilesState())
  val state: StateFlow<LiveTilesState> = _state.asStateFlow()

  private val _systemReducedMotion = MutableStateFlow(readSystemReducedMotion())
  val systemReducedMotion: StateFlow<Boolean> = _systemReducedMotion.asStateFlow()

  private val _systemAccessibilityEnabled = MutableStateFlow(readSystemAccessibilityActive())
  val systemAccessibilityEnabled: StateFlow<Boolean> = _systemAccessibilityEnabled.asStateFlow()

  private var registered = false
  private var calendarEnabled = false
  private var notificationsEnabled = false
  private var privatePreviewsEnabled = false
  private var calendarJob: Job? = null
  private var listenerJob: Job? = null

  private val receiver =
    object : BroadcastReceiver() {
      override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
          Intent.ACTION_BATTERY_CHANGED -> updateBatteryFromIntent(intent)
          Intent.ACTION_TIME_TICK,
          Intent.ACTION_TIME_CHANGED,
          Intent.ACTION_TIMEZONE_CHANGED,
          Intent.ACTION_DATE_CHANGED -> {
            refreshDateTime()
            refreshNextCalendarEvent()
          }
        }
      }
    }

  fun configurePrivacyAndAccess(
    calendarAccessEnabled: Boolean,
    notificationsAccessEnabled: Boolean,
    showPrivatePreviews: Boolean,
  ) {
    val changed =
      calendarEnabled != calendarAccessEnabled ||
        notificationsEnabled != notificationsAccessEnabled ||
        privatePreviewsEnabled != showPrivatePreviews
    calendarEnabled = calendarAccessEnabled
    notificationsEnabled = notificationsAccessEnabled
    privatePreviewsEnabled = showPrivatePreviews
    if (changed) {
      refreshAll()
    }
  }

  fun startObserving() {
    if (!registered) {
      val filter =
        IntentFilter().apply {
          addAction(Intent.ACTION_TIME_TICK)
          addAction(Intent.ACTION_TIME_CHANGED)
          addAction(Intent.ACTION_TIMEZONE_CHANGED)
          addAction(Intent.ACTION_DATE_CHANGED)
          addAction(Intent.ACTION_BATTERY_CHANGED)
        }
      val stickyBattery = runCatching { context.registerReceiver(receiver, filter) }.getOrNull()
      registered = true
      if (stickyBattery != null) {
        updateBatteryFromIntent(stickyBattery)
      }
    }

    if (listenerJob == null) {
      listenerJob =
        scope.launch {
          combine(
            BlocoNotificationListenerService.mediaState,
            BlocoNotificationListenerService.notificationsState,
          ) { media, notif -> media to notif }
            .collect { (media, notif) ->
              publishExternalSessionStates(media, notif)
            }
        }
    }

    refreshAll()
  }

  fun stopObserving() {
    if (registered) {
      runCatching { context.unregisterReceiver(receiver) }
      registered = false
    }
    listenerJob?.cancel()
    listenerJob = null
    calendarJob?.cancel()
    calendarJob = null
  }

  fun refreshAll() {
    refreshDateTime()
    refreshBattery()
    refreshNextCalendarEvent()
    publishExternalSessionStates(
      BlocoNotificationListenerService.mediaState.value,
      BlocoNotificationListenerService.notificationsState.value,
    )
    _systemReducedMotion.value = readSystemReducedMotion()
    _systemAccessibilityEnabled.value = readSystemAccessibilityActive()
  }

  private fun publishExternalSessionStates(
    rawMedia: MediaSessionState,
    rawNotifications: NotificationSummaryState,
  ) {
    val hasListenerAccess = BlocoNotificationListenerService.isAccessGranted(context)
    val effectiveNotif =
      if (!notificationsEnabled || !hasListenerAccess) {
        NotificationSummaryState(hasPermission = hasListenerAccess, activeCount = 0)
      } else if (!privatePreviewsEnabled) {
        rawNotifications.copy(hasPermission = true, latestTitle = null)
      } else {
        rawNotifications.copy(hasPermission = true)
      }

    val effectiveMedia =
      if (hasListenerAccess) rawMedia else MediaSessionState()

    _state.update { current ->
      if (current.notifications == effectiveNotif && current.mediaSession == effectiveMedia) {
        current
      } else {
        current.copy(notifications = effectiveNotif, mediaSession = effectiveMedia)
      }
    }
  }

  private fun refreshDateTime() {
    val now = Date()
    val ptBr = Locale("pt", "BR")
    val is24Hour = DateFormat.is24HourFormat(context)
    val timePattern = if (is24Hour) "HH:mm" else "h:mm"
    val timeText = SimpleDateFormat(timePattern, ptBr).format(now)
    val dayNumber = SimpleDateFormat("d", ptBr).format(now)
    val weekday =
      SimpleDateFormat("EEEE", ptBr)
        .format(now)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(ptBr) else it.toString() }
    val month =
      SimpleDateFormat("MMMM", ptBr)
        .format(now)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(ptBr) else it.toString() }

    val nextAlarm = readNextAlarm(timePattern, ptBr)

    _state.update { current ->
      if (
        current.timeFormatted == timeText &&
          current.nextAlarmFormatted == nextAlarm &&
          current.dayNumber == dayNumber &&
          current.weekdayLabel == weekday &&
          current.monthLabel == month
      ) {
        current
      } else {
        current.copy(
          timeFormatted = timeText,
          nextAlarmFormatted = nextAlarm,
          dayNumber = dayNumber,
          weekdayLabel = weekday,
          monthLabel = month,
        )
      }
    }
  }

  private fun refreshNextCalendarEvent() {
    val hasPerm =
      ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
        PackageManager.PERMISSION_GRANTED
    if (!calendarEnabled || !hasPerm) {
      _state.update { current ->
        current.copy(
          hasCalendarPermission = hasPerm,
          nextCalendarEventTitle = null,
          nextCalendarEventTime = null,
        )
      }
      return
    }

    calendarJob?.cancel()
    calendarJob =
      scope.launch {
        val (title, timeStr) = queryNextEventOffMainThread()
        _state.update { current ->
          current.copy(
            hasCalendarPermission = true,
            nextCalendarEventTitle = if (privatePreviewsEnabled) title else null,
            nextCalendarEventTime = timeStr,
          )
        }
      }
  }

  private suspend fun queryNextEventOffMainThread(): Pair<String?, String?> =
    withContext(Dispatchers.IO) {
      val nowMs = System.currentTimeMillis()
      val endMs = nowMs + 24L * 60L * 60L * 1000L
      val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
      ContentUris.appendId(builder, nowMs)
      ContentUris.appendId(builder, endMs)
      val projection = arrayOf(
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.ALL_DAY,
      )
      return@withContext runCatching {
        context.contentResolver.query(
          builder.build(),
          projection,
          null,
          null,
          "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { cursor ->
          while (cursor.moveToNext()) {
            val begin = cursor.getLong(1)
            val allDay = cursor.getInt(2) == 1
            if (allDay || begin >= nowMs) {
              val title = cursor.getString(0)?.trim()?.takeIf { it.isNotEmpty() }
              val ptBr = Locale("pt", "BR")
              val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
              val formatted = if (allDay) "Hoje" else SimpleDateFormat(pattern, ptBr).format(Date(begin))
              return@use title to formatted
            }
          }
          null to null
        } ?: (null to null)
      }.getOrDefault(null to null)
    }

  private fun readNextAlarm(timePattern: String, locale: Locale): String? {
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return null
    val triggerMs = runCatching { alarmManager.nextAlarmClock?.triggerTime }.getOrNull() ?: return null
    if (triggerMs <= 0L) return null
    return SimpleDateFormat(timePattern, locale).format(Date(triggerMs))
  }

  private fun refreshBattery() {
    val batteryIntent =
      runCatching {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
      }.getOrNull()
    if (batteryIntent != null) {
      updateBatteryFromIntent(batteryIntent)
    }
  }

  private fun updateBatteryFromIntent(intent: Intent) {
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val isCharging =
      status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    val percent =
      if (level >= 0 && scale > 0) {
        ((level * 100f) / scale).toInt().coerceIn(0, 100)
      } else {
        null
      }
    _state.update { current ->
      if (current.batteryPercent == percent && current.isCharging == isCharging) {
        current
      } else {
        current.copy(batteryPercent = percent, isCharging = isCharging)
      }
    }
  }

  private fun readSystemReducedMotion(): Boolean {
    val scale =
      runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
      }.getOrDefault(1f)
    return scale == 0f
  }

  private fun readSystemAccessibilityActive(): Boolean {
    val am = runCatching { context.getSystemService(AccessibilityManager::class.java) }.getOrNull()
    val touchExplore = am?.isTouchExplorationEnabled == true
    val a11yEnabled = am?.isEnabled == true
    return touchExplore || a11yEnabled
  }
}
