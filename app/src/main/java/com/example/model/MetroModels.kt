package com.example.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.pow

data class AppTargetKey(
  val packageName: String,
  val className: String,
  val userSerialNumber: Long,
) {
  val stableKey: String
    get() = "$packageName/$className#$userSerialNumber"
}

data class InstalledApp(
  val key: AppTargetKey,
  val label: String,
  val normalizedLabel: String,
  val sortSection: String,
  val isSystemApp: Boolean,
  val isWorkProfile: Boolean = false,
  val isProfilePaused: Boolean = false,
)

data class DeepShortcutItem(
  val id: String,
  val packageName: String,
  val userSerialNumber: Long,
  val label: String,
)

data class AppRepairProposal(
  val tileId: String,
  val hubId: String? = null,
  val labelHint: String,
  val candidates: List<InstalledApp>,
)

enum class TileKind {
  APP_SHORTCUT,
  CLOCK,
  BATTERY,
  PHOTO,
  CALENDAR,
  FOLDER,
  NOTE,
  MUSIC,
  NOTIFICATIONS,
  HUB,
  SYSTEM_WIDGET,
}

enum class TileSize(val colSpan: Int, val rowSpan: Int) {
  SMALL(1, 1),
  MEDIUM(2, 2),
  WIDE(4, 2);

  companion object {
    fun fromSpans(colSpan: Int, rowSpan: Int): TileSize =
      when {
        colSpan >= 4 -> WIDE
        colSpan >= 2 -> MEDIUM
        else -> SMALL
      }
  }
}

enum class RoleSlot {
  PHONE,
  CONTACTS,
  CHAT,
  FAVORITE_MESSENGER,
  MESSAGES,
  CLOCK,
  BATTERY,
  PHOTO,
  CALENDAR,
  MAIL,
  COMMUNITY,
  MAPS,
  MUSIC,
  PROFESSIONAL_NETWORK,
  STREAMING,
  VIDEO,
  SHORT_VIDEOS,
  MAIN_SOCIAL,
  SECONDARY_SOCIAL,
  BROWSER,
  STORE,
  ASSISTANT,
  WALLET,
  READER,
  FILES,
  CAMERA,
  SECURITY,
  NOTES,
}

data class MetroFolder(
  val id: String,
  val name: String,
  val appKeys: List<AppTargetKey> = emptyList(),
)

data class QuickNote(
  val id: String,
  val title: String = "",
  val body: String = "",
  val isChecklist: Boolean = false,
  val updatedAtMs: Long = 0L,
)

data class MetroHub(
  val id: String,
  val name: String,
  val accentHex: Long? = null,
  val tiles: List<StartTile> = emptyList(),
  val folders: List<MetroFolder> = emptyList(),
)

data class StartTile(
  val id: String,
  val kind: TileKind,
  val col: Int,
  val row: Int,
  val colSpan: Int,
  val rowSpan: Int,
  val target: AppTargetKey? = null,
  val fallbackLabel: String = "",
  val roleSlot: RoleSlot? = null,
  val customColorHex: Long? = null,
  val folderId: String? = null,
  val noteId: String? = null,
  val hubId: String? = null,
  val appWidgetId: Int? = null,
  val useOriginalIcon: Boolean = true,
  val isLiveEnabled: Boolean = true,
  val isUnavailable: Boolean = false,
  val needsRepairChoice: Boolean = false,
) {
  val size: TileSize
    get() = TileSize.fromSpans(colSpan, rowSpan)
}

data class StartLayoutState(
  val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
  val revision: Long = 0L,
  val initialized: Boolean = false,
  val tiles: List<StartTile> = emptyList(),
  val folders: List<MetroFolder> = emptyList(),
  val notes: List<QuickNote> = emptyList(),
  val hubs: List<MetroHub> = emptyList(),
) {
  companion object {
    const val CURRENT_SCHEMA_VERSION = 3
  }
}

enum class MetroAccent(
  val id: String,
  val displayNamePt: String,
  val colorValue: Long,
) {
  METRO_BLUE("metro_blue", "Azul Metro", 0xFF256D94L),
  TEAL("teal", "Verde-azulado", 0xFF008272L),
  FOREST("forest", "Verde", 0xFF107C10L),
  VIOLET("violet", "Violeta", 0xFF6B3FA0L),
  ORANGE("orange", "Laranja", 0xFFD83B01L),
  RED("red", "Vermelho", 0xFFB71C1CL),
  MONOCHROME("monochrome", "Monocromático", 0xFF3A3A3AL);

  val color: Color
    get() = Color(colorValue)

  companion object {
    fun fromId(id: String?): MetroAccent =
      when (id) {
        "cobalt" -> METRO_BLUE
        "emerald" -> TEAL
        "terracotta" -> ORANGE
        "crimson" -> RED
        "royal_purple" -> VIOLET
        "graphite" -> MONOCHROME
        else -> entries.firstOrNull { it.id == id } ?: METRO_BLUE
      }
  }
}

enum class AppThemeMode(val id: String, val labelPt: String) {
  LIGHT("light", "Claro"),
  DARK("dark", "Escuro"),
  SYSTEM("system", "Seguir o sistema");

  companion object {
    fun fromId(id: String?): AppThemeMode =
      entries.firstOrNull { it.id == id } ?: SYSTEM
  }
}

enum class LightweightMode(val id: String, val labelPt: String) {
  AUTO("auto", "Automático"),
  ENABLED("enabled", "Ativado"),
  DISABLED("disabled", "Desativado");

  companion object {
    fun fromId(id: String?): LightweightMode =
      entries.firstOrNull { it.id == id } ?: AUTO
  }
}

enum class SettingsSection(val titlePt: String) {
  LANDING("Configurações"),
  PROFILE("Meu perfil"),
  APPEARANCE("Aparência"),
  START_SCREEN("Tela inicial"),
  PERFORMANCE("Desempenho"),
  ACCESSIBILITY("Acessibilidade"),
  ABOUT("Sobre o projeto"),
  ADVANCED("Avançado");

  companion object {
    val detailPages: List<SettingsSection> =
      listOf(PROFILE, APPEARANCE, START_SCREEN, PERFORMANCE, ACCESSIBILITY, ABOUT, ADVANCED)
  }
}

data class LocalUserProfile(
  val name: String = "",
  val age: Int? = null,
  val avatarPath: String? = null,
  val avatarRevision: Long = 0L,
  val showOnStart: Boolean = false,
)

enum class LeftCommandSlot(val id: String, val labelPt: String) {
  SEARCH("search", "Buscar"),
  QUICK_NOTE("quick_note", "Nota rápida"),
  FOLDERS("folders", "Pastas");

  companion object {
    fun fromId(id: String?): LeftCommandSlot =
      entries.firstOrNull { it.id == id } ?: SEARCH
  }
}

enum class PhotoDisplayMode(val id: String, val labelPt: String) {
  STATIC("static", "Foto estática"),
  ROTATING("rotating", "Rotação de fotos");

  companion object {
    fun fromId(id: String?): PhotoDisplayMode =
      entries.firstOrNull { it.id == id } ?: STATIC
  }
}

enum class PhotoCropMode(val id: String, val labelPt: String) {
  CENTER("center", "Centro"),
  TOP("top", "Topo"),
  BOTTOM("bottom", "Base");

  companion object {
    fun fromId(id: String?): PhotoCropMode =
      entries.firstOrNull { it.id == id } ?: CENTER
  }
}

sealed interface LauncherSurface {
  data object Start : LauncherSurface
  data class Apps(val focusSearch: Boolean = false) : LauncherSurface
  data class Hub(val hubId: String) : LauncherSurface
  data class Settings(
    val returnTo: LauncherSurface = Start,
    val section: SettingsSection = SettingsSection.LANDING,
  ) : LauncherSurface
}

data class MediaSessionState(
  val isActive: Boolean = false,
  val isPlaying: Boolean = false,
  val playerLabel: String = "",
  val trackTitle: String = "",
  val artist: String = "",
  val packageName: String? = null,
  val albumArt: ImageBitmap? = null,
  val canPlayPause: Boolean = false,
  val canSkipPrevious: Boolean = false,
  val canSkipNext: Boolean = false,
)

data class NotificationSummaryState(
  val hasPermission: Boolean = false,
  val activeCount: Int = 0,
  val latestTitle: String? = null,
  val latestAppName: String? = null,
)

data class LiveTilesState(
  val timeFormatted: String = "",
  val nextAlarmFormatted: String? = null,
  val dayNumber: String = "",
  val weekdayLabel: String = "",
  val monthLabel: String = "",
  val batteryPercent: Int? = null,
  val isCharging: Boolean = false,
  val hasCalendarPermission: Boolean = false,
  val nextCalendarEventTitle: String? = null,
  val nextCalendarEventTime: String? = null,
  val showHomePhotos: Boolean = false,
  val activePhotoPath: String? = null,
  val selectedPhotoCount: Int = 0,
  val photoRevision: Long = 0L,
  val isPhotoMissing: Boolean = false,
  val mediaSession: MediaSessionState = MediaSessionState(),
  val notifications: NotificationSummaryState = NotificationSummaryState(),
)

object ProjectAboutConfig {
  const val CREATOR_NAME = "Felipe Oliveira"
  const val FREE_USE_STATEMENT_PT =
    "Eu sou Felipe Oliveira, criador do Bloco. O launcher e todos os seus recursos são totalmente gratuitos. Você pode usar os blocos, as pastas, os temas e as opções de acessibilidade sem pagar."

  // Configurável quando houver repositório público e licença verificados do projeto:
  val repositoryUrl: String? = null
  val licenseName: String? = null
  val licenseText: String? = null
}

object MetroColorUtils {
  fun relativeLuminance(color: Color): Float {
    fun linearize(c: Float): Float =
      if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    val r = linearize(color.red)
    val g = linearize(color.green)
    val b = linearize(color.blue)
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
  }

  fun isLightColor(color: Color): Boolean = relativeLuminance(color) > 0.45f

  fun legibleForeground(background: Color): Color {
    return if (relativeLuminance(background) > 0.36f) Color(0xFF111111) else Color.White
  }

  fun mutedForeground(background: Color): Color {
    return legibleForeground(background).copy(alpha = 0.76f)
  }

  fun parseHexColor(raw: String): Long? {
    val cleaned = raw.trim().removePrefix("#")
    if (cleaned.length != 6 && cleaned.length != 8) return null
    val parsed = cleaned.toLongOrNull(16) ?: return null
    return if (cleaned.length == 6) {
      0xFF000000L or parsed
    } else {
      parsed
    }
  }

  fun formatHexColor(argb: Long): String {
    val rgb = (argb and 0xFFFFFFL).toInt()
    return String.format("#%06X", rgb)
  }
}
