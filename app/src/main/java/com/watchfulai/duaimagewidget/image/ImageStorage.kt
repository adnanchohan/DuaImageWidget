package com.watchfulai.duaimagewidget.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class StoredImage(
    val fileName: String,
    val bitmap: Bitmap,
)

object ImageStorage {
    private const val DIRECTORY = "dua_images"
    private const val MAX_SOURCE_EDGE = 2_048

    suspend fun import(context: Context, uri: Uri): StoredImage = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val orientation = runCatching {
            resolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri)
            ?: error("The selected image could not be opened")
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) {
            "The selected file is not a supported image"
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: error("The selected image could not be decoded")
        val normalized = applyOrientation(decoded, orientation)

        val directory = directory(context).apply { mkdirs() }
        val extension = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "webp" else "png"
        val file = File(directory, "dua_${UUID.randomUUID()}.$extension")
        try {
            file.outputStream().buffered().use { output ->
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSLESS
                } else {
                    Bitmap.CompressFormat.PNG
                }
                check(normalized.compress(format, 100, output)) {
                    "The selected image could not be saved"
                }
            }
        } catch (throwable: Throwable) {
            file.delete()
            if (normalized !== decoded) normalized.recycle()
            decoded.recycle()
            throw throwable
        }

        if (normalized !== decoded) decoded.recycle()
        StoredImage(file.name, normalized)
    }

    suspend fun load(context: Context, fileName: String): Bitmap? = withContext(Dispatchers.IO) {
        val file = file(context, fileName)
        if (!file.isFile) return@withContext null
        BitmapFactory.decodeFile(file.path)
    }

    fun delete(context: Context, fileName: String) {
        file(context, fileName).delete()
    }

    private fun directory(context: Context) = File(context.filesDir, DIRECTORY)

    private fun file(context: Context, fileName: String): File {
        require(fileName == File(fileName).name) { "Invalid image file name" }
        return File(directory(context), fileName)
    }

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (width / sample > MAX_SOURCE_EDGE || height / sample > MAX_SOURCE_EDGE) {
            sample *= 2
        }
        return sample
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
