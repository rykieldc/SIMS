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
            generateMonthlyReportButton.isEnabled = false

            val selectedMonth = monthSpinner.selectedItem.toString().substring(0, 2)
            val selectedYear = yearSpinner.selectedItem.toString()

            Log.d("REPORT_DEBUG", "Generating report for Month: $selectedMonth, Year: $selectedYear")

            generateMonthlyReport(this, selectedMonth, selectedYear, generateMonthlyReportButton)
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

    private fun fetchMonthlyHistoryData(
        selectedMonth: String,
        selectedYear: String,
        callback: (Map<String, Pair<Int, Int>>) -> Unit
    ) {
        Log.d("REPORT_DEBUG", "Fetching history for Month: $selectedMonth, Year: $selectedYear")

        historyRef.get().addOnSuccessListener { snapshot ->
            val stockChanges = mutableMapOf<String, Pair<Int, Int>>()

            for (doc in snapshot.children) {
                val history = doc.getValue(History::class.java) ?: continue
                if (history.date.isNullOrEmpty()) continue

                val parts = history.date.split("/")
                if (parts.size != 3) continue

                val month = parts[0]
                val year = "20" + parts[2]

                if (month == selectedMonth && year == selectedYear) {
                    val itemName = extractItemName(history.action) ?: continue
                    val stockChange = extractStockChange(history.itemDetails)

                    val (changed, restocked) = stockChanges.getOrDefault(itemName, Pair(0, 0))
                    if (history.action.startsWith("Changed")) {
                        stockChanges[itemName] = Pair(changed + stockChange.absoluteValue, restocked)
                    } else if (history.action.startsWith("Restocked")) {
                        stockChanges[itemName] = Pair(changed, restocked + stockChange.absoluteValue)
                    }
                }
            }

            callback(stockChanges)
        }.addOnFailureListener {
            Log.e("REPORT_DEBUG", "Failed to fetch history: ${it.message}")
        }
    }

    private fun fetchMultiMonthHistoryData(
        selectedMonth: String,
        selectedYear: String,
        callback: (Map<String, List<Int>>) -> Unit
    ) {
        val salesHistory = mutableMapOf<String, MutableList<Int>>()

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, selectedYear.toInt())
        calendar.set(Calendar.MONTH, selectedMonth.toInt() - 1)

        val monthsToFetch = (0..6).map {
            val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
            val year = calendar.get(Calendar.YEAR).toString()
            calendar.add(Calendar.MONTH, -1) // Move back one month
            Pair(month, year)
        }

        Log.d("REPORT_DEBUG", "Fetching sales history for the past 7 months: $monthsToFetch")

        historyRef.get().addOnSuccessListener { snapshot ->
            Log.d("REPORT_DEBUG", "Successfully fetched history data from Firebase. Processing...")

            for (doc in snapshot.children) {
                val history = doc.getValue(History::class.java) ?: continue
                if (history.date.isNullOrEmpty()) continue

                val parts = history.date.split("/")
                if (parts.size != 3) continue

                val recordMonth = parts[0]
                val recordYear = "20" + parts[2]

                if (monthsToFetch.contains(Pair(recordMonth, recordYear))) {
                    val itemName = extractItemName(history.action) ?: continue
                    val stockChange = extractStockChange(history.itemDetails)

                    if (history.action.startsWith("Changed")) {
                        salesHistory.getOrPut(itemName) { mutableListOf() }.add(stockChange.absoluteValue)

                        Log.d(
                            "REPORT_DEBUG",
                            "Processed: Item = $itemName, Stock Change = ${stockChange.absoluteValue}, Total = ${salesHistory[itemName]}"
                        )
                    }
                }
            }

            // Ensure all lists have at least 5 elements by appending last known value or 0
            salesHistory.forEach { (item, salesList) ->
                while (salesList.size < 5) {
                    salesList.add(salesList.lastOrNull() ?: 0)
                }
            }

            Log.d("REPORT_DEBUG", "Final sales history formatted for API: $salesHistory")

            callback(salesHistory)
        }.addOnFailureListener {
            Log.e("REPORT_DEBUG", "Failed to fetch history: ${it.message}")
        }
    }

    private fun generateMonthlyReport(context: Context, selectedMonth: String, selectedYear: String, button: Button) {
        Log.d("REPORT_DEBUG", "generateMonthlyReport called for Month: $selectedMonth, Year: $selectedYear")

        databaseHelper.fetchItems { itemList ->
            Log.d("REPORT_DEBUG", "Fetched ${itemList.size} items from database.")

            fetchMultiMonthHistoryData(selectedMonth, selectedYear) { historyList ->
                Log.d("REPORT_DEBUG", "Fetched history data: ${historyList.size} items.")

                if (itemList.isEmpty()) {
                    Log.d("REPORT_DEBUG", "No items found, aborting PDF generation.")
                    Toast.makeText(context, "No items available", Toast.LENGTH_SHORT).show()
                    return@fetchMultiMonthHistoryData
                }

                val salesData = historyList.mapValues { it.value }
                Log.d("REPORT_DEBUG", "Sales data mapped: $salesData")

                fetchPrediction(salesData) { predictions ->
                    if (predictions == null) {
                        Log.e("REPORT_DEBUG", "Prediction fetch returned null.")
                        Toast.makeText(context, "Failed to fetch predictions", Toast.LENGTH_SHORT).show()
                        button.isEnabled = true
                        return@fetchPrediction
                    }

                    Log.d("REPORT_DEBUG", "Predictions received: $predictions")

                    val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    var file = File(downloadsDir, "Monthly_Report_$selectedMonth-$selectedYear.pdf")
                    var fileIndex = 1

                    while (file.exists()) {
                        file = File(downloadsDir, "Monthly_Report_${selectedMonth}-${selectedYear}_($fileIndex).pdf")
                        fileIndex++
                    }

                    Log.d("REPORT_DEBUG", "Saving PDF to: ${file.absolutePath}")

                    try {
                        val assetManager = context.assets
                        val templateStream = assetManager.open("report_template.pdf")
                        val pdfReader = PdfReader(templateStream)
                        val pdfWriter = PdfWriter(FileOutputStream(file))
                        val templateDocument = ITextPdfDocument(pdfReader)
                        val newDocument = ITextPdfDocument(pdfWriter)
                        val document = Document(newDocument)

                        templateDocument.copyPagesTo(1, 1, newDocument)

                        val monthNames = mapOf(
                            "01" to "January", "02" to "February", "03" to "March", "04" to "April",
                            "05" to "May", "06" to "June", "07" to "July", "08" to "August",
                            "09" to "September", "10" to "October", "11" to "November", "12" to "December"
                        )
                        val monthName = monthNames[selectedMonth] ?: "Unknown"


                        document.add(Paragraph("\n\n\n\n\n\n\n")) // Spacing after template content
                        document.add(Paragraph("Monthly Inventory Report"))
                        document.add(Paragraph("Report Date: $monthName $selectedYear"))

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
                            val changed = historyList[itemName]?.sum() ?: 0
                            val restocked = 0
                            val initialStocks = stocksLeft + changed + restocked
                            val forecastNeed = predictions.getOrDefault(itemName, 0)
                            val unitsToOrder = maxOf(0, forecastNeed - stocksLeft)

                            Log.d("REPORT_DEBUG", "Processing item: $itemName | Initial: $initialStocks, Sold: $changed, Left: $stocksLeft, Forecast: $forecastNeed, To Order: $unitsToOrder")

                            table.addCell(itemName)
                            table.addCell(initialStocks.toString())
                            table.addCell(changed.toString())
                            table.addCell(stocksLeft.toString())
                            table.addCell(forecastNeed.toString())
                            table.addCell(unitsToOrder.toString())
                        }

                        document.add(table)

                        document.close()
                        newDocument.close()
                        templateDocument.close()
                        pdfReader.close()
                        pdfWriter.close()

                        Log.d("REPORT_DEBUG", "PDF successfully created at: ${file.absolutePath}")
                        Toast.makeText(context, "PDF Successfully Generated", Toast.LENGTH_SHORT).show()
                        button.isEnabled = true

                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        context.startActivity(intent)
                        generateMonthlyReportButton.isEnabled = true

                    } catch (e: Exception) {
                        generateMonthlyReportButton.isEnabled = true
                        Log.e("REPORT_DEBUG", "Error generating PDF: ${e.message}")
                        Toast.makeText(context, "Error generating report", Toast.LENGTH_SHORT).show()
                        button.isEnabled = true
                    }
                }
            }
        }
    }


    private fun fetchPrediction(salesData: Map<String, List<Int>>, callback: (Map<String, Int>?) -> Unit) {
        Log.d("PREDICTION_API", "Sending request with sales data: $salesData")

        val requestBody = PredictionRequest(salesData)  // Use the correct data class

        RetrofitClient.instance.getPrediction(requestBody)
            .enqueue(object : retrofit2.Callback<PredictionResponse> {
                override fun onResponse(call: retrofit2.Call<PredictionResponse>, response: retrofit2.Response<PredictionResponse>) {
                    Log.d("PREDICTION_API", "Received Response Code: ${response.code()}")

                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        Log.d("PREDICTION_API", "Response Body: $responseBody")
                        callback(responseBody?.predictions)
                    } else {
                        Log.e("PREDICTION_API", "Failed Response: ${response.errorBody()?.string()}")
                        callback(null)
                    }
                }

                override fun onFailure(call: retrofit2.Call<PredictionResponse>, t: Throwable) {
                    Log.e("PREDICTION_API", "API Call Failed: ${t.message}")
                    callback(null)
                }
            })
    }



    private fun extractItemName(action: String?): String? {
        if (action == null) return null
        val regex = Regex("\\[(.*?)\\]")
        return regex.find(action)?.groups?.get(1)?.value
    }

    private fun extractStockChange(itemDetails: String?): Int {
        if (itemDetails.isNullOrEmpty()) return 0

        val regex = Regex("Changed from \\[(\\d+)] to \\[(\\d+)]")
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
