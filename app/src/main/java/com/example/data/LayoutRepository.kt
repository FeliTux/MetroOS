package com.example.data

import android.content.Context
import androidx.core.util.AtomicFile
import com.example.model.AppTargetKey
import com.example.model.MetroFolder
import com.example.model.MetroHub
import com.example.model.QuickNote
import com.example.model.RoleSlot
import com.example.model.StartLayoutState
import com.example.model.StartTile
import com.example.model.TileKind
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

interface LayoutRepository {
  suspend fun loadLayout(): StartLayoutState
  suspend fun saveLayout(state: StartLayoutState): Result<StartLayoutState>
}

class AtomicJsonLayoutRepository(
  private val layoutFile: File,
) : LayoutRepository {

  constructor(context: Context) : this(File(context.filesDir, DEFAULT_FILE_NAME))

  private val mutex = Mutex()
  private val atomicFile = AtomicFile(layoutFile)
  private val backupFile = File(layoutFile.parentFile, "${layoutFile.name}.recovery")

  override suspend fun loadLayout(): StartLayoutState =
    withContext(Dispatchers.IO) {
      mutex.withLock {
        val primaryLoaded =
          if (layoutFile.exists()) {
            tryReadState(atomicFile.readFully())
          } else {
            null
          }
        if (primaryLoaded != null) {
          return@withLock primaryLoaded
        }

        val backupLoaded =
          if (backupFile.exists()) {
            tryReadState(backupFile.readBytes())
          } else {
            null
          }
        if (backupLoaded != null) {
          return@withLock backupLoaded
        }

        StartLayoutState(
          schemaVersion = StartLayoutState.CURRENT_SCHEMA_VERSION,
          revision = 0L,
          initialized = false,
          tiles = emptyList(),
          folders = emptyList(),
          notes = emptyList(),
          hubs = emptyList(),
        )
      }
    }

  private fun tryReadState(bytes: ByteArray): StartLayoutState? {
    return try {
      val raw = bytes.toString(Charsets.UTF_8)
      if (raw.isBlank()) return null
      val json = JSONObject(raw)
      parseState(json)
    } catch (_: Exception) {
      null
    }
  }

  override suspend fun saveLayout(state: StartLayoutState): Result<StartLayoutState> =
    withContext(Dispatchers.IO) {
      mutex.withLock {
        val nextState =
          state.copy(
            schemaVersion = StartLayoutState.CURRENT_SCHEMA_VERSION,
            revision = state.revision + 1L,
            initialized = true,
          )
        var stream: java.io.FileOutputStream? = null
        return@withLock try {
          layoutFile.parentFile?.mkdirs()
          val payload = serializeState(nextState).toString().toByteArray(Charsets.UTF_8)
          stream = atomicFile.startWrite()
          stream.write(payload)
          stream.flush()
          atomicFile.finishWrite(stream)
          stream = null
          writeRecoveryCopy(payload)
          Result.success(nextState)
        } catch (e: IOException) {
          if (stream != null) atomicFile.failWrite(stream)
          Result.failure(e)
        } catch (e: SecurityException) {
          if (stream != null) atomicFile.failWrite(stream)
          Result.failure(e)
        } catch (e: Exception) {
          if (stream != null) atomicFile.failWrite(stream)
          Result.failure(e)
        }
      }
    }

  private fun writeRecoveryCopy(payload: ByteArray) {
    runCatching {
      val tmp = File(layoutFile.parentFile, "${layoutFile.name}.recovery.tmp")
      tmp.writeBytes(payload)
      if (!tmp.renameTo(backupFile)) {
        backupFile.writeBytes(payload)
        tmp.delete()
      }
    }
  }

  private fun serializeState(state: StartLayoutState): JSONObject {
    val root = JSONObject()
    root.put("schemaVersion", state.schemaVersion)
    root.put("revision", state.revision)
    root.put("initialized", state.initialized)

    root.put("tiles", serializeTilesArray(state.tiles))
    root.put("folders", serializeFoldersArray(state.folders))

    val notesArray = JSONArray()
    state.notes.forEach { note ->
      val obj = JSONObject()
      obj.put("id", note.id)
      obj.put("title", note.title)
      obj.put("body", note.body)
      obj.put("isChecklist", note.isChecklist)
      obj.put("updatedAtMs", note.updatedAtMs)
      notesArray.put(obj)
    }
    root.put("notes", notesArray)

    val hubsArray = JSONArray()
    state.hubs.forEach { hub ->
      val obj = JSONObject()
      obj.put("id", hub.id)
      obj.put("name", hub.name)
      if (hub.accentHex != null) obj.put("accentHex", hub.accentHex)
      obj.put("tiles", serializeTilesArray(hub.tiles))
      obj.put("folders", serializeFoldersArray(hub.folders))
      hubsArray.put(obj)
    }
    root.put("hubs", hubsArray)

    return root
  }

  private fun serializeTilesArray(tiles: List<StartTile>): JSONArray {
    val arr = JSONArray()
    tiles.forEach { tile ->
      val obj = JSONObject()
      obj.put("id", tile.id)
      obj.put("kind", tile.kind.name)
      obj.put("col", tile.col)
      obj.put("row", tile.row)
      obj.put("colSpan", tile.colSpan)
      obj.put("rowSpan", tile.rowSpan)
      obj.put("fallbackLabel", tile.fallbackLabel)
      if (tile.roleSlot != null) obj.put("roleSlot", tile.roleSlot.name)
      if (tile.customColorHex != null) obj.put("customColorHex", tile.customColorHex)
      if (tile.folderId != null) obj.put("folderId", tile.folderId)
      if (tile.noteId != null) obj.put("noteId", tile.noteId)
      if (tile.hubId != null) obj.put("hubId", tile.hubId)
      if (tile.appWidgetId != null) obj.put("appWidgetId", tile.appWidgetId)
      obj.put("useOriginalIcon", tile.useOriginalIcon)
      obj.put("isLiveEnabled", tile.isLiveEnabled)
      if (tile.target != null) {
        obj.put("target", serializeAppTargetKey(tile.target))
      }
      arr.put(obj)
    }
    return arr
  }

  private fun serializeFoldersArray(folders: List<MetroFolder>): JSONArray {
    val arr = JSONArray()
    folders.forEach { folder ->
      val obj = JSONObject()
      obj.put("id", folder.id)
      obj.put("name", folder.name)
      val appsArr = JSONArray()
      folder.appKeys.forEach { appsArr.put(serializeAppTargetKey(it)) }
      obj.put("appKeys", appsArr)
      arr.put(obj)
    }
    return arr
  }

  private fun serializeAppTargetKey(key: AppTargetKey): JSONObject {
    return JSONObject().apply {
      put("packageName", key.packageName)
      put("className", key.className)
      put("userSerialNumber", key.userSerialNumber)
    }
  }

  private fun parseAppTargetKey(obj: JSONObject?): AppTargetKey? {
    if (obj == null) return null
    val pkg = obj.optString("packageName", "")
    val cls = obj.optString("className", "")
    val serial = obj.optLong("userSerialNumber", 0L)
    return if (pkg.isNotBlank() && cls.isNotBlank()) AppTargetKey(pkg, cls, serial) else null
  }

  private fun parseTilesArray(tilesArray: JSONArray?): List<StartTile> {
    if (tilesArray == null) return emptyList()
    return buildList {
      for (i in 0 until tilesArray.length()) {
        val obj = tilesArray.optJSONObject(i) ?: continue
        val id = obj.optString("id", "").takeIf { it.isNotBlank() } ?: continue
        val kindName = obj.optString("kind", TileKind.APP_SHORTCUT.name)
        val kind = TileKind.entries.firstOrNull { it.name == kindName } ?: TileKind.APP_SHORTCUT
        val col = obj.optInt("col", 0).coerceIn(0, 5)
        val row = obj.optInt("row", 0).coerceAtLeast(0)
        val colSpan = obj.optInt("colSpan", 2).coerceIn(1, 6 - col)
        val rowSpan = obj.optInt("rowSpan", 2).coerceIn(1, 4)
        val fallbackLabel = obj.optString("fallbackLabel", "")
        val roleSlotName = obj.optString("roleSlot", "")
        val roleSlot = RoleSlot.entries.firstOrNull { it.name == roleSlotName }
        val customColorHex =
          if (obj.has("customColorHex") && !obj.isNull("customColorHex")) obj.getLong("customColorHex") else null
        val folderId = obj.optString("folderId", "").takeIf { it.isNotBlank() }
        val noteId = obj.optString("noteId", "").takeIf { it.isNotBlank() }
        val hubId = obj.optString("hubId", "").takeIf { it.isNotBlank() }
        val appWidgetId =
          if (obj.has("appWidgetId") && !obj.isNull("appWidgetId")) obj.getInt("appWidgetId") else null
        val useOriginalIcon = obj.optBoolean("useOriginalIcon", true)
        val isLiveEnabled = obj.optBoolean("isLiveEnabled", true)
        val target = parseAppTargetKey(obj.optJSONObject("target"))

        add(
          StartTile(
            id = id,
            kind = kind,
            col = col,
            row = row,
            colSpan = colSpan,
            rowSpan = rowSpan,
            target = target,
            fallbackLabel = fallbackLabel,
            roleSlot = roleSlot,
            customColorHex = customColorHex,
            folderId = folderId,
            noteId = noteId,
            hubId = hubId,
            appWidgetId = appWidgetId,
            useOriginalIcon = useOriginalIcon,
            isLiveEnabled = isLiveEnabled,
          )
        )
      }
    }
  }

  private fun parseFoldersArray(foldersArray: JSONArray?): List<MetroFolder> {
    if (foldersArray == null) return emptyList()
    return buildList {
      for (i in 0 until foldersArray.length()) {
        val obj = foldersArray.optJSONObject(i) ?: continue
        val id = obj.optString("id", "").takeIf { it.isNotBlank() } ?: continue
        val name = obj.optString("name", "Pasta")
        val appsArr = obj.optJSONArray("appKeys") ?: JSONArray()
        val appKeys = buildList {
          for (j in 0 until appsArr.length()) {
            parseAppTargetKey(appsArr.optJSONObject(j))?.let { add(it) }
          }
        }
        add(MetroFolder(id = id, name = name, appKeys = appKeys))
      }
    }
  }

  private fun parseState(json: JSONObject): StartLayoutState {
    val schemaVersion = json.optInt("schemaVersion", 1)
    val revision = json.optLong("revision", 0L)
    val initialized = json.optBoolean("initialized", false)

    val tiles = parseTilesArray(json.optJSONArray("tiles"))
    val folders = parseFoldersArray(json.optJSONArray("folders"))

    val notesArray = json.optJSONArray("notes") ?: JSONArray()
    val notes = buildList {
      for (i in 0 until notesArray.length()) {
        val obj = notesArray.optJSONObject(i) ?: continue
        val id = obj.optString("id", "").takeIf { it.isNotBlank() } ?: continue
        val title = obj.optString("title", "")
        val body = obj.optString("body", "")
        val isChecklist = obj.optBoolean("isChecklist", false)
        val updatedAtMs = obj.optLong("updatedAtMs", 0L)
        add(QuickNote(id = id, title = title, body = body, isChecklist = isChecklist, updatedAtMs = updatedAtMs))
      }
    }

    val hubsArray = json.optJSONArray("hubs") ?: JSONArray()
    val hubs = buildList {
      for (i in 0 until hubsArray.length()) {
        val obj = hubsArray.optJSONObject(i) ?: continue
        val id = obj.optString("id", "").takeIf { it.isNotBlank() } ?: continue
        val name = obj.optString("name", "Hub")
        val accentHex =
          if (obj.has("accentHex") && !obj.isNull("accentHex")) obj.getLong("accentHex") else null
        val hubTiles = parseTilesArray(obj.optJSONArray("tiles"))
        val hubFolders = parseFoldersArray(obj.optJSONArray("folders"))
        add(MetroHub(id = id, name = name, accentHex = accentHex, tiles = hubTiles, folders = hubFolders))
      }
    }

    return StartLayoutState(
      schemaVersion = maxOf(schemaVersion, StartLayoutState.CURRENT_SCHEMA_VERSION),
      revision = revision,
      initialized = initialized,
      tiles = tiles,
      folders = folders,
      notes = notes,
      hubs = hubs,
    )
  }

  companion object {
    private const val DEFAULT_FILE_NAME = "metro_layout_v1.json"
  }
}
