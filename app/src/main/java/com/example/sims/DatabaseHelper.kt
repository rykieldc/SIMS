package com.example.sims

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

private const val DATABASE_NAME = "Users.db"
private const val DATABASE_VERSION = 3
private const val TABLE_USERS = "users"
private const val COLUMN_USERNAME = "username"
private const val COLUMN_PASSWORD = "password"
private const val COLUMN_NAME = "name"
private const val COLUMN_ROLE = "role"

data class User(
    val username: String,
    val password: String,
    val name: String,
    val role: String
)

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE $TABLE_USERS ("
                + "$COLUMN_USERNAME TEXT PRIMARY KEY,"
                + "$COLUMN_PASSWORD TEXT,"
                + "$COLUMN_NAME TEXT,"
                + "$COLUMN_ROLE TEXT)")

        db.execSQL(createTable)

        // Insert initial users
        val initialUsers = listOf(
            User("user", "user_password", "User Name", "User"),
            User("admin", "admin_password", "Admin Name", "Admin")
        )

        for (user in initialUsers) {
            val values = ContentValues().apply {
                put(COLUMN_USERNAME, user.username)
                put(COLUMN_PASSWORD, user.password)
                put(COLUMN_NAME, user.name)
                put(COLUMN_ROLE, user.role)
            }
            db.insert(TABLE_USERS, null, values)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    fun checkUser(username: String, password: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?"
        val cursor = db.rawQuery(query, arrayOf(username, password))

        Log.d("DatabaseHelper", "Query executed: $query with Username: $username, Password: $password")

        val userExists = cursor.count > 0
        cursor.close()
        db.close()
        return userExists
    }

    fun checkRole(username: String): String {
        val db = readableDatabase
        val query = "SELECT $COLUMN_ROLE FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ?"
        val cursor = db.rawQuery(query, arrayOf(username))
        var role = ""

        if (cursor.moveToFirst()) {
            // Get the column index for COLUMN_ROLE
            val roleIndex = cursor.getColumnIndex(COLUMN_ROLE)
            if (roleIndex != -1) {
                role = cursor.getString(roleIndex) ?: ""
            } else {
                Log.d("checkRole", "Column index for role not found.")
            }
        } else {
            Log.d("checkRole", "No user found with username: $username")
        }

        cursor.close()
        db.close()
        return role
    }

    fun addUser(username: String, password: String, name: String, role: String): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_USERNAME, username)
            put(COLUMN_PASSWORD, password)
            put(COLUMN_NAME, name)
            put(COLUMN_ROLE, role)
        }
        val result = db.insert(TABLE_USERS, null, contentValues)
        db.close()
        return result != -1L
    }
}
