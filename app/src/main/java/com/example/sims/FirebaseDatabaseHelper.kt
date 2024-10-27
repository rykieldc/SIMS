package com.example.sims

import android.util.Log
import com.google.firebase.database.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale


data class User(
    val username: String = "",
    val password: String = "",
    val name: String = "",
    val role: String = ""
)

data class Item(
    val itemCode: String = "",
    val itemName: String = "",
    val itemCategory: String = "",
    val location: String = "",
    val supplier: String = "",
    val stocksLeft: Int = 0,
    val dateAdded: String = "",
    val lastRestocked: String = "",
    val imageUrl: String = ""
)

class FirebaseDatabaseHelper {

    private val usersRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("users")
    private val itemsRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("items")

    fun addUser(username: String, password: String, name: String, role: String, callback: (Boolean) -> Unit) {
        usersRef.child(username).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    callback(false)
                } else {
                    val user = User(username, password, name, role)
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
            callback(user?.password == password)
        }.addOnFailureListener {
            callback(false)
        }
    }

    fun getUser(username: String, callback: (User?) -> Unit) {
        usersRef.child(username).get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(User::class.java)
                callback(user)
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    fun updateUser(username: String, updatedUser: User, callback: (Boolean) -> Unit) {
        usersRef.child(username).setValue(updatedUser).addOnCompleteListener { task ->
            callback(task.isSuccessful)
        }
    }

    fun deleteUser(username: String, callback: (Boolean) -> Unit) {
        usersRef.child(username).removeValue().addOnCompleteListener { task ->
            callback(task.isSuccessful)
        }
    }

    fun fetchItems(callback: (List<Item>) -> Unit) {
        itemsRef.get().addOnSuccessListener { snapshot ->
            val itemsList = mutableListOf<Item>()
            for (itemSnapshot in snapshot.children) {
                val item = itemSnapshot.getValue<Item>()
                if (item != null) {
                    itemsList.add(item)
                }
            }
            callback(itemsList)
        }.addOnFailureListener {
            callback(emptyList())
        }
    }

    fun saveItem(item: Item, callback: (Boolean) -> Unit) {
        val itemCode = item.itemCode
        itemsRef.child(itemCode).setValue(item)
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    fun doesProductNameExist(productName: String, callback: (Boolean) -> Unit) {
        itemsRef.orderByChild("itemName").equalTo(productName).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                callback(snapshot.exists())
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
            else -> "UNK" // Unknown category
        }
    }
    fun changeUserPassword(username: String, currentPassword: String, newPassword: String, callback: (Boolean) -> Unit) {
        usersRef.child(username).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val user = snapshot.getValue<User>()

                if (user?.password == currentPassword) {
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
        }.addOnFailureListener {
            callback(false)
        }
    }
}
