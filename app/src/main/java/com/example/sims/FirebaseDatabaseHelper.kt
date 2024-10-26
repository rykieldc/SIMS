package com.example.sims

import android.util.Log
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import com.google.firebase.Firebase

// Reference to Firebase Database "users" node
private val usersRef: DatabaseReference = Firebase.database.reference.child("users")

data class User(
    val username: String = "",
    val password: String = "",
    val name: String = "",
    val role: String = ""
)

class FirebaseDatabaseHelper {

    // Adds a new user to Firebase Realtime Database
    fun addUser(username: String, password: String, name: String, role: String, callback: (Boolean) -> Unit) {
        val user = User(username, password, name, role)
        usersRef.child(username).setValue(user)
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener {
                Log.e("FirebaseDatabaseHelper", "Error adding user: ${it.message}")
                callback(false)
            }
    }

    // Checks if a user exists with a specified username and password
    fun checkUser(username: String, password: String, callback: (Boolean) -> Unit) {
        usersRef.child(username).get().addOnSuccessListener { snapshot ->
            val user = snapshot.getValue<User>()
            callback(user?.password == password)
        }.addOnFailureListener {
            Log.e("FirebaseDatabaseHelper", "Error checking user: ${it.message}")
            callback(false)
        }
    }

    // Retrieves the role and name of a user by username
    fun checkUserData(userKey: String, callback: (User) -> Unit) {
        usersRef.child(userKey).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: "Unknown"
                    val role = snapshot.child("role").getValue(String::class.java) ?: "Unknown"
                    val username = snapshot.child("username").getValue(String::class.java) ?: "Unknown"
                    val user = User(name = name, username = username, role = role)
                    callback(user)
                } else {
                    Log.e("FirebaseDatabaseHelper", "User data does not exist for userKey: $userKey")
                    callback(User()) // Return an empty User object if data doesn't exist
                }
            }
            .addOnFailureListener {
                Log.e("FirebaseDatabaseHelper", "Error fetching user data: ${it.message}")
                callback(User()) // Return an empty User object on failure
            }
    }
}