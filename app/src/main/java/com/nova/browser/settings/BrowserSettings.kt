package com.nova.browser.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * BrowserSettings wraps SharedPreferences so the rest of the app
 * reads/writes typed properties instead of raw pref keys.
 *
 * Call BrowserSettings.init(context) once from Application.onCreate()
 * or MainActivity.onCreate() before using any properties.
 */
object BrowserSettings {

    private const val PREF_FILE = "nova_prefs"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
    }

    // ------------------------------------------------------------------
    // Privacy
    // ------------------------------------------------------------------
    var adBlockEnabled: Boolean
        get() = prefs.getBoolean("ad_block", true)
        set(v) = prefs.edit().putBoolean("ad_block", v).apply()

    var dataSaver: Boolean
        get() = prefs.getBoolean("data_saver", false)
        set(v) = prefs.edit().putBoolean("data_saver", v).apply()

    var upgradeHttps: Boolean
        get() = prefs.getBoolean("upgrade_https", true)
        set(v) = prefs.edit().putBoolean("upgrade_https", v).apply()

    var clearDataOnExit: Boolean
        get() = prefs.getBoolean("clear_on_exit", false)
        set(v) = prefs.edit().putBoolean("clear_on_exit", v).apply()

    // ------------------------------------------------------------------
    // Appearance
    // ------------------------------------------------------------------
    var desktopMode: Boolean
        get() = prefs.getBoolean("desktop_mode", false)
        set(v) = prefs.edit().putBoolean("desktop_mode", v).apply()

    var toolbarPosition: String      // "top" | "bottom"
        get() = prefs.getString("toolbar_position", "bottom") ?: "bottom"
        set(v) = prefs.edit().putString("toolbar_position", v).apply()

    // ------------------------------------------------------------------
    // Search
    // ------------------------------------------------------------------
    var searchEngine: String         // "DuckDuckGo" | "Google" | "Bing" | "Brave"
        get() = prefs.getString("search_engine", "DuckDuckGo") ?: "DuckDuckGo"
        set(v) = prefs.edit().putString("search_engine", v).apply()

    // ------------------------------------------------------------------
    // Clear data utility
    // ------------------------------------------------------------------
    fun clearBrowsingData(context: Context) {
        android.webkit.WebStorage.getInstance().deleteAllData()
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        android.webkit.CookieManager.getInstance().flush()
        context.cacheDir.deleteRecursively()
        context.filesDir.deleteRecursively()
    }
}
