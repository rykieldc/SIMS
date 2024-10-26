package com.example.sims

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import com.google.firebase.Firebase

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