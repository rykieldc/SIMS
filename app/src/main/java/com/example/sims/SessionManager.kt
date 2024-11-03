import android.content.Context
import android.content.SharedPreferences
import com.example.sims.App

object SessionManager {
    private const val PREF_NAME = "UserSession"
    private const val KEY_USERNAME = "username"

    private fun getPreferences(): SharedPreferences {
        return App.getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveUsername(username: String) {
        val editor = getPreferences().edit()
        editor.putString(KEY_USERNAME, username)
        editor.apply()
    }

    fun getUsername(): String? {
        return getPreferences().getString(KEY_USERNAME, null)
    }

    fun clearUsername() {
        val editor = getPreferences().edit()
        editor.remove(KEY_USERNAME)
        editor.apply()
    }
}
