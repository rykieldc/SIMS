package com.example.sims

import SessionManager
import android.content.Context
import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mindrot.jbcrypt.BCrypt
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class User(
    val username: String = "",
    val password: String = "",
    val name: String = "",
    val role: String = "",
    val enabled: Boolean = true
)

data class History(
    val date: String = "",
    val name: String = "",
    val action: String = "",
    val itemCode: String? = null,
    val itemName: String? = null,
    val itemCategory: String? = null,
    val itemWeight: Float? = null,
    val location: String? = null,
    val supplier: String? = null,
    val stocksLeft: Int? = null,
    val dateAdded: String? = null,
    val lastRestocked: String? = null,
    val enabled: Boolean? = null,
    val imageUrl: String? = null,
    val itemDetails: String? = null,
    val userName: String? = null,
    val userUsername: String? = null,
    val userRole: String? = null
)

data class Notification(
    val itemCode: String = "",
    val date: String = "",
    val icon: String = "",
    val details: String = "",
    var enabled: Boolean = true
)

sealed class NotificationItem {
    data class DateHeader(val date: String) : NotificationItem()
    data class NotificationEntry(val notification: Notification) : NotificationItem()
}


data class Item(
    var itemCode: String = "",
    var itemName: String = "",
    var itemCategory: String = "",
    var itemWeight: Float = 0.00f,
    var location: String = "",
    var supplier: String = "",
    var stocksLeft: Int = 0,
    var dateAdded: String = "",
    var lastRestocked: String = "",
    var enabled: Boolean = true,
    var imageUrl: String = ""
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readFloat(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte(),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(itemCode)
        parcel.writeString(itemName)
        parcel.writeString(itemCategory)
        parcel.writeFloat(itemWeight)
        parcel.writeString(location)
        parcel.writeString(supplier)
        parcel.writeInt(stocksLeft)
        parcel.writeString(dateAdded)
        parcel.writeString(lastRestocked)
        parcel.writeByte(if (enabled) 1 else 0)
        parcel.writeString(imageUrl)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Item> {
        override fun createFromParcel(parcel: Parcel): Item {
            return Item(parcel)
        }

        override fun newArray(size: Int): Array<Item?> {
            return arrayOfNulls(size)
        }
    }
}

class FirebaseDatabaseHelper {

    private val usersRef: DatabaseReference = Firebase.database.reference.child("users")
    private val historyRef: DatabaseReference = Firebase.database.reference.child("history")
    private val itemsRef: DatabaseReference = Firebase.database.reference.child("items")
    private val notificationsRef: DatabaseReference = Firebase.database.reference.child("notifications")
    private val databaseReference = FirebaseDatabase.getInstance().getReference("items")
    private val db = FirebaseDatabase.getInstance().getReference("users")

    fun syncUsersToRoom(userDao: UserDao) {
        usersRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val users = snapshot.children.mapNotNull { it.getValue(User::class.java) }
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
                    Log.d("FirebaseDatabaseHelper", "Users synced to Room Database")
                }
            }
        }.addOnFailureListener {
            Log.e("FirebaseDatabaseHelper", "Failed to fetch users", it)
        }
    }
    fun syncItemsToRoom(itemDao: ItemDao) {
        itemsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                CoroutineScope(Dispatchers.IO).launch {
                    val itemsList = mutableListOf<LocalItem>()
                    for (itemSnapshot in snapshot.children) {
                        itemSnapshot.getValue(Item::class.java)?.let { item ->
                            itemsList.add(
                                LocalItem(
                                    item.itemCode, item.itemName, item.itemCategory, item.itemWeight,
                                    item.location, item.supplier, item.stocksLeft, item.dateAdded,
                                    item.lastRestocked, item.enabled, item.imageUrl
                                )
                            )
                        }
                    }
                    itemDao.insertAll(itemsList)
                    Log.d("FirebaseSync", "Items synced to Room!")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseSync", "Failed to sync items: ${error.message}")
            }
        })
    }

    fun syncHistoryToRoom(historyDao: HistoryDao) {
        historyRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                CoroutineScope(Dispatchers.IO).launch {
                    val historyList = mutableListOf<LocalHistory>()
                    for (historySnapshot in snapshot.children) {
                        historySnapshot.getValue(History::class.java)?.let { history ->
                            historyList.add(LocalHistory(0, history.date, history.name, history.action, history.itemCode,
                                history.itemName, history.itemCategory, history.itemWeight, history.location,
                                history.supplier, history.stocksLeft, history.dateAdded, history.lastRestocked,
                                history.enabled, history.imageUrl, history.itemDetails, history.userName,
                                history.userUsername, history.userRole))
                        }
                    }
                    historyDao.insertAll(historyList)
                    Log.d("FirebaseSync", "History synced to Room!")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseSync", "Failed to sync history: ${error.message}")
            }
        })
    }

    fun syncNotificationsToRoom(notificationDao: NotificationDao) {
        notificationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                CoroutineScope(Dispatchers.IO).launch {
                    val notificationList = mutableListOf<LocalNotification>()

                    for (notificationSnapshot in snapshot.children) {
                        notificationSnapshot.getValue(Notification::class.java)?.let { notification ->
                            notificationList.add(notification.toLocalNotification())
                        }
                    }

                    notificationDao.clearNotifications()
                    notificationDao.insertAll(notificationList)

                    Log.d("FirebaseSync", "Notifications synced to Room!")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseSync", "Failed to sync notifications: ${error.message}")
            }
        })
    }

    fun fetchMonthlyHistoryData(selectedMonth: String, selectedYear: String, callback: (List<History>) -> Unit) {
        historyRef.get().addOnSuccessListener { snapshot ->
            val historyList = mutableListOf<History>()

            for (doc in snapshot.children) {
                val history = doc.getValue(History::class.java) ?: continue
                val date = history.date

                val parts = date.split("/")
                if (parts.size == 3) {
                    val month = parts[0] // MM
                    val year = parts[2]  // YY

                    if (month == selectedMonth && year == selectedYear) {
                        historyList.add(history)
                    }
                }
            }
            callback(historyList)
        }.addOnFailureListener {
            Log.e("FirebaseDBHelper", "Failed to fetch history")
        }
    }

    private fun extractStockChange(itemDetails: String?): Int {
        if (itemDetails == null) return 0

        val regex = Regex("Changed from \\[(\\d+)] to \\[(\\d+)]")
        val match = regex.find(itemDetails)

        return if (match != null) {
            val (oldStock, newStock) = match.destructured
            oldStock.toInt() - newStock.toInt() // Calculate stock change
        } else {
            0
        }
    }


    fun generateMonthlyReport(context: Context, selectedMonth: String, selectedYear: String) {
        fetchMonthlyHistoryData(selectedMonth, selectedYear) { historyList ->
            if (historyList.isEmpty()) {
                Toast.makeText(context, "No history records found for this month", Toast.LENGTH_SHORT).show()
                return@fetchMonthlyHistoryData
            }

            val file = File(context.getExternalFilesDir(null), "Monthly_Report.pdf")
            val pdfWriter = PdfWriter(FileOutputStream(file))
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)

            // Add Title
            document.add(Paragraph("Monthly Inventory Report").setBold().setFontSize(18f))
            document.add(Paragraph("Report for: $selectedMonth/$selectedYear").setFontSize(14f).setBold())

            // Add History Data
            for (record in historyList) {
                val stockChange = extractStockChange(record.itemDetails)
                document.add(Paragraph("${record.date}: ${record.action} (Stock Change: $stockChange)"))
            }

            document.close()

            Toast.makeText(context, "Report Generated: ${file.absolutePath}", Toast.LENGTH_SHORT).show()

            // Open PDF
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        }
    }

    fun addUser(username: String, password: String, name: String, role: String, callback: (Boolean) -> Unit) {
        usersRef.child(username).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val existingUser = snapshot.getValue(User::class.java)
                    if (existingUser != null && !existingUser.enabled) {
                        callback(false)
                    } else {
                        val hashedPassword = if (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$")) {
                            password
                        } else {
                            PasswordUtils.hashPassword(password)
                        }
                        val user = User(username, hashedPassword, name, role, enabled = true)

                        usersRef.child(username).setValue(user)
                            .addOnSuccessListener {
                                callback(true)
                            }
                            .addOnFailureListener {
                                callback(false)
                            }
                    }
                } else {
                    val user = User(username, password, name, role, enabled = true)
                    usersRef.child(username).setValue(user)
                        .addOnSuccessListener {
                            val date = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
                            val action = "Added User [$username]"
                            recordUserHistory(date, action, user) { historySuccess ->
                                callback(historySuccess)
                            }
                        }
                        .addOnFailureListener {
                            callback(false)
                        }
                }
            }
            .addOnFailureListener {
                callback(false)
            }
    }



    fun checkUser(username: String, password: String, callback: (Boolean) -> Unit) {
        usersRef.get()
            .addOnSuccessListener { snapshot ->
                var isUserValid = false

                for (childSnapshot in snapshot.children) {
                    val user = childSnapshot.getValue(User::class.java)
                    if (user != null && user.username == username && user.enabled) {
                        if (PasswordUtils.verifyPassword(password, user.password)) {
                            SessionManager.saveUsername(user.username)
                            isUserValid = true
                            break
                        }
                    }
                }

                callback(isUserValid)
            }
            .addOnFailureListener {
                callback(false)
            }
    }


    fun checkUserData(username: String, callback: (User) -> Unit) {
        usersRef.get()
            .addOnSuccessListener { snapshot ->
                var foundUser: User? = null

                for (childSnapshot in snapshot.children) {
                    val user = childSnapshot.getValue(User::class.java)
                    if (user != null && user.username == username) {
                        foundUser = user
                        break
                    }
                }

                if (foundUser != null) {
                    callback(foundUser)
                } else {
                    callback(User())
                }
            }
            .addOnFailureListener {
                callback(User())
            }
    }

    fun changeUserPassword(username: String, currentPassword: String, newPassword: String, callback: (Boolean) -> Unit) {
        usersRef.child(username).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val user = snapshot.getValue<User>()
                if (user != null && user.enabled) {
                    if (BCrypt.checkpw(currentPassword, user.password)) {
                        val hashedNewPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt())
                        usersRef.child(username).child("password").setValue(hashedNewPassword)
                            .addOnSuccessListener {
                                callback(true)
                            }
                            .addOnFailureListener {
                                callback(false)
                            }
                    } else {
                        callback(false)
                    }
                } else {
                    callback(false)
                }
            } else {
                callback(false)
            }
        }.addOnFailureListener {
            callback(false)
        }
    }


    fun fetchUsers(callback: (List<User>) -> Unit) {
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val usersList = mutableListOf<User>()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    if (user != null && user.enabled) {
                        usersList.add(user)
                    } else {
                        Log.e("FetchUsers", "User is null or not enabled: $userSnapshot")
                    }
                }
                callback(usersList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FetchUsers", "Failed to fetch users: ${error.message}")
                callback(emptyList())
            }
        })
    }

    fun deleteUser(username: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        db.child(username).removeValue()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception ->
                onFailure(exception.message ?: "Error deleting user")
            }
    }

    fun addUser(username: String, user: User, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        db.child(username).setValue(user)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception ->
                onFailure(exception.message ?: "Error adding user")
            }
    }

    fun updateUser(userKey: String, user: User, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        db.child(userKey).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val existingUser = snapshot.getValue(User::class.java)
                    val userDetails = StringBuilder()

                    if (existingUser != null) {
                        if (existingUser.name != user.name) {
                            userDetails.append("Updated Name from [${existingUser.name}] to [${user.name}]. ")
                        }
                        if (existingUser.role != user.role) {
                            userDetails.append("Updated Role from [${existingUser.role}] to [${user.role}]. ")
                        }
                        if (existingUser.enabled != user.enabled) {
                            userDetails.append("Updated Status from [${existingUser.enabled}] to [${user.enabled}]. ")
                        }
                    }

                    db.child(userKey).setValue(user)
                        .addOnSuccessListener {
                            val date = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
                            val action = "Updated User [$userKey]"
                            recordUserHistory(date, action, user, userDetails.toString()) { historySuccess ->
                                if (historySuccess) onSuccess() else onFailure("Failed to record history")
                            }
                        }
                        .addOnFailureListener { exception ->
                            onFailure(exception.message ?: "Error updating user")
                        }
                } else {
                    onFailure("User not found")
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception.message ?: "Error retrieving user")
            }
    }


    fun setUserEnabled(username: String, isEnabled: Boolean, callback: (Boolean) -> Unit) {
        usersRef.child(username).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    usersRef.child(username).child("enabled").setValue(isEnabled)
                        .addOnSuccessListener {
                            val date = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
                            val action = if (isEnabled) "Restored User [$username]" else "Deleted User [$username]"
                            val userDetails = "User [${user.name}] with Role [${user.role}] was ${if (isEnabled) "restored" else "deleted"}."

                            recordUserHistory(date, action, user, userDetails) { historySuccess ->
                                callback(historySuccess)
                            }
                        }
                        .addOnFailureListener {
                            callback(false)
                        }
                } else {
                    callback(false)
                }
            } else {
                callback(false)
            }
        }.addOnFailureListener {
            callback(false)
        }
    }

    fun recordUserHistory(date: String, action: String, user: User? = null, userDetails: String? = null, callback: (Boolean) -> Unit) {
        val historyId = historyRef.push().key ?: ""
        val history = History(
            date = date,
            name = SessionManager.getUsername() ?: "Unknown",
            action = action,
            itemDetails = userDetails,
            userName = user?.name,
            userUsername = user?.username,
            userRole = user?.role
        )

        historyRef.child(historyId).setValue(history)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }


    private fun recordHistoryAdd(date: String, username: String, action: String, item: Item, callback: (Boolean) -> Unit) {
        val historyId = historyRef.push().key ?: ""
        val history = History(
            date = date,
            name = username,
            action = action,
            itemCode = item.itemCode,
            itemName = item.itemName,
            itemCategory = item.itemCategory,
            itemWeight = item.itemWeight,
            location = item.location,
            supplier = item.supplier,
            stocksLeft = item.stocksLeft,
            dateAdded = item.dateAdded,
            lastRestocked = item.lastRestocked,
            enabled = item.enabled,
            imageUrl = item.imageUrl
        )

        historyRef.child(historyId).setValue(history)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }


    private fun recordHistoryDelete(date: String, username: String, action: String, item: Item, callback: (Boolean) -> Unit) {
        val historyId = historyRef.push().key ?: ""
        val history = History(
            date = date,
            name = username,
            action = action,
            itemCode = item.itemCode,
            itemName = item.itemName,
            itemCategory = item.itemCategory,
            itemWeight = item.itemWeight,
            location = item.location,
            supplier = item.supplier,
            stocksLeft = item.stocksLeft,
            dateAdded = item.dateAdded,
            lastRestocked = item.lastRestocked,
            enabled = item.enabled,
            imageUrl = item.imageUrl
        )

        historyRef.child(historyId).setValue(history)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    private fun recordHistoryUpdate(date: String, username: String, action: String, itemDetails: String? = null, callback: (Boolean) -> Unit) {
        val historyId = historyRef.push().key ?: ""
        val history = History(
            date = date,
            name = username,
            action = action,
            itemDetails = itemDetails
        )

        historyRef.child(historyId).setValue(history)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun fetchHistory(callback: (List<History>) -> Unit) {
        historyRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val historyList = mutableListOf<History>()
                for (historySnapshot in snapshot.children) {
                    val history = historySnapshot.getValue(History::class.java)
                    if (history != null) {
                        historyList.add(history)
                    } else {
                        Log.e("FetchHistory", "History record is null: $historySnapshot")
                    }
                }

                // Sort by date in descending order
                val sortedHistoryList = historyList.sortedByDescending { history ->
                    parseDate(history.date)
                }

                callback(sortedHistoryList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FetchHistory", "Failed to fetch history: ${error.message}")
                callback(emptyList())
            }
        })
    }

    private fun parseDate(dateString: String): Date {
        return try {
            val format = SimpleDateFormat("MM/dd/yy", Locale.US)
            format.parse(dateString) ?: Date(0) // Default to epoch if parsing fails
        } catch (e: Exception) {
            Log.e("ParseDate", "Error parsing date: $dateString", e)
            Date(0) // Default fallback date
        }
    }


    fun fetchNotifications(callback: (List<NotificationItem>) -> Unit) {
        notificationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notificationsList = mutableListOf<LocalNotification>()
                for (notificationSnapshot in snapshot.children) {
                    val notification = notificationSnapshot.getValue(Notification::class.java)
                    if (notification != null && notification.enabled) {
                        notificationsList.add(notification.toLocalNotification())
                    } else {
                        Log.e("FetchNotifications", "Notification is null or not enabled: $notificationSnapshot")
                    }
                }

                val groupedNotifications = groupNotificationsByDate(notificationsList)

                callback(groupedNotifications)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FetchNotifications", "Failed to fetch notifications: ${error.message}")
                callback(emptyList())
            }
        })
    }



    fun groupNotificationsByDate(notifications: List<LocalNotification>): List<NotificationItem> {
        return notifications
            .sortedByDescending { it.date }
            .groupBy { it.date }
            .flatMap { (date, notifs) ->
                listOf(NotificationItem.DateHeader(date)) + notifs.map {
                    NotificationItem.NotificationEntry(it.toNotification())
                }
            }
    }

    fun fetchItems(callback: (List<Item>) -> Unit) {
        itemsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val itemsList = mutableListOf<Item>()
                for (itemSnapshot in snapshot.children) {
                    val item = itemSnapshot.getValue(Item::class.java)
                    if (item != null && item.enabled) {
                        itemsList.add(item)
                    } else {
                        Log.e("FetchItems", "Item is null or not enabled: $itemSnapshot")
                    }
                }

                val reversedItemsList = itemsList.asReversed()

                callback(reversedItemsList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FetchItems", "Failed to fetch items: ${error.message}")
                callback(emptyList())
            }
        })
    }

    fun fetchItemDetails(itemCode: String, onSuccess: (Item) -> Unit, onFailure: (String) -> Unit) {
        val dbRef = FirebaseDatabase.getInstance().getReference("items")
        dbRef.orderByChild("itemCode").equalTo(itemCode)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (child in snapshot.children) {
                            val item = child.getValue(Item::class.java)
                            if (item != null) {
                                onSuccess(item)
                                return
                            }
                        }
                    }
                    onFailure("Item not found.")
                }

                override fun onCancelled(error: DatabaseError) {
                    onFailure(error.message)
                }
            })
    }

    fun getItemByCode(itemCode: String, callback: (Item?) -> Unit) {
        val dbRef = FirebaseDatabase.getInstance().getReference("items")
        dbRef.orderByChild("itemCode").equalTo(itemCode)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val item = child.getValue(Item::class.java)
                        callback(item)
                        return
                    }
                    callback(null)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(null)
                }
            })
    }


    fun saveItem(item: Item, callback: (Boolean) -> Unit) {
        val itemCode = item.itemCode
        itemsRef.child(itemCode).setValue(item)
            .addOnSuccessListener {
                val date = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
                val action = "Added Item [${item.itemName}]"
                recordHistoryAdd(date, SessionManager.getUsername() ?: "Unknown", action, item) { historySuccess ->
                    callback(historySuccess)
                }
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    fun updateItem(productCode: String, item: Item, callback: (Boolean) -> Unit) {
        itemsRef.child(productCode).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val existingItem = snapshot.getValue(Item::class.java) ?: return@addOnSuccessListener
                var action: String? = null
                val itemDetails = StringBuilder()

                if (existingItem.itemName != item.itemName) {
                    itemDetails.append("Updated Item Name from [${existingItem.itemName}] to [${item.itemName}]. ")
                }
                if (existingItem.itemCategory != item.itemCategory) {
                    itemDetails.append("Updated Item Category from [${existingItem.itemCategory}] to [${item.itemCategory}]. ")
                }
                if (existingItem.itemWeight != item.itemWeight) {
                    itemDetails.append("Updated Item Weight from [${existingItem.itemWeight}] to [${item.itemWeight}]. ")
                }
                if (existingItem.location != item.location) {
                    itemDetails.append("Updated Location from [${existingItem.location}] to [${item.location}]. ")
                }
                if (existingItem.supplier != item.supplier) {
                    itemDetails.append("Updated Supplier from [${existingItem.supplier}] to [${item.supplier}]. ")
                }
                if (existingItem.imageUrl != item.imageUrl) {
                    itemDetails.append("Updated Image URL from [${existingItem.imageUrl}] to [${item.imageUrl}]. ")
                }

                if (existingItem.stocksLeft != item.stocksLeft) {
                    val stockDifference = item.stocksLeft - existingItem.stocksLeft
                    action = if (stockDifference > 0) {
                        "Restocked Item [${item.itemName}]"
                    } else {
                        "Changed Item [${item.itemName}]"
                    }
                    itemDetails.append("Changed from [${existingItem.stocksLeft}] to [${item.stocksLeft}]. ")
                }

                itemsRef.child(productCode).setValue(item).addOnSuccessListener {
                    val date = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
                    if (itemDetails.isNotEmpty()) {
                        recordHistoryUpdate(
                            date,
                            SessionManager.getUsername() ?: "Unknown",
                            action ?: "Updated Item [${item.itemName}]",
                            itemDetails.toString()
                        ) { historySuccess ->
                            if (historySuccess) {
                                monitorStockLevels { monitorSuccess ->
                                    callback(monitorSuccess)
                                }
                            } else {
                                callback(false)
                            }
                        }
                    } else {
                        monitorStockLevels { monitorSuccess ->
                            callback(monitorSuccess)
                        }
                    }
                }.addOnFailureListener {
                    callback(false)
                }
            } else {
                callback(false)
            }
        }.addOnFailureListener {
            callback(false)
        }
    }



    fun setItemEnabled(itemCode: String, isEnabled: Boolean, callback: (Boolean) -> Unit) {
        itemsRef.child(itemCode).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val item = snapshot.getValue(Item::class.java) ?: return@addOnSuccessListener
                itemsRef.child(itemCode).child("enabled").setValue(isEnabled)
                    .addOnSuccessListener {
                        val date = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
                        val action = if (isEnabled) "Restored Item" else "Deleted Item"

                        val notificationRef = FirebaseDatabase.getInstance()
                            .getReference("notifications")
                            .child(itemCode)

                        notificationRef.get().addOnSuccessListener { notificationSnapshot ->
                            if (notificationSnapshot.exists()) {
                                notificationRef.child("enabled").setValue(false)
                                    .addOnSuccessListener {
                                        Log.d("setItemEnabled", "Notification for $itemCode disabled successfully.")
                                    }
                                    .addOnFailureListener {
                                        Log.e("setItemEnabled", "Failed to disable notification for $itemCode: ${it.message}")
                                    }
                            }
                        }.addOnFailureListener {
                            Log.e("setItemEnabled", "Failed to fetch notification for $itemCode: ${it.message}")
                        }

                        recordHistoryDelete(date, SessionManager.getUsername() ?: "Unknown", action, item) { historySuccess ->
                            callback(historySuccess)
                        }
                    }
                    .addOnFailureListener {
                        callback(false)
                    }
            } else {
                callback(false)
            }
        }.addOnFailureListener {
            callback(false)
        }
    }



    fun doesProductNameExistExcludingCurrent(
        productName: String,
        currentProductName: String,
        callback: (Boolean) -> Unit
    ) {
        databaseReference.orderByChild("itemName").equalTo(productName).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val exists = snapshot.children.any {
                    it.child("enabled").getValue(Boolean::class.java) == true &&
                            it.child("itemName").getValue(String::class.java) != currentProductName
                }
                callback(exists)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(false)
            }
        })
    }



    fun doesProductNameExist(productName: String, callback: (Boolean) -> Unit) {
        databaseReference.orderByChild("itemName").equalTo(productName).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val exists = snapshot.children.any { it.child("enabled").getValue(Boolean::class.java) == true }
                callback(exists)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(false)
            }
        })
    }

    fun getNextProductCode(category: String, callback: (String?) -> Unit) {
        itemsRef.orderByChild("itemCategory").equalTo(category).get().addOnSuccessListener { snapshot ->

            var maxCode = 0
            var hasExistingCodes = false

            for (itemSnapshot in snapshot.children) {
                val item = itemSnapshot.getValue<Item>()
                item?.let {

                    val codeParts = it.itemCode.split("-")
                    if (codeParts.size == 2 && codeParts[0] == getCategoryPrefix(category)) {
                        hasExistingCodes = true
                        val codeNumber = codeParts[1].toIntOrNull()
                        if (codeNumber != null && codeNumber > maxCode) {
                            maxCode = codeNumber
                        }
                    }
                }
            }

            if (!hasExistingCodes) {
                Log.d("FirebaseDatabaseHelper", "No product code found for category: $category")
            }

            val nextCode = maxCode + 1
            val formattedCode = "${getCategoryPrefix(category)}-${String.format(Locale.US, "%03d", nextCode)}"

            callback(formattedCode)
        }.addOnFailureListener {
            callback(null)
        }
    }

    private fun getCategoryPrefix(category: String): String {
        return when (category) {
            "Syringes & Needles" -> "SYR"
            "Dressings & Bandages" -> "DRS"
            "Disinfectants & Antiseptics" -> "ANT"
            "Personal Protective Equipment (PPE)" -> "PPE"
            "Diagnostic Devices" -> "DGD"
            else -> "OTI"
        }
    }

    fun monitorStockLevels(callback: (Boolean) -> Unit) {
        itemsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentDate = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())

                for (itemSnapshot in snapshot.children) {
                    val item = itemSnapshot.getValue(Item::class.java)
                    if (item != null && item.enabled) {
                        val itemCode = item.itemCode
                        val stocksLeft = item.stocksLeft

                        val notificationRef = FirebaseDatabase.getInstance()
                            .getReference("notifications")
                            .child(itemCode)

                        notificationRef.get().addOnSuccessListener { notificationSnapshot ->
                            val existingNotification = notificationSnapshot.getValue(Notification::class.java)

                            val newNotification = when {
                                stocksLeft < 20 -> Notification(
                                    itemCode = itemCode,
                                    date = currentDate,
                                    icon = "critical",
                                    details = "Stocks for [${item.itemName}] are critically low. Stocks Left: [$stocksLeft]",
                                    enabled = true
                                )
                                stocksLeft < 50 -> Notification(
                                    itemCode = itemCode,
                                    date = currentDate,
                                    icon = "low",
                                    details = "Stocks for [${item.itemName}] are low. Stocks Left: [$stocksLeft]",
                                    enabled = true
                                )
                                else -> null
                            }

                            if (newNotification != null) {
                                if (existingNotification == null || existingNotification.details != newNotification.details) {
                                    notificationRef.setValue(newNotification)
                                }
                            } else {
                                if (existingNotification != null && existingNotification.enabled) {
                                    notificationRef.child("enabled").setValue(false)
                                }
                            }
                        }
                    }
                }
                callback(true)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MonitorStockLevels", "Failed to monitor stock levels: ${error.message}")
                callback(false)
            }
        })
    }


    fun listenForStockChanges() {
        itemsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("StockMonitor", "Stock levels changed, triggering monitorStockLevels()")
                monitorStockLevels { success ->
                    if (success) {
                        Log.d("StockMonitor", "Stock monitoring updated successfully")
                    } else {
                        Log.e("StockMonitor", "Failed to update stock monitoring")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("StockMonitor", "Failed to listen for stock changes: ${error.message}")
            }
        })
    }

}

