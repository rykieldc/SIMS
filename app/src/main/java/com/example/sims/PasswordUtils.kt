package com.example.sims

import org.mindrot.jbcrypt.BCrypt

object PasswordUtils {
    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    fun verifyPassword(enteredPassword: String, storedHash: String): Boolean {
        return BCrypt.checkpw(enteredPassword, storedHash)
    }
}
