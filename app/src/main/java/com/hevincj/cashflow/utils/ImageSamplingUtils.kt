package com.hevincj.cashflow.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.max

object ImageSamplingUtils {

    /**
     * Calculates the optimal inSampleSize (power of 2) for decoding a bitmap
     * without exceeding the requested bounding dimensions, saving substantial memory.
     */
    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return max(1, inSampleSize)
    }

    /**
     * Decodes a downsampled bitmap from raw byte array to cap heap usage.
     */
    fun decodeSampledBitmapFromByteArray(
        bytes: ByteArray,
        reqWidth: Int = 1280,
        reqHeight: Int = 1280
    ): Bitmap? {
        if (bytes.isEmpty()) return null
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (e: Throwable) {
            CrashLogger.e("ImageSamplingUtils", "Error decoding sampled byte array", e)
            null
        }
    }

    /**
     * Decodes a downsampled bitmap directly from a content URI on IO dispatcher.
     */
    suspend fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int = 1280,
        reqHeight: Int = 1280
    ): Bitmap? = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false

            inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            bitmap
        } catch (e: Throwable) {
            CrashLogger.e("ImageSamplingUtils", "Error decoding sampled URI bitmap", e)
            null
        } finally {
            try {
                inputStream?.close()
            } catch (_: Throwable) {}
        }
    }
}
