package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoStorageManager(private val context: Context) {

  suspend fun importBoundedPhoto(
    uri: Uri,
    previousPaths: List<String> = emptyList(),
  ): Result<Pair<List<String>, Long>> = importMultipleBoundedPhotos(listOf(uri), previousPaths)

  suspend fun importMultipleBoundedPhotos(
    uris: List<Uri>,
    previousPaths: List<String> = emptyList(),
  ): Result<Pair<List<String>, Long>> =
    withContext(Dispatchers.IO) {
      if (uris.isEmpty()) {
        return@withContext Result.failure(IllegalArgumentException("No URIs selected"))
      }
      val photoDir = File(context.filesDir, "photos").apply { mkdirs() }
      val importedPaths = mutableListOf<String>()
      val rev = System.currentTimeMillis()

      try {
        for (uri in uris.take(MAX_PHOTOS)) {
          val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
          context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
          } ?: continue

          val srcWidth = boundsOpts.outWidth
          val srcHeight = boundsOpts.outHeight
          if (srcWidth <= 0 || srcHeight <= 0) continue

          val sampleSize = computeSampleSize(srcWidth, srcHeight, MAX_EDGE_PX)
          val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
          val decoded =
            context.contentResolver.openInputStream(uri)?.use { stream ->
              BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: continue

          val orientation = readExifOrientation(uri)
          val oriented = rotateIfNeeded(decoded, orientation)
          if (oriented !== decoded) {
            decoded.recycle()
          }

          val fileToken = "${rev}_${UUID.randomUUID().toString().take(6)}"
          val targetFile = File(photoDir, "start_photo_$fileToken.jpg")
          val tempFile = File(photoDir, "start_photo_$fileToken.tmp")
          try {
            FileOutputStream(tempFile).use { out ->
              val compressed = oriented.compress(Bitmap.CompressFormat.JPEG, 86, out)
              out.flush()
              if (!compressed) throw IllegalStateException("Compression failed")
            }
            if (!tempFile.renameTo(targetFile)) {
              throw IllegalStateException("Atomic photo rename failed")
            }
            importedPaths.add(targetFile.absolutePath)
          } finally {
            oriented.recycle()
            if (tempFile.exists()) runCatching { tempFile.delete() }
          }
        }

        if (importedPaths.isEmpty()) {
          return@withContext Result.failure(IllegalStateException("Could not decode any selected image"))
        }

        for (oldPath in previousPaths) {
          if (oldPath !in importedPaths) {
            runCatching { File(oldPath).delete() }
          }
        }

        Result.success(importedPaths to rev)
      } catch (e: Exception) {
        for (newPath in importedPaths) {
          runCatching { File(newPath).delete() }
        }
        Result.failure(e)
      }
    }

  suspend fun loadCropPreviewBitmap(uri: Uri, maxEdgePx: Int = 512): ImageBitmap? =
    withContext(Dispatchers.IO) {
      decodeOrientedSampled(uri, maxEdgePx)?.asImageBitmap()
    }

  suspend fun importCroppedProfileAvatar(
    uri: Uri,
    normCenterX: Float = 0.5f,
    normCenterY: Float = 0.5f,
    zoom: Float = 1f,
    targetPx: Int = PROFILE_AVATAR_PX,
    previousPath: String? = null,
  ): Result<Pair<String, Long>> =
    withContext(Dispatchers.IO) {
      val profileDir = File(context.filesDir, "profile").apply { mkdirs() }
      val rev = System.currentTimeMillis()
      val boundedTarget = targetPx.coerceIn(96, PROFILE_AVATAR_PX)
      var oriented: Bitmap? = null
      var cropped: Bitmap? = null
      var scaled: Bitmap? = null
      val fileToken = "${rev}_${UUID.randomUUID().toString().take(6)}"
      val targetFile = File(profileDir, "avatar_$fileToken.jpg")
      val tempFile = File(profileDir, "avatar_$fileToken.tmp")

      try {
        oriented =
          decodeOrientedSampled(uri, boundedTarget * 3)
            ?: return@withContext Result.failure(IllegalStateException("Could not decode avatar image"))

        val srcW = oriented.width
        val srcH = oriented.height
        val baseSide = minOf(srcW, srcH)
        val safeZoom = zoom.coerceIn(1f, 3f)
        val cropSide = (baseSide / safeZoom).toInt().coerceIn(32, baseSide)
        val maxLeft = (srcW - cropSide).coerceAtLeast(0)
        val maxTop = (srcH - cropSide).coerceAtLeast(0)
        val left = ((normCenterX.coerceIn(0f, 1f) * maxLeft)).toInt().coerceIn(0, maxLeft)
        val top = ((normCenterY.coerceIn(0f, 1f) * maxTop)).toInt().coerceIn(0, maxTop)

        cropped = Bitmap.createBitmap(oriented, left, top, cropSide, cropSide)
        scaled =
          if (cropped.width == boundedTarget && cropped.height == boundedTarget) {
            cropped
          } else {
            Bitmap.createScaledBitmap(cropped, boundedTarget, boundedTarget, true)
          }

        FileOutputStream(tempFile).use { out ->
          val ok = scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
          out.flush()
          if (!ok) throw IllegalStateException("Avatar compression failed")
        }
        if (!tempFile.renameTo(targetFile)) {
          throw IllegalStateException("Atomic avatar rename failed")
        }

        if (!previousPath.isNullOrBlank() && previousPath != targetFile.absolutePath) {
          runCatching { File(previousPath).delete() }
        }
        Result.success(targetFile.absolutePath to rev)
      } catch (e: Exception) {
        if (targetFile.exists()) runCatching { targetFile.delete() }
        Result.failure(e)
      } finally {
        if (tempFile.exists()) runCatching { tempFile.delete() }
        if (scaled != null && scaled !== cropped && scaled !== oriented) scaled.recycle()
        if (cropped != null && cropped !== oriented) cropped.recycle()
        oriented?.recycle()
      }
    }

  suspend fun loadProfileAvatar(path: String?, maxEdgePx: Int = PROFILE_AVATAR_PX): ImageBitmap? =
    withContext(Dispatchers.IO) {
      if (path.isNullOrBlank()) return@withContext null
      val file = File(path)
      if (!file.exists() || !file.canRead() || file.length() <= 0L) return@withContext null
      try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return@withContext null
        val boundedEdge = maxEdgePx.coerceIn(64, PROFILE_AVATAR_PX)
        val sample = computeSampleSize(opts.outWidth, opts.outHeight, boundedEdge)
        val decodeOpts =
          BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
          }
        BitmapFactory.decodeFile(path, decodeOpts)?.asImageBitmap()
      } catch (_: Exception) {
        null
      }
    }

  suspend fun deleteProfileAvatar(path: String?) =
    withContext(Dispatchers.IO) {
      if (!path.isNullOrBlank()) {
        runCatching { File(path).delete() }
      }
    }

  suspend fun deletePhotoFiles(paths: List<String>) =
    withContext(Dispatchers.IO) {
      for (path in paths) {
        if (path.isNotBlank()) {
          runCatching { File(path).delete() }
        }
      }
    }

  suspend fun loadPhotoBitmap(path: String?, maxEdgePx: Int = MAX_EDGE_PX): ImageBitmap? =
    withContext(Dispatchers.IO) {
      if (path.isNullOrBlank()) return@withContext null
      val file = File(path)
      if (!file.exists() || !file.canRead() || file.length() <= 0L) return@withContext null
      try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return@withContext null
        val boundedEdge = maxEdgePx.coerceIn(160, MAX_EDGE_PX)
        val sample = computeSampleSize(opts.outWidth, opts.outHeight, boundedEdge)
        val decodeOpts =
          BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
          }
        BitmapFactory.decodeFile(path, decodeOpts)?.asImageBitmap()
      } catch (_: Exception) {
        null
      }
    }

  fun isPhotoAvailable(path: String?): Boolean {
    if (path.isNullOrBlank()) return false
    val file = File(path)
    return file.exists() && file.canRead() && file.length() > 0L
  }

  private fun decodeOrientedSampled(uri: Uri, maxEdge: Int): Bitmap? {
    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { stream ->
      BitmapFactory.decodeStream(stream, null, boundsOpts)
    } ?: return null

    val srcWidth = boundsOpts.outWidth
    val srcHeight = boundsOpts.outHeight
    if (srcWidth <= 0 || srcHeight <= 0) return null

    val sampleSize = computeSampleSize(srcWidth, srcHeight, maxEdge)
    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val decoded =
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, decodeOpts)
      } ?: return null

    val orientation = readExifOrientation(uri)
    val oriented = rotateIfNeeded(decoded, orientation)
    if (oriented !== decoded) {
      decoded.recycle()
    }
    return oriented
  }

  private fun readExifOrientation(uri: Uri): Int {
    return try {
      context.contentResolver.openInputStream(uri)?.use { stream ->
        ExifInterface(stream)
          .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
      } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (_: Exception) {
      ExifInterface.ORIENTATION_NORMAL
    }
  }

  private fun rotateIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
    val degrees =
      when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> return bitmap
      }
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
  }

  private fun computeSampleSize(width: Int, height: Int, maxEdge: Int): Int {
    var sample = 1
    val longest = maxOf(width, height)
    while (longest / sample > maxEdge) {
      sample *= 2
    }
    return sample
  }

  companion object {
    private const val MAX_EDGE_PX = 1200
    private const val MAX_PHOTOS = 12
    const val PROFILE_AVATAR_PX = 256
  }
}
