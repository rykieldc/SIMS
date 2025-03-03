package com.example.sims

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.kernel.pdf.PdfDocument as ITextPdfDocument
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import io.opencensus.stats.View
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import kotlin.math.absoluteValue

class GenerateReportsActivity : AppCompatActivity() {

    private lateinit var header: TextView
    private lateinit var reportTextView: TextView
    private lateinit var generateMonthlyReportButton: Button
    private lateinit var monthSpinner: Spinner
    private lateinit var yearSpinner: Spinner
    private lateinit var historyRef: DatabaseReference
    private val databaseHelper = FirebaseDatabaseHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_reports)

        header = findViewById(R.id.header)

        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_back_arrow_circle)
        drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)

        val spannableString = SpannableString("  ${header.text}")
        spannableString.setSpan(
            ImageSpan(drawable!!, DynamicDrawableSpan.ALIGN_BASELINE),
            0,
            1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString.setSpan(
            DrawableClickSpan { onBackPressedDispatcher.onBackPressed() },
            0,
            1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        header.text = spannableString
        header.movementMethod = LinkMovementMethod.getInstance()

        reportTextView = findViewById(R.id.reportTextView)
        generateMonthlyReportButton = findViewById(R.id.generateMonthlyReportButton)
        monthSpinner = findViewById(R.id.monthSpinner)
        yearSpinner = findViewById(R.id.yearSpinner)

        historyRef = FirebaseDatabase.getInstance().getReference("history")

        setupSpinners()

        generateMonthlyReportButton.setOnClickListener {
            val selectedMonth = monthSpinner.selectedItem.toString().substring(0, 2) // Extract MM format
            val selectedYear = yearSpinner.selectedItem.toString() // Extract YYYY format

            Log.d("REPORT_DEBUG", "Generating report for Month: $selectedMonth, Year: $selectedYear")

            generateMonthlyReport(this, selectedMonth, selectedYear)
        }
    }

    class DrawableClickSpan(private val clickListener: () -> Unit) : ClickableSpan() {
        override fun onClick(widget: android.view.View) {
            clickListener()
        }
    }

    private fun setupSpinners() {
        val months = listOf(
            "01 - January", "02 - February", "03 - March", "04 - April",
            "05 - May", "06 - June", "07 - July", "08 - August",
            "09 - September", "10 - October", "11 - November", "12 - December"
        )

        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        monthSpinner.adapter = monthAdapter
        monthAdapter.notifyDataSetChanged()

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear downTo (currentYear - 10)).map { it.toString() }

        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        yearSpinner.adapter = yearAdapter
        yearAdapter.notifyDataSetChanged()
    }

    private fun fetchMonthlyHistoryData(selectedMonth: String, selectedYear: String, callback: (List<History>) -> Unit) {
        Log.d("REPORT_DEBUG", "Fetching history for Month: $selectedMonth, Year: $selectedYear")

        historyRef.get().addOnSuccessListener { snapshot ->
            val historyList = mutableListOf<History>()

            for (doc in snapshot.children) {
                val history = doc.getValue(History::class.java) ?: continue
                if (history.date.isNullOrEmpty()) continue

                Log.d("REPORT_DEBUG", "Fetched history: $history")

                val parts = history.date.split("/")
                if (parts.size != 3) continue

                val month = parts[0] // Extract month (MM)
                val year = "20" + parts[2] // Convert YY to YYYY

                if (month == selectedMonth && year == selectedYear) {
                    historyList.add(history)
                }
            }

            Log.d("REPORT_DEBUG", "Total history records found: ${historyList.size}")
            callback(historyList)

        }.addOnFailureListener {
            Log.e("REPORT_DEBUG", "Failed to fetch history: ${it.message}")
        }
    }

    private fun generateMonthlyReport(context: Context, selectedMonth: String, selectedYear: String) {
        Log.d("REPORT_DEBUG", "generateMonthlyReport called for Month: $selectedMonth, Year: $selectedYear")

        databaseHelper.fetchItems { itemList ->
            fetchMonthlyHistoryData(selectedMonth, selectedYear) { historyList ->
                if (itemList.isEmpty()) {
                    Log.d("REPORT_DEBUG", "No items found, aborting PDF generation.")
                    Toast.makeText(context, "No items available", Toast.LENGTH_SHORT).show()
                    return@fetchMonthlyHistoryData
                }

                val stockSummary = mutableMapOf<String, Pair<Int, Int>>()

                for (record in historyList) {
                    val itemName = extractItemName(record.action) ?: continue
                    val stockChange = extractStockChange(record.itemDetails)

                    if (record.action.startsWith("Consumed Stock")) {
                        val (consumed, restocked) = stockSummary.getOrDefault(itemName, Pair(0, 0))
                        stockSummary[itemName] = Pair(consumed + stockChange.absoluteValue, restocked)
                    } else if (record.action.startsWith("Restocked Item")) {
                        val (consumed, restocked) = stockSummary.getOrDefault(itemName, Pair(0, 0))
                        stockSummary[itemName] = Pair(consumed, restocked + stockChange.absoluteValue)
                    }
                }

                val assetManager = context.assets
                val templateFile = File(context.cacheDir, "report_template.pdf")
                context.assets.open("report_template.pdf").use { input ->
                    FileOutputStream(templateFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                var file = File(downloadsDir, "Monthly_Report_$selectedMonth-$selectedYear.pdf")
                var fileIndex = 1

                while (file.exists()) {
                    file = File(downloadsDir, "Monthly_Report_${selectedMonth}-${selectedYear}_($fileIndex).pdf")
                    fileIndex++
                }

                Log.d("REPORT_DEBUG", "Saving PDF to: ${file.absolutePath}")

                try {
                    val pdfReader = PdfReader(templateFile.absolutePath)
                    val pdfWriter = PdfWriter(FileOutputStream(file))
                    val pdfDocument = ITextPdfDocument(pdfReader, pdfWriter)
                    val document = Document(pdfDocument)

                    val page = pdfDocument.getFirstPage()
                    val pdfCanvas = com.itextpdf.kernel.pdf.canvas.PdfCanvas(page)
                    val font = com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD)

                    val monthNames = mapOf(
                        "01" to "January", "02" to "February", "03" to "March", "04" to "April",
                        "05" to "May", "06" to "June", "07" to "July", "08" to "August",
                        "09" to "September", "10" to "October", "11" to "November", "12" to "December"
                    )

                    val monthName = monthNames[selectedMonth] ?: "Unknown"

                    pdfCanvas.beginText()
                        .setFontAndSize(font, 14f)
                        .moveText(50.0, 630.0)
                        .showText("Monthly Inventory Report")
                        .endText()

                    pdfCanvas.beginText()
                        .setFontAndSize(font, 12f)
                        .moveText(50.0, 610.0)
                        .showText("Report Date: $monthName $selectedYear")
                        .endText()

                    pdfCanvas.release()

                    val table = Table(floatArrayOf(2f, 2f, 2f, 2f, 2f, 2f))
                    table.addHeaderCell("Item Name")
                    table.addHeaderCell("Initial Stocks")
                    table.addHeaderCell("Units Sold")
                    table.addHeaderCell("Stocks Left")
                    table.addHeaderCell("Forecast Need")
                    table.addHeaderCell("Units to Order")

                    for (item in itemList) {
                        val itemName = item.itemName
                        val stocksLeft = item.stocksLeft
                        val (consumed, restocked) = stockSummary.getOrDefault(itemName, Pair(0, 0))
                        val initialStocks = stocksLeft + consumed + restocked
                        val forecastNeed = (consumed * 2).toInt()
                        val unitsToOrder = maxOf(0, forecastNeed - stocksLeft)

                        table.addCell(itemName)
                        table.addCell(initialStocks.toString())
                        table.addCell(consumed.toString())
                        table.addCell(stocksLeft.toString())
                        table.addCell(forecastNeed.toString())
                        table.addCell(unitsToOrder.toString())
                    }

                    table.setFixedPosition(1, 50f, 200f, pdfDocument.defaultPageSize.width - 100f)

                    document.add(table)
                    document.close()

                    Log.d("REPORT_DEBUG", "PDF successfully created at: ${file.absolutePath}")
                    Toast.makeText(context, "PDF Successfully Generated", Toast.LENGTH_SHORT).show()

                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    context.startActivity(intent)

                } catch (e: Exception) {
                    Log.e("REPORT_DEBUG", "Error generating PDF: ${e.message}")
                    Toast.makeText(context, "Error generating report", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    private fun extractItemName(action: String?): String? {
        if (action == null) return null
        val regex = Regex("\\[(.*?)\\]")
        return regex.find(action)?.groups?.get(1)?.value
    }

    private fun extractStockChange(itemDetails: String?): Int {
        if (itemDetails.isNullOrEmpty()) return 0

        val regex = Regex("Stocks Left changed from \\[(\\d+)] to \\[(\\d+)]")
        val match = regex.find(itemDetails)

        return if (match != null) {
            val (oldStock, newStock) = match.destructured
            val difference = newStock.toInt() - oldStock.toInt()

            difference
        } else {
            0
        }
    }

}
