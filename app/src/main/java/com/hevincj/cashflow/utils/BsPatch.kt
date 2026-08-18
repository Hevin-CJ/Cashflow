package com.hevincj.cashflow.utils

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

object BsPatch {

    private const val HEADER_MAGIC = "BSDIFF40"

    /**
     * Applies a standard BSDIFF40 patch file to [oldFile] to synthesize [newFile].
     * @param oldFile The currently installed APK file (e.g. from context.applicationInfo.sourceDir)
     * @param patchFile The downloaded .patch delta file
     * @param newFile The target reconstructed .apk file
     * @return true if the patch was successfully applied and verified, false otherwise.
     */
    fun applyPatch(oldFile: File, patchFile: File, newFile: File): Boolean {
        if (!oldFile.exists() || !patchFile.exists()) return false

        try {
            val patchBytes = patchFile.readBytes()
            if (patchBytes.size < 32) return false

            val magic = String(patchBytes, 0, 8, Charsets.US_ASCII)
            if (magic != HEADER_MAGIC) return false

            val headerBuffer = ByteBuffer.wrap(patchBytes, 8, 24).order(ByteOrder.LITTLE_ENDIAN)
            val ctrlLen = readOffT(headerBuffer)
            val diffLen = readOffT(headerBuffer)
            val newSize = readOffT(headerBuffer)

            if (ctrlLen < 0 || diffLen < 0 || newSize < 0) return false
            if (32 + ctrlLen + diffLen > patchBytes.size) return false

            val oldBytes = oldFile.readBytes()
            val oldSize = oldBytes.size

            val ctrlStream = BZip2CompressorInputStream(
                ByteArrayInputStream(patchBytes, 32, ctrlLen.toInt())
            )
            val diffStream = BZip2CompressorInputStream(
                ByteArrayInputStream(patchBytes, (32 + ctrlLen).toInt(), diffLen.toInt())
            )
            val extraStream = BZip2CompressorInputStream(
                ByteArrayInputStream(
                    patchBytes,
                    (32 + ctrlLen + diffLen).toInt(),
                    (patchBytes.size - 32 - ctrlLen - diffLen).toInt()
                )
            )

            val newBytes = ByteArray(newSize.toInt())
            var oldPos = 0
            var newPos = 0

            val ctrlBuffer = ByteArray(24)
            while (newPos < newSize) {
                var bytesRead = 0
                while (bytesRead < 24) {
                    val count = ctrlStream.read(ctrlBuffer, bytesRead, 24 - bytesRead)
                    if (count <= 0) break
                    bytesRead += count
                }
                if (bytesRead < 24) break

                val ctrlBuf = ByteBuffer.wrap(ctrlBuffer).order(ByteOrder.LITTLE_ENDIAN)
                val diffCount = readOffT(ctrlBuf).toInt()
                val extraCount = readOffT(ctrlBuf).toInt()
                val seekOffset = readOffT(ctrlBuf).toInt()

                if (newPos + diffCount > newSize) return false

                // 1. Read diff block and add to oldBytes
                var diffRead = 0
                while (diffRead < diffCount) {
                    val count = diffStream.read(newBytes, newPos + diffRead, diffCount - diffRead)
                    if (count <= 0) break
                    diffRead += count
                }

                for (i in 0 until diffCount) {
                    if (oldPos + i in 0 until oldSize) {
                        newBytes[newPos + i] = ((newBytes[newPos + i].toInt() + oldBytes[oldPos + i].toInt()) and 0xFF).toByte()
                    }
                }
                newPos += diffCount
                oldPos += diffCount

                // 2. Read extra block directly into newBytes
                if (newPos + extraCount > newSize) return false
                var extraRead = 0
                while (extraRead < extraCount) {
                    val count = extraStream.read(newBytes, newPos + extraRead, extraCount - extraRead)
                    if (count <= 0) break
                    extraRead += count
                }
                newPos += extraCount
                oldPos += seekOffset
            }

            ctrlStream.close()
            diffStream.close()
            extraStream.close()

            if (newPos.toLong() != newSize) return false

            newFile.parentFile?.mkdirs()
            newFile.writeBytes(newBytes)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun readOffT(buffer: ByteBuffer): Long {
        val raw = buffer.long
        return if ((raw and (1L shl 63)) != 0L) {
            -(raw and (1L shl 63).inv())
        } else {
            raw
        }
    }
}
