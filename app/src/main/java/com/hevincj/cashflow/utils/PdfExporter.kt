package com.hevincj.cashflow.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.hevincj.cashflow.domain.models.Transaction
import java.io.IOException
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object PdfExporter {
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    private val zoneId = ZoneId.systemDefault()
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy", Locale.US)

    fun exportToPdf(context: Context, uri: Uri, data: List<Transaction>): Boolean {
        val pdfDocument = PdfDocument()
        
        // A4 page dimensions in PostScript points: 595 x 842
        val pageWidth = 595
        val pageHeight = 842
        
        // Margins
        val marginStart = 40f
        val marginEnd = 40f
        val marginTop = 50f
        val marginBottom = 50f
        
        // Columns setup (Total width: 595 - 80 = 515)
        val colDateWidth = 75f
        val colCategoryWidth = 85f
        val colTitleDescWidth = 235f
        val colAmountWidth = 120f // Right-aligned
        
        val colDateX = marginStart
        val colCategoryX = colDateX + colDateWidth
        val colTitleDescX = colCategoryX + colCategoryWidth
        val colAmountX = colTitleDescX + colTitleDescWidth
        
        // Setup paints
        val textPaint = TextPaint().apply {
            color = android.graphics.Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val boldPaint = TextPaint().apply {
            color = android.graphics.Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titlePaint = TextPaint().apply {
            color = android.graphics.Color.rgb(129, 33, 253) // IncomePurpleColor #8121FD
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val linePaint = Paint().apply {
            color = android.graphics.Color.LTGRAY
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
        }
        
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        
        var currentY = marginTop
        
        fun drawHeaderAndFooter(canvas: Canvas, pageNum: Int) {
            // Document Title
            canvas.drawText("CashFlow - Transaction Report", marginStart, 40f, titlePaint)
            
            // Subtle boundary lines
            canvas.drawLine(marginStart, 48f, pageWidth - marginEnd, 48f, linePaint)
            canvas.drawLine(marginStart, pageHeight - 45f, pageWidth - marginEnd, pageHeight - 45f, linePaint)
            
            // Footer Page Number
            val footerPaint = Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 8f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("Page $pageNum", pageWidth - marginEnd, pageHeight - 30f, footerPaint)
        }
        
        fun drawTableHeaders(canvas: Canvas, y: Float) {
            canvas.drawText("Date", colDateX, y, boldPaint)
            canvas.drawText("Category", colCategoryX, y, boldPaint)
            canvas.drawText("Title", colTitleDescX, y, boldPaint)
            
            val rightAlignPaint = TextPaint(boldPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText("Amount", colAmountX + colAmountWidth, y, rightAlignPaint)
            
            canvas.drawLine(marginStart, y + 6f, pageWidth - marginEnd, y + 6f, linePaint)
        }
        
        // Initial setup for the first page
        drawHeaderAndFooter(canvas, pageNumber)
        currentY = marginTop + 20f
        drawTableHeaders(canvas, currentY)
        currentY += 20f
        
        var totalIncome = 0.0
        var totalExpenses = 0.0

        for (item in data) {
            val titleText = item.title
            
            // Build text-wrapping layout for title (No description printed)
            val titleLayout = StaticLayout.Builder
                .obtain(titleText, 0, titleText.length, textPaint, colTitleDescWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.0f)
                .setIncludePad(false)
                .build()
                
            val rowHeight = Math.max(22f, titleLayout.height.toFloat() + 8f)
            
            // Pagination Check
            if (currentY + rowHeight > pageHeight - marginBottom - 20f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                
                drawHeaderAndFooter(canvas, pageNumber)
                currentY = marginTop + 20f
                drawTableHeaders(canvas, currentY)
                currentY += 20f
            }
            
            // Format date using standard dd/MM/yy format
            val localDate = Instant.ofEpochMilli(item.timestamp)
                .atZone(zoneId)
                .toLocalDate()
            val formattedDate = localDate.format(dateFormatter)
            
            // Draw standard text columns
            canvas.drawText(formattedDate, colDateX, currentY + 12f, textPaint)
            canvas.drawText(item.category.displayName, colCategoryX, currentY + 12f, textPaint)
            
            // Draw Title (Wrapped)
            canvas.save()
            canvas.translate(colTitleDescX, currentY + 2f)
            titleLayout.draw(canvas)
            canvas.restore()
            
            // Draw Amount (Right-Aligned, using absAmount to prevent double-minus)
            val isIncome = item.type == com.hevincj.cashflow.domain.models.TransactionType.INCOME
            val absAmount = kotlin.math.abs(item.amount)
            if (isIncome) {
                totalIncome += absAmount
            } else {
                totalExpenses += absAmount
            }

            val amountColor = if (isIncome) {
                android.graphics.Color.rgb(16, 124, 65) // positive green
            } else {
                android.graphics.Color.rgb(209, 36, 36) // negative red
            }
            val amountTextPaint = TextPaint(textPaint).apply {
                color = amountColor
                textAlign = Paint.Align.RIGHT
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val formattedAmount = (if (isIncome) "+" else "-") + currencyFormatter.format(absAmount)
            canvas.drawText(formattedAmount, colAmountX + colAmountWidth, currentY + 12f, amountTextPaint)
            
            // Row divider line
            canvas.drawLine(marginStart, currentY + rowHeight, pageWidth - marginEnd, currentY + rowHeight, linePaint)
            
            currentY += rowHeight
        }

        val totalBalance = totalIncome - totalExpenses
        val summaryBoxHeight = 70f
        
        // Summary Box Pagination Check
        if (currentY + summaryBoxHeight > pageHeight - marginBottom) {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            
            drawHeaderAndFooter(canvas, pageNumber)
            currentY = marginTop + 20f
        }

        // Draw Summary Box
        currentY += 15f
        val summaryPaint = Paint().apply {
            color = android.graphics.Color.rgb(129, 33, 253) // #8121FD
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        canvas.drawRoundRect(
            marginStart, 
            currentY, 
            pageWidth - marginEnd, 
            currentY + 55f, 
            8f, 
            8f, 
            summaryPaint
        )

        val rightAlignBoldPaint = TextPaint(boldPaint).apply { textAlign = Paint.Align.RIGHT }
        
        // Row 1: Total Income
        canvas.drawText("Total Income", marginStart + 15f, currentY + 18f, boldPaint)
        canvas.drawText(currencyFormatter.format(totalIncome), pageWidth - marginEnd - 15f, currentY + 18f, rightAlignBoldPaint)

        // Row 2: Total Expenses
        canvas.drawText("Total Expenses", marginStart + 15f, currentY + 33f, boldPaint)
        canvas.drawText(currencyFormatter.format(totalExpenses), pageWidth - marginEnd - 15f, currentY + 33f, rightAlignBoldPaint)

        // Row 3: Total Balance
        canvas.drawText("Total Balance", marginStart + 15f, currentY + 48f, boldPaint)
        val balanceColor = if (totalBalance >= 0) {
            android.graphics.Color.rgb(16, 124, 65) // positive green
        } else {
            android.graphics.Color.rgb(209, 36, 36) // negative red
        }
        val balancePaint = TextPaint(boldPaint).apply { 
            color = balanceColor
            textAlign = Paint.Align.RIGHT 
        }
        canvas.drawText(currencyFormatter.format(totalBalance), pageWidth - marginEnd - 15f, currentY + 48f, balancePaint)

        pdfDocument.finishPage(page)
        
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        } finally {
            pdfDocument.close()
        }
    }
}
