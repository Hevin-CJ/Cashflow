package com.hevincj.cashflow.utils

import android.content.Context
import android.net.Uri
import com.hevincj.cashflow.domain.models.Transaction
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object CsvExporter {
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    private val zoneId = ZoneId.systemDefault()
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy", Locale.US)

    fun exportToCsv(context: Context, uri: Uri, data: List<Transaction>): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8)).use { writer ->
                    // Write BOM for Excel UTF-8 support
                    writer.write("\uFEFF")
                    
                    // Write Header Row (8 columns total)
                    writer.write("Date,Title,Category,Type,Amount,Description,Barcode,Product Name\n")
                    
                    var totalIncome = 0.0
                    var totalExpenses = 0.0

                    // Write Data Rows
                    data.forEach { item ->
                        val localDate = Instant.ofEpochMilli(item.timestamp)
                            .atZone(zoneId)
                            .toLocalDate()
                        val date = localDate.format(dateFormatter).escapeCsv()
                        
                        val title = item.title.escapeCsv()
                        val cat = item.category.displayName.escapeCsv()
                        val type = item.type.name.escapeCsv()
                        
                        val isIncome = item.type == com.hevincj.cashflow.domain.models.TransactionType.INCOME
                        val absAmount = kotlin.math.abs(item.amount)
                        if (isIncome) {
                            totalIncome += absAmount
                        } else {
                            totalExpenses += absAmount
                        }
                        
                        // Format with Rupee sign: +₹4,500.00 or -₹120.00
                        val formattedAmount = (if (isIncome) "+" else "-") + currencyFormatter.format(absAmount)
                        val amount = formattedAmount.escapeCsv()
                        
                        val desc = (item.description ?: "").escapeCsv()
                        val barcode = (item.barcode ?: "").escapeCsv()
                        val productName = (item.productName ?: "").escapeCsv()
                        
                        writer.write("$date,$title,$cat,$type,$amount,$desc,$barcode,$productName\n")
                    }

                    val totalBalance = totalIncome - totalExpenses

                    val incomeStr = currencyFormatter.format(totalIncome).escapeCsv()
                    val expensesStr = currencyFormatter.format(totalExpenses).escapeCsv()
                    val balanceStr = currencyFormatter.format(totalBalance).escapeCsv()

                    // Write Summary Rows (Amount column aligned at index 4, exactly 3 trailing commas)
                    writer.write("\n")
                    writer.write("Total Income,,,,$incomeStr,,,\n")
                    writer.write("Total Expenses,,,,$expensesStr,,,\n")
                    writer.write("Total Balance,,,,$balanceStr,,,\n")
                    
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun String.escapeCsv(): String {
        val needsQuotes = this.contains(",") || this.contains("\"") || this.contains("\n") || this.contains("\r")
        return if (needsQuotes) {
            "\"" + this.replace("\"", "\"\"") + "\""
        } else {
            this
        }
    }
}
