package com.example.sims

import SessionManager
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val usersRef: DatabaseReference = Firebase.database.reference.child("users")
private val historyRef: DatabaseReference = Firebase.database.reference.child("history")
private val itemsRef: DatabaseReference = Firebase.database.reference.child("items")
private val notificationsRef: DatabaseReference = Firebase.database.reference.child("notifications")
private val databaseReference = FirebaseDatabase.getInstance().getReference("items")
private val db = FirebaseDatabase.getInstance().getReference("users")

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


data class Item(
    var itemCode: String = "",
    var itemName: String = "",
    var itemCategory: String = "",
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

    fun addUser(username: String, password: String, name: String, role: String, callback: (Boolean) -> Unit) {
        usersRef.child(username).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val existingUser = snapshot.getValue(User::class.java)
                    if (existingUser != null && !existingUser.enabled) {
                        callback(false)
                    } else {
                        val user = User(username, password, name, role, enabled = true)
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
                        if (user.password == password) {
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
                    if (user.password == currentPassword) {
                        usersRef.child(username).child("password").setValue(newPassword)
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

                val reversedHistoryList = historyList.asReversed()

                callback(reversedHistoryList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FetchHistory", "Failed to fetch history: ${error.message}")
                callback(emptyList())
            }
        })
    }

    fun fetchNotifications(callback: (List<Notification>) -> Unit) {
        notificationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notificationsList = mutableListOf<Notification>()
                for (notificationSnapshot in snapshot.children) {
                    val notification = notificationSnapshot.getValue(Notification::class.java)
                    if (notification != null && notification.enabled) {
                        notificationsList.add(notification)
                    } else {
                        Log.e("FetchNotifications", "Notification is null or not enabled: $notificationSnapshot")
                    }
                }

                val reversedNotificationsList = notificationsList.asReversed()

                callback(reversedNotificationsList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FetchNotifications", "Failed to fetch notifications: ${error.message}")
                callback(emptyList())
            }
        })
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
                        "Consumed Stock of Item [${item.itemName}]"
                    }
                    itemDetails.append("Stocks Left changed from [${existingItem.stocksLeft}] to [${item.stocksLeft}]. ")
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
            else -> "OTH"
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

                        if (stocksLeft < 20) {
                            val notification = Notification(
                                itemCode = itemCode,
                                date = currentDate,
                                icon = "critical",
                                details = "Stocks for [${item.itemName}] are critically low. Stocks Left: [$stocksLeft]",
                                enabled = true
                            )
                            notificationRef.setValue(notification)
                        } else if (stocksLeft < 50) {
                            val notification = Notification(
                                itemCode = itemCode,
                                date = currentDate,
                                icon = "low",
                                details = "Stocks for [${item.itemName}] are low. Stocks Left: [$stocksLeft]",
                                enabled = true
                            )
                            notificationRef.setValue(notification)
                        } else {
                            //notificationRef.removeValue()
                            notificationRef.get().addOnSuccessListener { notificationSnapshot ->
                                if (notificationSnapshot.exists()) {
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
}