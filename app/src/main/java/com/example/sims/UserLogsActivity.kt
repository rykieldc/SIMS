package com.example.sims

import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.view.View
import android.widget.SearchView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserLogsActivity : AppCompatActivity() {

    private lateinit var header: TextView
    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewUserLogsAdapter: RecyclerViewUserLogsAdapter
    private var userLogList = mutableListOf<LocalHistory>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_logs)

        header = findViewById(R.id.header)
        header.highlightColor = ContextCompat.getColor(this, android.R.color.transparent)

        setupHeader()
        initializeRecyclerView()
        setupSearchView()
        adjustWindowInsets()

        fetchHistoryFromDatabase()
    }

    private fun setupHeader() {
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
    }

    private fun initializeRecyclerView() {
        recyclerView = findViewById(R.id.rvViewUserLogs)
        recyclerViewUserLogsAdapter = RecyclerViewUserLogsAdapter(userLogList, this)

        recyclerView.layoutManager = GridLayoutManager(this, 1)
        recyclerView.adapter = recyclerViewUserLogsAdapter
    }

    private fun setupSearchView() {
        searchView = findViewById(R.id.searchProduct)
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty()) {
                    recyclerViewUserLogsAdapter.resetList()
                } else {
                    recyclerViewUserLogsAdapter.filter(newText)
                }
                return true
            }
        })
    }

    private fun adjustWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        fetchHistoryFromDatabase()
        searchView.setQuery("", false)
        searchView.clearFocus()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fetchHistoryFromDatabase() {
        val historyDao = App.database.historyDao() // Get Room database DAO

        if (isInternetAvailable()) {
            // If internet is available, fetch from Firestore and sync to Room
            val databaseHelper = FirebaseDatabaseHelper()
            databaseHelper.fetchHistory { logsList ->
                userLogList.clear()
                userLogList.addAll(logsList.map { log ->
                    LocalHistory(
                        date = log.date,
                        name = log.name,
                        action = log.action,
                        itemCode = log.itemCode,
                        itemName = log.itemName,
                        itemCategory = log.itemCategory,
                        itemWeight = log.itemWeight,
                        rackNo = log.rackNo,
                        location = log.location,
                        supplier = log.supplier,
                        stocksLeft = log.stocksLeft,
                        dateAdded = log.dateAdded,
                        lastRestocked = log.lastRestocked,
                        enabled = log.enabled,
                        imageUrl = log.imageUrl,
                        itemDetails = log.itemDetails,
                        userName = log.userName,
                        userUsername = log.userUsername,
                        userRole = log.userRole
                    )
                })

                recyclerViewUserLogsAdapter.apply {
                    originalList.clear()
                    originalList.addAll(userLogList)
                    notifyDataSetChanged()
                }

                // Save fetched data into Room database
                lifecycleScope.launch(Dispatchers.IO) {
                    historyDao.clearHistory()
                    historyDao.insertAll(userLogList)
                }
            }
        } else {
            // No internet, fetch from Room
            lifecycleScope.launch(Dispatchers.IO) {
                val localHistory = historyDao.getAllHistory()
                withContext(Dispatchers.Main) {
                    userLogList.clear()
                    userLogList.addAll(localHistory)

                    recyclerViewUserLogsAdapter.apply {
                        originalList.clear()
                        originalList.addAll(userLogList)
                        notifyDataSetChanged()
                    }
                }
            }
        }
    }


    class DrawableClickSpan(private val clickListener: () -> Unit) : ClickableSpan() {
        override fun onClick(widget: View) {
            clickListener()
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        return connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true
    }

}
