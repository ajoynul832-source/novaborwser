package com.nova.browser.tabs

import android.webkit.WebView

/**
 * Represents a single browser tab.
 *
 * @param id       Unique identifier (timestamp-based)
 * @param url      Current URL (empty = New Tab / homepage)
 * @param title    Page title shown in the tab switcher
 * @param isIncognito  Private browsing mode — no history saved
 * @param webView  Lazily attached WebView instance
 */
data class BrowserTab(
    val id: Long = System.currentTimeMillis(),
    var url: String = "",
    var title: String = "New Tab",
    val isIncognito: Boolean = false,
    var webView: WebView? = null,
    var isLoading: Boolean = false,
    var progress: Int = 0
)
