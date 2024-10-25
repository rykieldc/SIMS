package com.example.sims

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

private const val DATABASE_NAME = "Users.db"
private const val DATABASE_VERSION = 2
private const val TABLE_USERS = "users"
private const val COLUMN_USERNAME = "username"
private const val COLUMN_PASSWORD = "password"

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE $TABLE_USERS ("
                + "$COLUMN_USERNAME TEXT PRIMARY KEY,"
                + "$COLUMN_PASSWORD TEXT)")
        db.execSQL(createTable)


        val initialUsers = listOf(
            Pair("user", "user_password"),
            Pair("admin", "admin_password")

        )


        for (user in initialUsers) {
            val values = ContentValues().apply {
                put(COLUMN_USERNAME, user.first)
                put(COLUMN_PASSWORD, user.second)
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
}
