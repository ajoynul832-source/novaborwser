package com.nova.browser.webview

import android.content.Context
import android.graphics.Color
import android.net.http.SslError
import android.os.Build
import android.webkit.*
import com.nova.browser.settings.BrowserSettings
import com.nova.browser.tabs.BrowserTab
import com.nova.browser.tabs.TabManager

/**
 * WebViewManager builds and configures a WebView for each tab.
 * Keeps all WebView logic in one place so MainActivity stays clean.
 */
object WebViewManager {

    /**
     * Creates a fully configured WebView for the given tab.
     * Attaches a WebViewClient (navigation) and WebChromeClient (progress/title).
     *
     * @param context   Activity context (required for WebView constructor)
     * @param tab       The BrowserTab this WebView belongs to
     * @param onProgress  Called with progress 0-100 while a page loads
     * @param onTitleChanged  Called when the page title changes
     * @param onUrlChanged    Called when navigation commits to a new URL
     */
    fun buildWebView(
        context: Context,
        tab: BrowserTab,
        onProgress: (Int) -> Unit,
        onTitleChanged: (String) -> Unit,
        onUrlChanged: (String) -> Unit
    ): WebView {
        val webView = WebView(context).apply {
            setBackgroundColor(Color.parseColor("#0A0A0A"))
            isScrollbarFadingEnabled = true
            isVerticalScrollBarEnabled = false

            // -------------------------------------------------------
            // WebView settings — performance & compatibility
            // -------------------------------------------------------
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                setSupportMultipleWindows(false)
                allowFileAccess = true
                allowContentAccess = true
                loadsImagesAutomatically = true
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                cacheMode = WebSettings.LOAD_DEFAULT

                // Smooth scrolling
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                // Force dark mode on Android 10+ if device is in dark mode
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    forceDark = WebSettings.FORCE_DARK_AUTO
                }

                // Desktop / Mobile user-agent controlled by setting
                userAgentString = if (BrowserSettings.desktopMode) {
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                } else {
                    null // Use system default mobile UA
                }
            }

            // Incognito: disable all caching and storage
            if (tab.isIncognito) {
                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                CookieManager.getInstance().setAcceptCookie(false)
                settings.domStorageEnabled = false
                settings.databaseEnabled = false
            }
        }

        // -------------------------------------------------------
        // WebViewClient — handle navigation events & ad blocking
        // -------------------------------------------------------
        webView.webViewClient = object : WebViewClient() {

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val url = request.url.toString()

                // Block ads
                if (AdBlocker.shouldBlock(url)) {
                    // Return an empty response instead of loading the resource
                    return WebResourceResponse("text/plain", "utf-8", null)
                }

                // HTTPS upgrade — redirect http → https
                if (BrowserSettings.upgradeHttps && url.startsWith("http://")) {
                    val httpsUrl = url.replaceFirst("http://", "https://")
                    view.post { view.loadUrl(httpsUrl) }
                    return WebResourceResponse("text/plain", "utf-8", null)
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                tab.url = url
                tab.isLoading = true
                onUrlChanged(url)
                TabManager.notifyChanged()
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                tab.url = url
                tab.isLoading = false

                // Data saver: hide images and pause videos via JS
                if (BrowserSettings.dataSaver) {
                    view.evaluateJavascript(
                        """
                        document.querySelectorAll('img').forEach(i => i.style.display='none');
                        document.querySelectorAll('video').forEach(v => { v.pause(); v.style.display='none'; });
                        """.trimIndent(), null
                    )
                }

                TabManager.notifyChanged()
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                // Don't proceed on SSL errors — safer than super call
                handler.cancel()
            }

            /** Open all links inside the app instead of launching external browser */
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                // Let http/https load normally inside WebView
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false
                }
                // tel:, mailto:, intent: etc. — let system handle
                return true
            }
        }

        // -------------------------------------------------------
        // WebChromeClient — progress bar and title updates
        // -------------------------------------------------------
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                tab.progress = newProgress
                onProgress(newProgress)
            }

            override fun onReceivedTitle(view: WebView, title: String) {
                tab.title = title.ifBlank { formatUrlForDisplay(tab.url) }
                onTitleChanged(tab.title)
                TabManager.notifyChanged()
            }
        }

        return webView
    }

    // ------------------------------------------------------------------
    // URL helpers
    // ------------------------------------------------------------------

    /**
     * Converts raw user input into a loadable URL.
     * - Already a URL  →  ensure https:// prefix
     * - Looks like a domain (contains ".", no spaces)  →  prepend https://
     * - Everything else  →  search query
     */
    fun resolveInput(input: String, searchEngine: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        if (trimmed.contains(".") && !trimmed.contains(" ")) {
            return "https://$trimmed"
        }
        val encoded = java.net.URLEncoder.encode(trimmed, "UTF-8")
        return when (searchEngine) {
            "Google"    -> "https://www.google.com/search?q=$encoded"
            "Bing"      -> "https://www.bing.com/search?q=$encoded"
            "Brave"     -> "https://search.brave.com/search?q=$encoded"
            else        -> "https://duckduckgo.com/?q=$encoded" // DuckDuckGo default
        }
    }

    /**
     * Strips protocol and trailing slash for display in the URL bar.
     */
    fun formatUrlForDisplay(url: String): String {
        if (url.isEmpty()) return ""
        return url
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
    }
}
