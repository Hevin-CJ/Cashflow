package com.hevincj.cashflow.data.remote.api

import com.hevincj.cashflow.data.remote.models.ScanResultDto
import com.hevincj.cashflow.data.remote.models.ReceiptScanResponseDto
import com.hevincj.cashflow.data.remote.models.UpiQrResponseDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ScanApi {
    @GET("/api/transactions/scan/{barcode}")
    suspend fun lookupBarcode(@Path("barcode") barcode: String): Response<ScanResultDto>

    @POST("/api/transactions/scan/batch")
    suspend fun lookupBatchBarcodes(@Body barcodes: List<String>): Response<List<ScanResultDto>>

    @Multipart
    @POST("/api/transactions/scan/receipt")
    suspend fun analyzeReceipt(@Part file: MultipartBody.Part): Response<ReceiptScanResponseDto>

    @GET("/api/transactions/upi-qr")
    suspend fun getUpiQr(
        @Query("amount") amount: Double?,
        @Query("note") note: String?
    ): Response<UpiQrResponseDto>
}
