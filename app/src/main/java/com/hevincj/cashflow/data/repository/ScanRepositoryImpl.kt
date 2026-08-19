package com.hevincj.cashflow.data.repository

import android.content.Context
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.hevincj.cashflow.data.remote.api.ScanApi
import com.hevincj.cashflow.domain.models.ReceiptScanResult
import com.hevincj.cashflow.domain.models.ScanResult
import com.hevincj.cashflow.domain.models.UpiQrResult
import com.hevincj.cashflow.domain.repository.ScanRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ScanRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val scanApi: ScanApi
) : ScanRepository {

    override suspend fun scanSingleBarcode(): String? = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            try {
                val options = GmsBarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                    .enableAutoZoom()
                    .build()

                val scanner = GmsBarcodeScanning.getClient(context, options)

                scanner.startScan()
                    .addOnSuccessListener { barcode ->
                        if (continuation.isActive) {
                            continuation.resume(barcode.rawValue)
                        }
                    }
                    .addOnFailureListener { exception ->
                        com.hevincj.cashflow.utils.CrashLogger.w("ScanRepositoryImpl", "Google Code Scanner failed: ${exception.message}", exception)
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                    .addOnCanceledListener {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
            } catch (e: Exception) {
                com.hevincj.cashflow.utils.CrashLogger.e("ScanRepositoryImpl", "Failed to start Google Code Scanner", e)
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }
    }

    override suspend fun lookupBarcode(barcode: String): ScanResult? = withContext(Dispatchers.IO) {
        try {
            val response = scanApi.lookupBarcode(barcode)
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    ScanResult(
                        barcode = dto.barcode,
                        productName = dto.productName,
                        category = dto.category,
                        price = dto.price,
                        currency = dto.currency
                    )
                }
            } else {
                com.hevincj.cashflow.utils.CrashLogger.w("ScanRepositoryImpl", "Barcode lookup unsuccessful for $barcode: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("ScanRepositoryImpl", "Error looking up barcode: $barcode", e)
            null
        }
    }

    override suspend fun lookupBatchBarcodes(barcodes: List<String>): List<ScanResult> = withContext(Dispatchers.IO) {
        try {
            val response = scanApi.lookupBatchBarcodes(barcodes)
            if (response.isSuccessful) {
                response.body()?.map { dto ->
                    ScanResult(
                        barcode = dto.barcode,
                        productName = dto.productName,
                        category = dto.category,
                        price = dto.price,
                        currency = dto.currency
                    )
                } ?: emptyList()
            } else {
                com.hevincj.cashflow.utils.CrashLogger.w("ScanRepositoryImpl", "Batch barcode lookup unsuccessful: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("ScanRepositoryImpl", "Error in batch lookup", e)
            emptyList()
        }
    }

    override suspend fun analyzeReceipt(imageBytes: ByteArray): ReceiptScanResult? = withContext(Dispatchers.IO) {
        try {
            val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", "receipt.jpg", requestBody)
            val response = scanApi.analyzeReceipt(body)
            if (response.isSuccessful) {
                val dto = response.body() ?: return@withContext null
                ReceiptScanResult(
                    merchant = dto.description,
                    amount = dto.totalAmount,
                    date = null,
                    category = dto.category,
                    description = dto.description
                )
            } else {
                com.hevincj.cashflow.utils.CrashLogger.w("ScanRepositoryImpl", "Analyze receipt unsuccessful: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.e("ScanRepositoryImpl", "Error parsing receipt via backend", e)
            null
        }
    }

    override suspend fun generateUpiQr(amount: Double?, note: String?): UpiQrResult? = withContext(Dispatchers.IO) {
        try {
            val response = scanApi.getUpiQr(amount, note)
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    UpiQrResult(
                        upiUri = dto.upiUri,
                        qrCodeUrl = dto.qrCodeUrl
                    )
                }
            } else {
                com.hevincj.cashflow.utils.CrashLogger.w("ScanRepositoryImpl", "Generate UPI QR code unsuccessful: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("ScanRepositoryImpl", "Error generating UPI QR code", e)
            null
        }
    }
}
