package com.example.data

import android.content.ComponentCallbacks2
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.UserHandle
import android.os.UserManager
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.model.AppTargetKey
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

data class LoadedAppIcon(
  val targetKey: AppTargetKey,
  val generation: Long,
  val bitmap: ImageBitmap?,
)

interface AppIconLoader {
  val cacheGeneration: StateFlow<Long>
  suspend fun loadIcon(targetKey: AppTargetKey, maxPx: Int = DEFAULT_ICON_PX): LoadedAppIcon
  fun invalidatePackage(packageName: String)
  fun rebuildCache()
  fun setLightweightMode(enabled: Boolean)
  fun onTrimMemory(level: Int)

  companion object {
    const val DEFAULT_ICON_PX = 144
    const val COMPACT_ICON_PX = 84
  }
}

class AndroidAppIconLoader(
  private val context: Context,
  private val launcherApps: LauncherApps? = context.getSystemService(LauncherApps::class.java),
  private val userManager: UserManager? = context.getSystemService(UserManager::class.java),
  private val packageManager: PackageManager = context.packageManager,
) : AppIconLoader {

  private val generationCounter = AtomicLong(1L)
  private val _cacheGeneration = MutableStateFlow(1L)
  override val cacheGeneration: StateFlow<Long> = _cacheGeneration.asStateFlow()

  private var lightweightMode: Boolean = false
  private val iconDensityHint = context.resources.displayMetrics.densityDpi
  private val decodeSemaphore = Semaphore(4)

  private val cacheLock = Any()
  private var iconCache = createCache(STANDARD_MAX_ENTRIES)

  private fun createCache(maxEntries: Int): LruCache<String, ImageBitmap> =
    object : LruCache<String, ImageBitmap>(maxEntries) {
      override fun sizeOf(key: String, value: ImageBitmap): Int = 1
    }

  private fun cacheKey(key: AppTargetKey, maxPx: Int, generation: Long): String =
    "${key.stableKey}@${maxPx}px@${iconDensityHint}dpi#g$generation"

  override suspend fun loadIcon(targetKey: AppTargetKey, maxPx: Int): LoadedAppIcon {
    val startGeneration = generationCounter.get()
    val boundedMaxPx =
      if (lightweightMode) minOf(maxPx, AppIconLoader.COMPACT_ICON_PX) else maxPx
    val k = cacheKey(targetKey, boundedMaxPx, startGeneration)

    synchronized(cacheLock) {
      iconCache.get(k)?.let { cached ->
        return LoadedAppIcon(targetKey = targetKey, generation = startGeneration, bitmap = cached)
      }
    }

    currentCoroutineContext().ensureActive()
    val decoded =
      decodeSemaphore.withPermit {
        currentCoroutineContext().ensureActive()
        withContext(Dispatchers.IO) {
          currentCoroutineContext().ensureActive()
          if (generationCounter.get() != startGeneration) {
            return@withContext null
          }
          decodeActivityIcon(targetKey, boundedMaxPx)
        }
      }

    // Only populate cache if the cache wasn't invalidated/rebuilt while decoding
    if (decoded != null && generationCounter.get() == startGeneration) {
      synchronized(cacheLock) {
        iconCache.put(k, decoded)
      }
    }

    return LoadedAppIcon(
      targetKey = targetKey,
      generation = startGeneration,
      bitmap = decoded,
    )
  }

  override fun invalidatePackage(packageName: String) {
    synchronized(cacheLock) {
      val prefix = "$packageName/"
      val snapshotKeys = iconCache.snapshot().keys
      for (key in snapshotKeys) {
        if (key.startsWith(prefix)) {
          iconCache.remove(key)
        }
      }
    }
    val nextGen = generationCounter.incrementAndGet()
    _cacheGeneration.value = nextGen
  }

  override fun rebuildCache() {
    synchronized(cacheLock) {
      iconCache.evictAll()
    }
    val nextGen = generationCounter.incrementAndGet()
    _cacheGeneration.value = nextGen
  }

  override fun setLightweightMode(enabled: Boolean) {
    if (lightweightMode == enabled) return
    lightweightMode = enabled
    val targetLimit = if (enabled) LOW_RAM_MAX_ENTRIES else STANDARD_MAX_ENTRIES
    synchronized(cacheLock) {
      iconCache.resize(targetLimit)
    }
  }

  override fun onTrimMemory(level: Int) {
    synchronized(cacheLock) {
      when {
        level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
          level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
          iconCache.evictAll()
        }
        level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
          iconCache.trimToSize(if (lightweightMode) 12 else 40)
        }
      }
    }
  }

  private fun decodeActivityIcon(targetKey: AppTargetKey, maxPx: Int): ImageBitmap? {
    val profile = resolveUserHandleExact(targetKey.userSerialNumber) ?: return null
    val component = ComponentName(targetKey.packageName, targetKey.className)
    val activityInfo: LauncherActivityInfo? =
      runCatching {
        launcherApps?.getActivityList(targetKey.packageName, profile)?.firstOrNull {
          it.componentName == component
        }
      }.getOrNull()

    val drawable: Drawable? =
      activityInfo?.let { runCatching { it.getBadgedIcon(iconDensityHint) }.getOrNull() }
        ?: runCatching {
          if (profile == android.os.Process.myUserHandle()) {
            packageManager.getActivityIcon(component)
          } else {
            null
          }
        }.getOrNull()

    return drawable?.let { renderBoundedBitmap(it, maxPx) }?.asImageBitmap()
  }

  private fun resolveUserHandleExact(userSerialNumber: Long): UserHandle? {
    val myHandle = android.os.Process.myUserHandle()
    val um = userManager ?: return if (userSerialNumber == 0L) myHandle else null
    val resolved = runCatching { um.getUserForSerialNumber(userSerialNumber) }.getOrNull()
    if (resolved != null) return resolved
    val mySerial = runCatching { um.getSerialNumberForUser(myHandle) }.getOrDefault(0L)
    return if (userSerialNumber == mySerial) myHandle else null
  }

  private fun renderBoundedBitmap(drawable: Drawable, maxPx: Int): Bitmap? {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
      val src = drawable.bitmap
      if (src.width <= maxPx && src.height <= maxPx) return src
      val scale = minOf(maxPx.toFloat() / src.width, maxPx.toFloat() / src.height)
      val w = (src.width * scale).toInt().coerceAtLeast(1)
      val h = (src.height * scale).toInt().coerceAtLeast(1)
      return Bitmap.createScaledBitmap(src, w, h, true)
    }
    val intrinsicW = drawable.intrinsicWidth.takeIf { it > 0 } ?: maxPx
    val intrinsicH = drawable.intrinsicHeight.takeIf { it > 0 } ?: maxPx
    val scale = minOf(1f, maxPx.toFloat() / maxOf(intrinsicW, intrinsicH))
    val w = (intrinsicW * scale).toInt().coerceAtLeast(1)
    val h = (intrinsicH * scale).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, w, h)
    drawable.draw(canvas)
    return bitmap
  }

  companion object {
    private const val STANDARD_MAX_ENTRIES = 128
    private const val LOW_RAM_MAX_ENTRIES = 32
  }
}
