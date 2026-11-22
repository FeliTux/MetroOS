package com.example.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import android.provider.AlarmClock
import android.provider.Settings
import com.example.model.AppRepairProposal
import com.example.model.AppTargetKey
import com.example.model.DeepShortcutItem
import com.example.model.InstalledApp
import com.example.model.RoleSlot
import com.example.model.StartTile
import com.example.model.TileKind
import java.text.Collator
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TileReconciliationResult(
  val tiles: List<StartTile>,
  val repairProposals: Map<String, AppRepairProposal>,
  val repairedAutomatically: Boolean,
)

class AppCatalogManager(
  private val context: Context,
  val iconLoader: AppIconLoader = AndroidAppIconLoader(context),
) {

  private val launcherApps = context.getSystemService(LauncherApps::class.java)
  private val userManager = context.getSystemService(UserManager::class.java)
  private val packageManager = context.packageManager

  private val ptBrCollator =
    Collator.getInstance(Locale("pt", "BR")).apply { strength = Collator.PRIMARY }

  private var registeredCallback: LauncherApps.Callback? = null

  suspend fun queryInstalledApps(): List<InstalledApp> =
    withContext(Dispatchers.IO) {
      val selfPackage = context.packageName
      val myHandle = android.os.Process.myUserHandle()
      val profiles = runCatching { userManager?.userProfiles }.getOrNull() ?: listOf(myHandle)
      val entries = mutableListOf<InstalledApp>()
      val seenKeys = HashSet<String>()

      for (profile in profiles) {
        val serial = runCatching { userManager?.getSerialNumberForUser(profile) }.getOrNull() ?: 0L
        val isWork = profile != myHandle
        val isQuiet =
          if (isWork && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching { userManager?.isQuietModeEnabled(profile) == true }.getOrDefault(false)
          } else {
            false
          }
        val activities =
          runCatching { launcherApps?.getActivityList(null, profile) }.getOrNull().orEmpty()
        for (info in activities) {
          val component = info.componentName
          if (component.packageName == selfPackage) continue
          val key = AppTargetKey(component.packageName, component.className, serial)
          if (!seenKeys.add(key.stableKey)) continue
          val label = info.label?.toString()?.trim().takeUnless { it.isNullOrEmpty() } ?: component.packageName
          val normalized = normalizeForSearch(label)
          val section = computeSortSection(normalized)
          val isSystem = (info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
          entries.add(
            InstalledApp(
              key = key,
              label = label,
              normalizedLabel = normalized,
              sortSection = section,
              isSystemApp = isSystem,
              isWorkProfile = isWork,
              isProfilePaused = isQuiet,
            )
          )
        }
      }

      entries.sortedWith { a, b ->
        val cmp = ptBrCollator.compare(a.label, b.label)
        if (cmp != 0) cmp else a.key.stableKey.compareTo(b.key.stableKey)
      }
    }

  suspend fun queryDeepShortcuts(key: AppTargetKey): List<DeepShortcutItem> =
    withContext(Dispatchers.IO) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return@withContext emptyList()
      val la = launcherApps ?: return@withContext emptyList()
      if (!runCatching { la.hasShortcutHostPermission() }.getOrDefault(false)) {
        return@withContext emptyList()
      }
      val profile = resolveUserHandleExact(key.userSerialNumber) ?: return@withContext emptyList()
      val query =
        LauncherApps.ShortcutQuery().apply {
          setPackage(key.packageName)
          setQueryFlags(
            LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
              LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
              LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
          )
        }
      val list = runCatching { la.getShortcuts(query, profile) }.getOrNull().orEmpty()
      list.take(4).mapNotNull { info ->
        val label = (info.shortLabel ?: info.longLabel)?.toString()?.trim()
        if (label.isNullOrEmpty()) null
        else DeepShortcutItem(
          id = info.id,
          packageName = key.packageName,
          userSerialNumber = key.userSerialNumber,
          label = label,
        )
      }
    }

  fun launchDeepShortcut(item: DeepShortcutItem): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return false
    val la = launcherApps ?: return false
    val profile = resolveUserHandleExact(item.userSerialNumber) ?: return false
    return runCatching {
      la.startShortcut(item.packageName, item.id, null, null, profile)
      true
    }.getOrDefault(false)
  }

  fun openAppInfo(key: AppTargetKey): Boolean {
    val profile = resolveUserHandleExact(key.userSerialNumber)
    val component = ComponentName(key.packageName, key.className)
    if (profile != null && launcherApps != null) {
      val started =
        runCatching {
          launcherApps.startAppDetailsActivity(component, profile, null, null)
          true
        }.getOrDefault(false)
      if (started) return true
    }
    return runCatching {
      val intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
          data = Uri.fromParts("package", key.packageName, null)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      context.startActivity(intent)
      true
    }.getOrDefault(false)
  }

  fun observeCatalogChanges(onChanged: () -> Unit) {
    unregisterCatalogObserver()
    val callback =
      object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) {
          iconLoader.invalidatePackage(packageName)
          onChanged()
        }

        override fun onPackageAdded(packageName: String, user: UserHandle) {
          iconLoader.invalidatePackage(packageName)
          onChanged()
        }

        override fun onPackageChanged(packageName: String, user: UserHandle) {
          iconLoader.invalidatePackage(packageName)
          onChanged()
        }

        override fun onPackagesAvailable(
          packageNames: Array<out String>,
          user: UserHandle,
          replacing: Boolean,
        ) {
          packageNames.forEach(iconLoader::invalidatePackage)
          onChanged()
        }

        override fun onPackagesUnavailable(
          packageNames: Array<out String>,
          user: UserHandle,
          replacing: Boolean,
        ) {
          packageNames.forEach(iconLoader::invalidatePackage)
          onChanged()
        }
      }
    registeredCallback = callback
    runCatching { launcherApps?.registerCallback(callback, Handler(Looper.getMainLooper())) }
  }

  fun unregisterCatalogObserver() {
    registeredCallback?.let { cb ->
      runCatching { launcherApps?.unregisterCallback(cb) }
    }
    registeredCallback = null
  }

  fun isTargetAvailable(key: AppTargetKey): Boolean {
    val profile = resolveUserHandleExact(key.userSerialNumber) ?: return false
    val component = ComponentName(key.packageName, key.className)
    return runCatching {
      launcherApps?.isActivityEnabled(component, profile) == true
    }.getOrDefault(false)
  }

  fun launchApp(key: AppTargetKey, sourceBounds: Rect? = null): Boolean {
    val profile = resolveUserHandleExact(key.userSerialNumber) ?: return false
    val component = ComponentName(key.packageName, key.className)
    if (!isTargetAvailable(key)) {
      return false
    }
    return try {
      launcherApps?.startMainActivity(component, profile, sourceBounds, null)
      true
    } catch (_: SecurityException) {
      false
    } catch (_: android.content.ActivityNotFoundException) {
      false
    } catch (_: Exception) {
      false
    }
  }

  fun launchUtilityTile(tile: StartTile, installedApps: List<InstalledApp>): Boolean {
    tile.target?.let { target ->
      if (launchApp(target)) return true
    }
    return when (tile.kind) {
      TileKind.CLOCK -> {
        launchFirstResolvableIntent(
          Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
          Intent(AlarmClock.ACTION_SET_ALARM).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
          findAppIntentByKeywords(installedApps, listOf("relogio", "clock", "deskclock")),
        )
      }
      TileKind.BATTERY -> {
        launchFirstResolvableIntent(
          Intent(Intent.ACTION_POWER_USAGE_SUMMARY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
          Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
          Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
      }
      TileKind.CALENDAR -> {
        launchFirstResolvableIntent(
          Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
          findAppIntentByKeywords(installedApps, listOf("agenda", "calendar", "calendario")),
        )
      }
      TileKind.MUSIC -> {
        launchFirstResolvableIntent(
          Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
          findAppIntentByKeywords(installedApps, listOf("spotify", "music", "musica", "metrolist", "ytmusic")),
        )
      }
      TileKind.PHOTO,
      TileKind.APP_SHORTCUT,
      TileKind.FOLDER,
      TileKind.NOTE,
      TileKind.NOTIFICATIONS,
      TileKind.HUB,
      TileKind.SYSTEM_WIDGET -> false
    }
  }

  /**
   * Reconciles saved tiles with the live installed catalog, preserving component + user profile
   * identity, repairing unambiguous component renames, and flagging ambiguous legacy matches with
   * "Escolher aplicativo" proposals so the user can repair them in place.
   */
  fun reconcileTiles(
    tiles: List<StartTile>,
    installedApps: List<InstalledApp>,
    hubId: String? = null,
  ): TileReconciliationResult {
    val byStableKey = installedApps.associateBy { it.key.stableKey }
    var autoRepaired = false
    val proposals = mutableMapOf<String, AppRepairProposal>()

    val updated =
      tiles.map { tile ->
        if (tile.kind != TileKind.APP_SHORTCUT) return@map tile
        val target = tile.target

        if (target != null) {
          val exact = byStableKey[target.stableKey]
          if (exact != null) {
            return@map tile.copy(
              fallbackLabel = exact.label,
              isUnavailable = exact.isProfilePaused,
              needsRepairChoice = false,
            )
          }
          if (isTargetAvailable(target)) {
            return@map tile.copy(isUnavailable = false, needsRepairChoice = false)
          }

          // Activity class renamed or moved within same package/profile: check unambiguous candidate
          val pkgCandidates =
            installedApps.filter {
              it.key.packageName == target.packageName &&
                it.key.userSerialNumber == target.userSerialNumber
            }
          when (pkgCandidates.size) {
            1 -> {
              autoRepaired = true
              val match = pkgCandidates.first()
              return@map tile.copy(
                target = match.key,
                fallbackLabel = match.label,
                isUnavailable = match.isProfilePaused,
                needsRepairChoice = false,
              )
            }
            in 2..Int.MAX_VALUE -> {
              proposals[tile.id] =
                AppRepairProposal(
                  tileId = tile.id,
                  hubId = hubId,
                  labelHint = tile.fallbackLabel,
                  candidates = pkgCandidates,
                )
              return@map tile.copy(isUnavailable = true, needsRepairChoice = true)
            }
          }
          return@map tile.copy(isUnavailable = true, needsRepairChoice = false)
        } else {
          // Legacy unlinked shortcut: attempt unambiguous match by normalized fallbackLabel
          val normLabel = normalizeForSearch(tile.fallbackLabel)
          if (normLabel.isBlank() || installedApps.isEmpty()) {
            return@map tile.copy(isUnavailable = true, needsRepairChoice = false)
          }
          val labelMatches = installedApps.filter { it.normalizedLabel == normLabel }
          when (labelMatches.size) {
            1 -> {
              autoRepaired = true
              val match = labelMatches.first()
              return@map tile.copy(
                target = match.key,
                fallbackLabel = match.label,
                isUnavailable = match.isProfilePaused,
                needsRepairChoice = false,
              )
            }
            in 2..Int.MAX_VALUE -> {
              proposals[tile.id] =
                AppRepairProposal(
                  tileId = tile.id,
                  hubId = hubId,
                  labelHint = tile.fallbackLabel,
                  candidates = labelMatches,
                )
              return@map tile.copy(isUnavailable = true, needsRepairChoice = true)
            }
            else -> return@map tile.copy(isUnavailable = true, needsRepairChoice = false)
          }
        }
      }

    return TileReconciliationResult(
      tiles = updated,
      repairProposals = proposals,
      repairedAutomatically = autoRepaired,
    )
  }

  /**
   * Seeds the initial reference mosaic using ONLY verified real installed targets.
   * Never cross-assigns random unrelated apps onto specialized slots like Phone/Messages/Mail.
   */
  fun seedReferenceLayout(installedApps: List<InstalledApp>): List<StartTile> {
    val usedKeys = HashSet<String>()

    fun takeApp(vararg hints: String, categoryIntent: Intent? = null): InstalledApp? {
      if (categoryIntent != null) {
        val resolved = resolveCategoryTarget(categoryIntent, installedApps)
        if (resolved != null && usedKeys.add(resolved.key.stableKey)) {
          return resolved
        }
      }
      for (hint in hints) {
        val match =
          installedApps.firstOrNull { app ->
            app.key.stableKey !in usedKeys &&
              (app.key.packageName.contains(hint, ignoreCase = true) ||
                app.normalizedLabel.contains(hint, ignoreCase = true))
          }
        if (match != null) {
          usedKeys.add(match.key.stableKey)
          return match
        }
      }
      return null
    }

    val seeded = mutableListOf<StartTile>()

    fun addMatchedSlot(
      id: String,
      role: RoleSlot,
      col: Int,
      row: Int,
      colSpan: Int,
      rowSpan: Int,
      matchedApp: InstalledApp?,
    ) {
      val resolved = matchedApp ?: return
      seeded.add(
        StartTile(
          id = id,
          kind = TileKind.APP_SHORTCUT,
          col = col,
          row = row,
          colSpan = colSpan,
          rowSpan = rowSpan,
          target = resolved.key,
          fallbackLabel = resolved.label,
          roleSlot = role,
          useOriginalIcon = true,
        )
      )
    }

    // Row 0..1: Clock, Calendar / Phone, Chat, Messages
    addMatchedSlot(
      id = "slot_phone",
      role = RoleSlot.PHONE,
      col = 0,
      row = 0,
      colSpan = 1,
      rowSpan = 1,
      matchedApp = takeApp("dialer", "telefone", "phone", categoryIntent = Intent(Intent.ACTION_DIAL)),
    )
    addMatchedSlot(
      id = "slot_contacts",
      role = RoleSlot.CONTACTS,
      col = 1,
      row = 0,
      colSpan = 1,
      rowSpan = 1,
      matchedApp =
        takeApp(
          "contacts",
          "contatos",
          categoryIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CONTACTS),
        ),
    )
    addMatchedSlot("slot_chat", RoleSlot.CHAT, 0, 1, 1, 1, takeApp("whatsapp", "telegram", "signal"))
    addMatchedSlot("slot_fav_messenger", RoleSlot.FAVORITE_MESSENGER, 1, 1, 1, 1, takeApp("telegram", "orca", "discord"))
    addMatchedSlot(
      id = "slot_messages",
      role = RoleSlot.MESSAGES,
      col = 2,
      row = 0,
      colSpan = 2,
      rowSpan = 2,
      matchedApp =
        takeApp(
          "messaging",
          "mensagens",
          "mms",
          categoryIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING),
        ),
    )
    seeded.add(
      StartTile(
        id = "slot_clock",
        kind = TileKind.CLOCK,
        col = 4,
        row = 0,
        colSpan = 2,
        rowSpan = 2,
        target = takeApp("deskclock", "clock", "relogio")?.key,
        fallbackLabel = "Relógio",
        roleSlot = RoleSlot.CLOCK,
      )
    )

    // Row 2..3: Battery + Wide Photo (photo respects Mostrar fotos no início setting)
    seeded.add(
      StartTile(
        id = "slot_battery",
        kind = TileKind.BATTERY,
        col = 0,
        row = 2,
        colSpan = 2,
        rowSpan = 2,
        target = null,
        fallbackLabel = "Bateria",
        roleSlot = RoleSlot.BATTERY,
      )
    )
    seeded.add(
      StartTile(
        id = "slot_photo",
        kind = TileKind.PHOTO,
        col = 2,
        row = 2,
        colSpan = 4,
        rowSpan = 2,
        target = null,
        fallbackLabel = "Fotos",
        roleSlot = RoleSlot.PHOTO,
      )
    )

    // Row 4..5: Wide Calendar + Media/Music live tile
    seeded.add(
      StartTile(
        id = "slot_calendar",
        kind = TileKind.CALENDAR,
        col = 0,
        row = 4,
        colSpan = 4,
        rowSpan = 2,
        target =
          takeApp(
            "calendar",
            "agenda",
            "calendario",
            categoryIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR),
          )?.key,
        fallbackLabel = "Calendário",
        roleSlot = RoleSlot.CALENDAR,
      )
    )
    seeded.add(
      StartTile(
        id = "slot_music_live",
        kind = TileKind.MUSIC,
        col = 4,
        row = 4,
        colSpan = 2,
        rowSpan = 2,
        target =
          takeApp(
            "spotify",
            "ytmusic",
            "music",
            "musica",
            "metrolist",
            categoryIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC),
          )?.key,
        fallbackLabel = "Música",
        roleSlot = RoleSlot.MUSIC,
      )
    )

    // Fill remaining well-known slots if present on device without guessing unrelated apps
    addMatchedSlot(
      "slot_camera",
      RoleSlot.CAMERA,
      0,
      6,
      2,
      2,
      takeApp("camera", "cam", categoryIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)),
    )
    addMatchedSlot(
      "slot_browser",
      RoleSlot.BROWSER,
      2,
      6,
      2,
      2,
      takeApp("chrome", "firefox", "browser", categoryIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER)),
    )
    addMatchedSlot(
      "slot_files",
      RoleSlot.FILES,
      4,
      6,
      2,
      2,
      takeApp("documentsui", "files", "arquivos", "nbu.files"),
    )

    // Fill additional open 2x2 slots from remaining genuine installed catalog (up to 6 more)
    var nextRow = 8
    var nextCol = 0
    val remaining = installedApps.filter { it.key.stableKey !in usedKeys }.take(6)
    for ((idx, app) in remaining.withIndex()) {
      seeded.add(
        StartTile(
          id = "slot_catalog_$idx",
          kind = TileKind.APP_SHORTCUT,
          col = nextCol,
          row = nextRow,
          colSpan = 2,
          rowSpan = 2,
          target = app.key,
          fallbackLabel = app.label,
          useOriginalIcon = true,
        )
      )
      nextCol += 2
      if (nextCol >= 6) {
        nextCol = 0
        nextRow += 2
      }
    }

    return seeded.sortedWith(compareBy<StartTile> { it.row }.thenBy { it.col })
  }

  private fun resolveCategoryTarget(
    intent: Intent,
    installedApps: List<InstalledApp>,
  ): InstalledApp? {
    val info =
      runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          packageManager.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
          @Suppress("DEPRECATION")
          packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
      }.getOrNull() ?: return null
    val pkg = info.activityInfo?.packageName ?: return null
    val cls = info.activityInfo?.name ?: return null
    return installedApps.firstOrNull { it.key.packageName == pkg && it.key.className == cls }
      ?: installedApps.firstOrNull { it.key.packageName == pkg }
  }

  private fun findAppIntentByKeywords(
    installedApps: List<InstalledApp>,
    keywords: List<String>,
  ): Intent? {
    val match =
      installedApps.firstOrNull { app ->
        keywords.any { kw ->
          app.key.packageName.contains(kw, ignoreCase = true) ||
            app.normalizedLabel.contains(kw, ignoreCase = true)
        }
      } ?: return null
    return Intent(Intent.ACTION_MAIN).apply {
      addCategory(Intent.CATEGORY_LAUNCHER)
      component = ComponentName(match.key.packageName, match.key.className)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  }

  private fun launchFirstResolvableIntent(vararg intents: Intent?): Boolean {
    for (intent in intents) {
      if (intent == null) continue
      val launched =
        runCatching {
          context.startActivity(intent)
          true
        }.getOrDefault(false)
      if (launched) return true
    }
    return false
  }

  private fun resolveUserHandleExact(userSerialNumber: Long): UserHandle? {
    val myHandle = android.os.Process.myUserHandle()
    val um = userManager ?: return if (userSerialNumber == 0L) myHandle else null
    val resolved = runCatching { um.getUserForSerialNumber(userSerialNumber) }.getOrNull()
    if (resolved != null) return resolved
    val mySerial = runCatching { um.getSerialNumberForUser(myHandle) }.getOrDefault(0L)
    return if (userSerialNumber == mySerial) myHandle else null
  }

  companion object {
    private val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")

    fun normalizeForSearch(input: String): String {
      val decomposed = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
      return DIACRITICS_REGEX.replace(decomposed, "").lowercase(Locale("pt", "BR"))
    }

    fun computeSortSection(normalizedLabel: String): String {
      val first = normalizedLabel.firstOrNull { it.isLetterOrDigit() } ?: return "#"
      return if (first.isDigit()) "#" else first.uppercaseChar().toString()
    }
  }
}
