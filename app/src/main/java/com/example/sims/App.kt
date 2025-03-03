package com.example.sims

import android.app.Application
import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {

    companion object {
        private var instance: App? = null

        lateinit var database: AppDatabase
            private set

        fun getContext(): Context {
            return instance!!.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getDatabase(this)

        val firestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            val userDao = database.userDao()
            val itemDao = database.itemDao()
            val historyDao = database.historyDao()
            val notificationDao = database.notificationDao()

            FirebaseSyncHelper.syncUsersToRoom(userDao)
            FirebaseSyncHelper.syncItemsToRoom(itemDao)
            FirebaseSyncHelper.syncHistoryToRoom(historyDao)
            FirebaseSyncHelper.syncNotificationsToRoom(notificationDao)
        }
    }

}
