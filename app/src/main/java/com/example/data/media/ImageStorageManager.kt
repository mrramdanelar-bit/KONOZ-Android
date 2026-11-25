package com.example.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

sealed class ImageLoadResult {
    data class Success(val filePath: String) : ImageLoadResult()
    data class Error(val arabicMessage: String) : ImageLoadResult()
}

class ImageStorageManager(private val context: Context) {

    companion object {
        const val MAX_RAW_FILE_BYTES = 18L * 1024L * 1024L // 18MB raw cap (well below 20MB AI limit)
        const val MAX_AI_DIMENSION = 1600
    }

    private fun imagesDir(): File {
        val dir = File(context.filesDir, "analysis_images")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun createCameraOutputFile(): File {
        return File(imagesDir(), "capture_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
    }

    suspend fun importUriToLocalFile(uri: Uri): ImageLoadResult = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val inputStream = resolver.openInputStream(uri)
                ?: return@withContext ImageLoadResult.Error("الصورة المختارة فارغة أو تعذر فتحها. يرجى اختيار صورة أخرى صالحة.")

            val tempBytes = inputStream.use { it.readBytes() }
            if (tempBytes.isEmpty()) {
                return@withContext ImageLoadResult.Error("الصورة المختارة فارغة أو تالفة. يرجى اختيار صورة أخرى صالحة.")
            }
            if (tempBytes.size.toLong() > MAX_RAW_FILE_BYTES) {
                return@withContext ImageLoadResult.Error("حجم الصورة يتجاوز الحد المسموح به (18 ميغابايت). يرجى اختيار صورة أصغر أو قص المنطقة المطلوبة.")
            }

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(tempBytes, 0, tempBytes.size, options)
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return@withContext ImageLoadResult.Error("الصورة المختارة تالفة أو ليست بصيغة صورة مدعومة.")
            }

            val targetFile = File(imagesDir(), "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
            FileOutputStream(targetFile).use { out ->
                out.write(tempBytes)
            }
            ImageLoadResult.Success(targetFile.absolutePath)
        } catch (e: Exception) {
            ImageLoadResult.Error("تعذر استيراد الصورة إلى ذاكرة التطبيق. يرجى المحاولة مجدداً.")
        }
    }

    suspend fun cropNormalizedRegion(
        sourcePath: String,
        leftFraction: Float,
        topFraction: Float,
        rightFraction: Float,
        bottomFraction: Float
    ): ImageLoadResult = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists() || sourceFile.length() == 0L) {
                return@withContext ImageLoadResult.Error("تعذر العثور على الصورة الأصلية لتطبيق القص.")
            }
            val bitmap = BitmapFactory.decodeFile(sourcePath)
                ?: return@withContext ImageLoadResult.Error("الصورة تالفة ولا يمكن قصها.")

            val x = (leftFraction.coerceIn(0f, 0.9f) * bitmap.width).toInt()
            val y = (topFraction.coerceIn(0f, 0.9f) * bitmap.height).toInt()
            val right = (rightFraction.coerceIn(leftFraction + 0.05f, 1f) * bitmap.width).toInt()
            val bottom = (bottomFraction.coerceIn(topFraction + 0.05f, 1f) * bitmap.height).toInt()
            val width = (right - x).coerceAtLeast(32).coerceAtMost(bitmap.width - x)
            val height = (bottom - y).coerceAtLeast(32).coerceAtMost(bitmap.height - y)

            val cropped = Bitmap.createBitmap(bitmap, x, y, width, height)
            val outFile = File(imagesDir(), "crop_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
            FileOutputStream(outFile).use { out ->
                cropped.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            if (cropped != bitmap) cropped.recycle()
            bitmap.recycle()
            ImageLoadResult.Success(outFile.absolutePath)
        } catch (e: Exception) {
            ImageLoadResult.Error("تعذر قص المنطقة المحددة من الصورة. يمكنك المتابعة بالصورة الأصلية.")
        }
    }

    suspend fun decodeScaledBitmapForAI(filePath: String): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) return@withContext null

        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(filePath, boundsOpts)
        val rawW = boundsOpts.outWidth
        val rawH = boundsOpts.outHeight
        if (rawW <= 0 || rawH <= 0) return@withContext null

        var sampleSize = 1
        while (rawW / sampleSize > MAX_AI_DIMENSION * 2 || rawH / sampleSize > MAX_AI_DIMENSION * 2) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = BitmapFactory.decodeFile(filePath, decodeOpts) ?: return@withContext null

        val maxSide = maxOf(decoded.width, decoded.height)
        if (maxSide <= MAX_AI_DIMENSION) {
            decoded
        } else {
            val scale = MAX_AI_DIMENSION.toFloat() / maxSide.toFloat()
            val scaled = Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true
            )
            if (scaled != decoded) decoded.recycle()
            scaled
        }
    }
}
