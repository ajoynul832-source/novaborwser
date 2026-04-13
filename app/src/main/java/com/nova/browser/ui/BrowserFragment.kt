package com.nova.browser.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.Fragment
import com.nova.browser.tabs.BrowserTab
import com.nova.browser.tabs.TabManager
import com.nova.browser.webview.WebViewManager

/**
 * BrowserFragment hosts the WebView for one tab.
 *
 * Lifecycle note: we do NOT inflate a layout XML here.
 * Instead we create the WebView programmatically so it can be
 * reused across orientation changes without being destroyed.
 *
 * The Fragment is created once per tab and kept alive in the
 * FragmentManager using the tab's unique ID as a tag.
 */
class BrowserFragment : Fragment() {

    companion object {
        private const val ARG_TAB_ID = "tab_id"

        fun newInstance(tabId: Long): BrowserFragment {
            return BrowserFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_TAB_ID, tabId)
                }
            }
        }
    }

    private var tabId: Long = 0L
    private var webView: WebView? = null

    // Callback to MainActivity — fired on URL / progress changes
    var onUrlChanged: ((String) -> Unit)? = null
    var onProgressChanged: ((Int) -> Unit)? = null
    var onTitleChanged: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tabId = arguments?.getLong(ARG_TAB_ID) ?: 0L
        // Keep fragment alive across config changes
        retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Re-use existing WebView if we have one (rotation / tab switch)
        if (webView == null) {
            val tab = getTab() ?: BrowserTab()
            webView = WebViewManager.buildWebView(
                context = requireContext(),
                tab = tab,
                onProgress = { progress ->
                    onProgressChanged?.invoke(progress)
                },
                onTitleChanged = { title ->
                    onTitleChanged?.invoke(title)
                },
                onUrlChanged = { url ->
                    onUrlChanged?.invoke(url)
                }
            )
            // Attach the created WebView back to the tab model
            tab.webView = webView
        }

        // Remove from previous parent if any (required when reattaching)
        (webView?.parent as? ViewGroup)?.removeView(webView)
        return webView!!
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Do NOT destroy webView here — we reuse it on re-attach
    }

    override fun onDestroy() {
        super.onDestroy()
        webView?.destroy()
        webView = null
    }

    // ------------------------------------------------------------------
    // Navigation helpers — called from MainActivity
    // ------------------------------------------------------------------

    fun loadUrl(url: String) {
        webView?.loadUrl(url)
    }

    fun canGoBack(): Boolean = webView?.canGoBack() == true

    fun canGoForward(): Boolean = webView?.canGoForward() == true

    fun goBack() { webView?.goBack() }

    fun goForward() { webView?.goForward() }

    fun reload() { webView?.reload() }

    fun stopLoading() { webView?.stopLoading() }

    fun getCurrentUrl(): String = webView?.url ?: ""

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private fun getTab(): BrowserTab? =
        TabManager.getAll().firstOrNull { it.id == tabId }
}
