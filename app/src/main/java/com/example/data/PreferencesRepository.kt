package com.example.data

import android.app.ActivityManager
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppThemeMode
import com.example.model.LeftCommandSlot
import com.example.model.LightweightMode
import com.example.model.LocalUserProfile
import com.example.model.MetroAccent
import com.example.model.MetroColorUtils
import com.example.model.PhotoCropMode
import com.example.model.PhotoDisplayMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

internal val Context.metroPreferencesDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "metro_home_prefs")

data class LauncherPreferences(
  val accent: MetroAccent = MetroAccent.METRO_BLUE,
  val customAccentHex: Long? = null,
  val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  val backgroundHex: Long = 0xFF000000L,
  val gutterDp: Int = 4,
  val showLabels: Boolean = true,
  val lockLayout: Boolean = false,
  val reduceMotion: Boolean = false,
  val lightweightMode: LightweightMode = LightweightMode.AUTO,
  val isLowRamDevice: Boolean = false,
  val leftCommandSlot: LeftCommandSlot = LeftCommandSlot.SEARCH,
  // Icons
  val themeThirdPartyIcons: Boolean = false,
  // Photos (off by default on fresh install; preserved on upgrade if legacy photo existed)
  val showHomePhotos: Boolean = false,
  val selectedPhotoPaths: List<String> = emptyList(),
  val photoDisplayMode: PhotoDisplayMode = PhotoDisplayMode.STATIC,
  val photoCropMode: PhotoCropMode = PhotoCropMode.CENTER,
  val showPhotoCaptions: Boolean = true,
  val photoRevision: Long = 0L,
  // Optional Live Tile permissions / privacy
  val calendarAccessEnabled: Boolean = false,
  val notificationsAccessEnabled: Boolean = false,
  val showPrivatePreviews: Boolean = false,
  // Local profile (strictly offline & never mixed with Start photos)
  val profile: LocalUserProfile = LocalUserProfile(),
  // Hearing / feedback preference
  val vibrationEnabled: Boolean = false,
) {
  val lightMode: Boolean
    get() =
      when (lightweightMode) {
        LightweightMode.ENABLED -> true
        LightweightMode.DISABLED -> false
        LightweightMode.AUTO -> isLowRamDevice
      }

  val effectiveAccentColor: Color
    get() = Color(customAccentHex ?: accent.colorValue)

  val effectiveBackgroundColor: Color
    get() = Color(backgroundHex)

  val primaryPhotoPath: String?
    get() = selectedPhotoPaths.firstOrNull()
}

class PreferencesRepository(private val context: Context) {

  private val systemLowRam: Boolean = detectLowRamDevice(context)

  private object Keys {
    val ACCENT_ID = stringPreferencesKey("accent_id")
    val CUSTOM_ACCENT_HEX = longPreferencesKey("custom_accent_hex")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val BACKGROUND_HEX = longPreferencesKey("background_hex")
    val GUTTER_DP = intPreferencesKey("gutter_dp")
    val SHOW_LABELS = booleanPreferencesKey("show_labels")
    val LOCK_LAYOUT = booleanPreferencesKey("lock_layout")
    val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
    val LIGHT_MODE = booleanPreferencesKey("light_mode")
    val LIGHTWEIGHT_MODE = stringPreferencesKey("lightweight_mode")
    val LEFT_COMMAND_SLOT = stringPreferencesKey("left_command_slot")
    val THEME_THIRD_PARTY_ICONS = booleanPreferencesKey("theme_third_party_icons")

    // Photo keys (including legacy PHOTO_FILE_PATH for seamless upgrade)
    val PHOTO_FILE_PATH = stringPreferencesKey("photo_file_path")
    val SELECTED_PHOTO_PATHS_JSON = stringPreferencesKey("selected_photo_paths_json")
    val SHOW_HOME_PHOTOS = booleanPreferencesKey("show_home_photos")
    val PHOTO_DISPLAY_MODE = stringPreferencesKey("photo_display_mode")
    val PHOTO_CROP_MODE = stringPreferencesKey("photo_crop_mode")
    val SHOW_PHOTO_CAPTIONS = booleanPreferencesKey("show_photo_captions")
    val PHOTO_REVISION = longPreferencesKey("photo_revision")

    // Live tile optional accesses & privacy
    val CALENDAR_ACCESS_ENABLED = booleanPreferencesKey("calendar_access_enabled")
    val NOTIFICATIONS_ACCESS_ENABLED = booleanPreferencesKey("notifications_access_enabled")
    val SHOW_PRIVATE_PREVIEWS = booleanPreferencesKey("show_private_previews")

    // Local profile
    val PROFILE_NAME = stringPreferencesKey("profile_name")
    val PROFILE_AGE = intPreferencesKey("profile_age")
    val PROFILE_AVATAR_PATH = stringPreferencesKey("profile_avatar_path")
    val PROFILE_AVATAR_REVISION = longPreferencesKey("profile_avatar_revision")
    val PROFILE_SHOW_ON_START = booleanPreferencesKey("profile_show_on_start")

    // Alerts / tactile confirmation
    val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
  }

  val preferencesFlow: Flow<LauncherPreferences> =
    context.metroPreferencesDataStore.data.map { prefs ->
      val legacyPhoto = prefs[Keys.PHOTO_FILE_PATH]?.takeIf { it.isNotBlank() }
      val parsedPaths = parseStringListJson(prefs[Keys.SELECTED_PHOTO_PATHS_JSON])
      val selectedPaths =
        when {
          parsedPaths.isNotEmpty() -> parsedPaths
          legacyPhoto != null -> listOf(legacyPhoto)
          else -> emptyList()
        }

      // Fresh install -> false; upgrade with legacy explicit photo -> true unless user toggled off
      val showPhotos = prefs[Keys.SHOW_HOME_PHOTOS] ?: (legacyPhoto != null)

      val legacyBg = prefs[Keys.BACKGROUND_HEX] ?: 0xFF000000L
      val resolvedThemeMode =
        prefs[Keys.THEME_MODE]?.let { AppThemeMode.fromId(it) }
          ?: if (prefs[Keys.BACKGROUND_HEX] != null && MetroColorUtils.isLightColor(Color(legacyBg))) {
            AppThemeMode.LIGHT
          } else {
            AppThemeMode.SYSTEM
          }

      val resolvedLightweight =
        prefs[Keys.LIGHTWEIGHT_MODE]?.let { LightweightMode.fromId(it) }
          ?: prefs[Keys.LIGHT_MODE]?.let { legacyLight ->
            if (legacyLight) LightweightMode.ENABLED else LightweightMode.DISABLED
          }
          ?: LightweightMode.AUTO

      val profileAge = prefs[Keys.PROFILE_AGE]?.takeIf { it in 1..120 }
      val profile =
        LocalUserProfile(
          name = prefs[Keys.PROFILE_NAME].orEmpty(),
          age = profileAge,
          avatarPath = prefs[Keys.PROFILE_AVATAR_PATH]?.takeIf { it.isNotBlank() },
          avatarRevision = prefs[Keys.PROFILE_AVATAR_REVISION] ?: 0L,
          showOnStart = prefs[Keys.PROFILE_SHOW_ON_START] ?: false,
        )

      LauncherPreferences(
        accent = MetroAccent.fromId(prefs[Keys.ACCENT_ID]),
        customAccentHex = prefs[Keys.CUSTOM_ACCENT_HEX],
        themeMode = resolvedThemeMode,
        backgroundHex = legacyBg,
        gutterDp = (prefs[Keys.GUTTER_DP] ?: 4).coerceIn(2, 8),
        showLabels = prefs[Keys.SHOW_LABELS] ?: true,
        lockLayout = prefs[Keys.LOCK_LAYOUT] ?: false,
        reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false,
        lightweightMode = resolvedLightweight,
        isLowRamDevice = systemLowRam,
        leftCommandSlot = LeftCommandSlot.fromId(prefs[Keys.LEFT_COMMAND_SLOT]),
        themeThirdPartyIcons = prefs[Keys.THEME_THIRD_PARTY_ICONS] ?: false,
        showHomePhotos = showPhotos,
        selectedPhotoPaths = selectedPaths,
        photoDisplayMode = PhotoDisplayMode.fromId(prefs[Keys.PHOTO_DISPLAY_MODE]),
        photoCropMode = PhotoCropMode.fromId(prefs[Keys.PHOTO_CROP_MODE]),
        showPhotoCaptions = prefs[Keys.SHOW_PHOTO_CAPTIONS] ?: true,
        photoRevision = prefs[Keys.PHOTO_REVISION] ?: 0L,
        calendarAccessEnabled = prefs[Keys.CALENDAR_ACCESS_ENABLED] ?: false,
        notificationsAccessEnabled = prefs[Keys.NOTIFICATIONS_ACCESS_ENABLED] ?: false,
        showPrivatePreviews = prefs[Keys.SHOW_PRIVATE_PREVIEWS] ?: false,
        profile = profile,
        vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: false,
      )
    }

  suspend fun setAccent(accent: MetroAccent) {
    context.metroPreferencesDataStore.edit { prefs ->
      prefs[Keys.ACCENT_ID] = accent.id
      prefs.remove(Keys.CUSTOM_ACCENT_HEX)
    }
  }

  suspend fun setCustomAccentHex(argb: Long) {
    context.metroPreferencesDataStore.edit { prefs ->
      prefs[Keys.CUSTOM_ACCENT_HEX] = argb
    }
  }

  suspend fun setThemeMode(mode: AppThemeMode) {
    context.metroPreferencesDataStore.edit { prefs ->
      prefs[Keys.THEME_MODE] = mode.id
      when (mode) {
        AppThemeMode.LIGHT -> prefs[Keys.BACKGROUND_HEX] = 0xFFF6F6F6L
        AppThemeMode.DARK -> prefs[Keys.BACKGROUND_HEX] = 0xFF000000L
        AppThemeMode.SYSTEM -> prefs.remove(Keys.BACKGROUND_HEX)
      }
    }
  }

  suspend fun setBackgroundHex(argb: Long) {
    context.metroPreferencesDataStore.edit { prefs ->
      prefs[Keys.BACKGROUND_HEX] = argb
    }
  }

  suspend fun setGutterDp(dp: Int) {
    context.metroPreferencesDataStore.edit { prefs ->
      prefs[Keys.GUTTER_DP] = dp.coerceIn(2, 8)
    }
  }

  suspend fun setShowLabels(show: Boolean) {
    context.metroPreferencesDataStore.edit { prefs -> prefs[Keys.SHOW_LABELS] = show }
  }

  suspend fun setLockLayout(locked: Boolean) {
    context.metroPreferencesDataStore.edit { prefs -> prefs[Keys.LOCK_LAYOUT] = locked }
  }

  suspend fun setReduceMotion(enabled: Boolean) {
    context.metroPreferencesDataStore.edit { prefs -> prefs[Keys.REDUCE_MOTION] = enabled }
  }

  suspend fun setLightweightMode(mode: LightweightMode) {
    context.metroPreferencesDataStore.edit { prefs ->
      prefs[Keys.LIGHTWEIGHT_MODE] = mode.id
      when (mode) {
        LightweightMode.ENABLED -> prefs[Keys.LIGHT_MODE] = true
        LightweightMode.DISABLED -> prefs[Keys.LIGHT_MODE] = false
        LightweightMode.AUTO -> prefs.remove(Keys.LIGHT_MODE)
      }
    }
  }

  suspend fun setLightMode(enabled: Boolean) {
    setLightweightMode(if (enabled) LightweightMode.ENABLED else LightweightMode.DISABLED)
  }

  suspend fun setLeftCommandSlot(slot: LeftCommandSlot) {
    context.metroPreferencesDataStore.edit { prefs -> prefs[Keys.LEFT_COMMAND_SLOT] = slot.id }
  }

  suspend fun setThemeThirdPartyIcons(enabled: Boolean) {
    context.metroPreferencesDataStore.edit { prefs -> prefs[Keys.THEME_THIRD_PARTY_ICONS] = enabled }
  }

  suspend fun setShowHomePhotos(show: Boolean) {
    context.metroPreferencesDataStore.edit { prefs -> prefs[Keys.SHOW_HOME_PHOTOS] = show }
  }

  suspend fun setSelectedPhotos(paths: List<String>, revision: Long, enableDisplay: Boolean = true) {
    context.metroPreferencesDataStore.edit { prefs ->
      prefs[Keys.SELECTED_PHOTO_PATHS_JSON] = encodeStringListJson(paths)
      if (paths.isNotEmpty()) {
        prefs[Keys.PHOTO_FILE_PATH] = paths.first()
      } else {
        prefs.remove(Keys.PHOTO_FILE_PATH)
      }
      prefs[Keys.PHOTO_REVISION] = revision
      if (enableDisplay && paths.isNotEmpty()) {
        prefs[Keys.SHOW_HOME_PHOTOS] = true
      }
    }
  }

  suspend fun forgetSelectedPhotos() {
    context.metroPreferencesDataStore.edit { prefs ->
      prefs.remove(Keys.SELECTED_PHOTO_PATHS_JSON)
      prefs.remove(Keys.PHOTO_FILE_PATH)
      prefs[Keys.PHOTO_REVISION] = System.currentTimeMillis()
    }
  }

  suspend fun setPhotoDisplayMode(mode: PhotoDisplayMode) {
    context.metroPreferencesDataStore.edit { prefs -> prefs[Keys.PHOTO_DISPLAY_MODE] = mode.id }
  }

  suspend fun setPhotoCropMode(crop: PhotoCropMode) {
    context.metroPreferencesDataStore.edit { prefs -> prefs[Keys.PHOTO_CROP_MODE] = crop.id }
  }

  suspend fun setShowPhotoCaptions(show: Boolean) {
    context.metroPreferencesDataStore.edit { prefs -> prefs[Keys.SHOW_PHOTO_CAPTIONS] = show }
  }

  suspend fun setCalendarAccessEnabled(enabled: Boolean) {
    context.metroPreferencesDataStore.edit { prefs -> prefs[Keys.CALENDAR_ACCESS_ENABLED] = enabled }
  }

  suspend fun setNotificationsAccessEnabled(enabled: Boolean) {
    context.metroPreferencesDataStore.edit { prefs -> prefs[Keys.NOTIFICATIONS_ACCESS_ENABLED] = enabled }
  }

  suspend fun setShowPrivatePreviews(show: Boolean) {
    context.metroPreferencesDataStore.edit { prefs -> prefs[Keys.SHOW_PRIVATE_PREVIEWS] = show }
  }

  suspend fun setProfileName(name: String) {
    context.metroPreferencesDataStore.edit { prefs ->
      val trimmed = name.trim()
      if (trimmed.isEmpty()) {
        prefs.remove(Keys.PROFILE_NAME)
      } else {
        prefs[Keys.PROFILE_NAME] = trimmed
      }
    }
  }

  suspend fun setProfileAge(age: Int?) {
    context.metroPreferencesDataStore.edit { prefs ->
      if (age == null || age !in 1..120) {
        prefs.remove(Keys.PROFILE_AGE)
      } else {
        prefs[Keys.PROFILE_AGE] = age
      }
    }
  }

  suspend fun setProfileAvatar(path: String?, revision: Long) {
    context.metroPreferencesDataStore.edit { prefs ->
      if (path.isNullOrBlank()) {
        prefs.remove(Keys.PROFILE_AVATAR_PATH)
      } else {
        prefs[Keys.PROFILE_AVATAR_PATH] = path
      }
      prefs[Keys.PROFILE_AVATAR_REVISION] = revision
    }
  }

  suspend fun setShowProfileOnStart(show: Boolean) {
    context.metroPreferencesDataStore.edit { prefs ->
      prefs[Keys.PROFILE_SHOW_ON_START] = show
    }
  }

  suspend fun setVibrationEnabled(enabled: Boolean) {
    context.metroPreferencesDataStore.edit { prefs ->
      prefs[Keys.VIBRATION_ENABLED] = enabled
    }
  }

  private fun parseStringListJson(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val item = arr.optString(i, "")
          if (item.isNotBlank()) add(item)
        }
      }
    }.getOrDefault(emptyList())
  }

  private fun encodeStringListJson(list: List<String>): String {
    val arr = JSONArray()
    list.forEach { arr.put(it) }
    return arr.toString()
  }

  companion object {
    private const val LOW_RAM_THRESHOLD_BYTES = 768L * 1024L * 1024L

    fun detectLowRamDevice(context: Context): Boolean {
      return runCatching {
        val am = context.getSystemService(ActivityManager::class.java) ?: return false
        if (am.isLowRamDevice) return true
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        memInfo.totalMem in 1L..LOW_RAM_THRESHOLD_BYTES
      }.getOrDefault(false)
    }
  }
}
