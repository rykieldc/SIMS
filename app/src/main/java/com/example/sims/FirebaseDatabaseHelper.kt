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
    val action: String = ""
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
                            callback(true)
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
        usersRef.child(username).get().addOnSuccessListener { snapshot ->
            val user = snapshot.getValue<User>()
            if (user != null && user.enabled) {
                SessionManager.saveUsername(username)
                callback(user.password == password)
            } else {
                callback(false)
            }
        }.addOnFailureListener {
            callback(false)
        }
    }

    fun checkUserData(userKey: String, callback: (User) -> Unit) {
        usersRef.child(userKey).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: "Unknown"
                    val role = snapshot.child("role").getValue(String::class.java) ?: "Unknown"
                    val username = snapshot.child("username").getValue(String::class.java) ?: "Unknown"
                    val enabled = snapshot.child("enabled").getValue(Boolean::class.java) ?: false
                    val user = User(name = name, username = username, role = role, enabled = enabled)
                    callback(user)
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

    fun updateUser(userKey: String, user: User, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        db.child(userKey)
            .setValue(user)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception.message ?: "Error updating user")
            }
    }

    fun setUserEnabled(username: String, isEnabled: Boolean, callback: (Boolean) -> Unit) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")

        usersRef.child(username).child("enabled").setValue(isEnabled)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }


    private fun recordHistory(date: String, name: String, action: String, callback: (Boolean) -> Unit) {
        val historyId = historyRef.push().key ?: ""
        val history = History(date, name, action)

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
                callback(historyList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FetchHistory", "Failed to fetch history: ${error.message}")
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
                callback(itemsList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FetchItems", "Failed to fetch items: ${error.message}")
                callback(emptyList())
            }
        })
    }



    fun saveItem(item: Item, callback: (Boolean) -> Unit) {
        val itemCode = item.itemCode
        itemsRef.child(itemCode).setValue(item)
            .addOnSuccessListener {
                val date = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
                val action = "Added Item [${item.itemName}]"
                recordHistory(date, SessionManager.getUsername() ?: "Unknown", action) { historySuccess ->
                    if (historySuccess) {
                        callback(true)
                    } else {
                        callback(false)
                    }
                }
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    fun updateItem(productCode: String, item: Item, callback: (Boolean) -> Unit) {
        itemsRef.child(productCode).setValue(item)
            .addOnSuccessListener {
                val date = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
                val action = "Edited Item [${item.itemName}]"
                recordHistory(date, SessionManager.getUsername() ?: "Unknown", action) { historySuccess ->
                    if (historySuccess) {
                        callback(true)
                    } else {
                        callback(false)
                    }
                }
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    fun setItemEnabled(itemCode: String, isEnabled: Boolean, callback: (Boolean) -> Unit) {
        itemsRef.child(itemCode).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val itemName = snapshot.child("itemName").value?.toString() ?: "Unknown Item"
                itemsRef.child(itemCode).child("enabled").setValue(isEnabled)
                    .addOnSuccessListener {
                        val date = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
                        val action = "Deleted Item [$itemName]"
                        recordHistory(date, SessionManager.getUsername() ?: "Unknown", action) { historySuccess ->
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


}