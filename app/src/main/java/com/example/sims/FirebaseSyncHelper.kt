package com.example.sims

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FirebaseSyncHelper {
    private val firestore = FirebaseFirestore.getInstance()

    fun syncUsersToRoom(userDao: UserDao) {
        firestore.collection("users").get()
            .addOnSuccessListener { snapshot ->
                CoroutineScope(Dispatchers.IO).launch {
                    val users = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
                    val localUsers = users.map { firebaseUser ->
                        LocalUser(
                            username = firebaseUser.username,
                            password = firebaseUser.password,
                            name = firebaseUser.name,
                            role = firebaseUser.role,
                            enabled = firebaseUser.enabled
                        )
                    }
                    userDao.insertAll(localUsers)
                    Log.d("FirebaseSync", "Users synced to Room!")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseSync", "Failed to fetch users", e)
            }
    }

    fun syncItemsToRoom(itemDao: ItemDao) {
        firestore.collection("items").get()
            .addOnSuccessListener { snapshot ->
                CoroutineScope(Dispatchers.IO).launch {
                    val items = snapshot.documents.mapNotNull { it.toObject(Item::class.java) }
                    val localItems = items.map { firebaseItem ->
                        LocalItem(
                            itemCode = firebaseItem.itemCode,
                            itemName = firebaseItem.itemName,
                            itemCategory = firebaseItem.itemCategory,
                            itemWeight = firebaseItem.itemWeight,
                            rackNo = firebaseItem.rackNo,
                            location = firebaseItem.location,
                            supplier = firebaseItem.supplier,
                            stocksLeft = firebaseItem.stocksLeft,
                            dateAdded = firebaseItem.dateAdded,
                            lastRestocked = firebaseItem.lastRestocked,
                            enabled = firebaseItem.enabled,
                            imageUrl = firebaseItem.imageUrl
                        )
                    }
                    itemDao.insertAll(localItems)
                    Log.d("FirebaseSync", "Items synced to Room!")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseSync", "Failed to fetch items", e)
            }
    }

    fun syncHistoryToRoom(historyDao: HistoryDao) {
        firestore.collection("history").get()
            .addOnSuccessListener { snapshot ->
                CoroutineScope(Dispatchers.IO).launch {
                    val historyList = snapshot.documents.mapNotNull { it.toObject(History::class.java) }
                    val localHistory = historyList.map { firebaseHistory ->
                        LocalHistory(
                            id = 0,
                            date = firebaseHistory.date,
                            name = firebaseHistory.name,
                            action = firebaseHistory.action,
                            itemCode = firebaseHistory.itemCode,
                            itemName = firebaseHistory.itemName,
                            itemCategory = firebaseHistory.itemCategory,
                            itemWeight = firebaseHistory.itemWeight,
                            rackNo = firebaseHistory.rackNo,
                            location = firebaseHistory.location,
                            supplier = firebaseHistory.supplier,
                            stocksLeft = firebaseHistory.stocksLeft,
                            dateAdded = firebaseHistory.dateAdded,
                            lastRestocked = firebaseHistory.lastRestocked,
                            enabled = firebaseHistory.enabled,
                            imageUrl = firebaseHistory.imageUrl,
                            itemDetails = firebaseHistory.itemDetails,
                            userName = firebaseHistory.userName,
                            userUsername = firebaseHistory.userUsername,
                            userRole = firebaseHistory.userRole
                        )
                    }
                    historyDao.insertAll(localHistory)
                    Log.d("FirebaseSync", "History synced to Room!")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseSync", "Failed to fetch history", e)
            }
    }

    fun syncNotificationsToRoom(notificationDao: NotificationDao) {
        firestore.collection("notifications").get()
            .addOnSuccessListener { snapshot ->
                CoroutineScope(Dispatchers.IO).launch {
                    val notifications = snapshot.documents.mapNotNull { it.toObject(Notification::class.java) }
                    val localNotifications = notifications.map { firebaseNotification ->
                        LocalNotification(
                            itemCode = firebaseNotification.itemCode,
                            date = firebaseNotification.date,
                            icon = firebaseNotification.icon,
                            details = firebaseNotification.details,
                            enabled = firebaseNotification.enabled
                        )
                    }
                    notificationDao.insertAll(localNotifications)
                    Log.d("FirebaseSync", "Notifications synced to Room!")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseSync", "Failed to fetch notifications", e)
            }
    }
}
