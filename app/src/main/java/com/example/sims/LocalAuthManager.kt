package com.example.sims.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.sims.PasswordUtils

object LocalAuthManager {
    private const val PREF_NAME = "LocalAuth"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD_HASH = "password_hash"

    fun saveUserCredentials(context: Context, username: String, passwordHash: String) {
        val sharedPref: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            putString(KEY_USERNAME, username)
            putString(KEY_PASSWORD_HASH, passwordHash)
            apply()
        }
    }

    fun getStoredUsername(context: Context): String? {
        val sharedPref: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPref.getString(KEY_USERNAME, null)
    }

    fun verifyStoredPassword(context: Context, enteredPassword: String): Boolean {
        val sharedPref: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val storedPasswordHash = sharedPref.getString(KEY_PASSWORD_HASH, null) ?: return false
        return PasswordUtils.verifyPassword(enteredPassword, storedPasswordHash)
    }
}
