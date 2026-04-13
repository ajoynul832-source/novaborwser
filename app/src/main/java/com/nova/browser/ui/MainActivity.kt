package com.nova.browser.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.nova.browser.R
import com.nova.browser.databinding.ActivityMainBinding
import com.nova.browser.settings.BrowserSettings
import com.nova.browser.tabs.BrowserTab
import com.nova.browser.tabs.TabManager
import com.nova.browser.webview.WebViewManager

/**
 * MainActivity — single-activity architecture.
 *
 * Responsibilities:
 *  - Owns the Toolbar / URL bar / nav buttons
 *  - Manages BrowserFragment lifecycle (one per tab)
 *  - Observes TabManager LiveData and refreshes UI
 *  - Handles back press (WebView history first, then tab close)
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Current visible fragment
    private var activeBrowserFragment: BrowserFragment? = null

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        BrowserSettings.init(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Don't use the default ActionBar — we have a custom Toolbar
        supportActionBar?.hide()

        setupToolbar()
        setupNavButtons()
        setupTabSwitcher()
        observeTabManager()

        // Launch with first tab
        if (TabManager.getCount() == 0) {
            TabManager.addTab()
        }
        switchToTab(TabManager.activeIndex.value ?: 0)

        // Handle incoming intent (e.g. opened as default browser)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val url = intent?.dataString
        if (!url.isNullOrBlank()) {
            loadUrlInActiveTab(url)
        }
    }

    // ------------------------------------------------------------------
    // Toolbar / URL bar
    // ------------------------------------------------------------------

    private fun setupToolbar() {
        // URL bar submit on keyboard "Go" / Enter
        binding.urlBar.setOnEditorActionListener { _, actionId, event ->
            val isGo = actionId == EditorInfo.IME_ACTION_GO ||
                    (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            if (isGo) {
                commitUrlInput()
                true
            } else false
        }

        // Focus clears the formatted URL → show raw URL for editing
        binding.urlBar.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val activeTab = TabManager.getActive()
                binding.urlBar.setText(activeTab?.url ?: "")
                binding.urlBar.selectAll()
            } else {
                updateUrlDisplay()
            }
        }

        // Menu (three-dot) button
        binding.btnMenu.setOnClickListener { showPopupMenu(it) }
    }

    private fun commitUrlInput() {
        val input = binding.urlBar.text.toString().trim()
        val url = WebViewManager.resolveInput(input, BrowserSettings.searchEngine)
        hideKeyboard()
        binding.urlBar.clearFocus()
        loadUrlInActiveTab(url)
    }

    private fun updateUrlDisplay() {
        val url = TabManager.getActive()?.url ?: ""
        binding.urlBar.setText(WebViewManager.formatUrlForDisplay(url))

        // Security lock icon
        val isSecure = url.startsWith("https://")
        val isEmpty = url.isEmpty()
        binding.iconSecurity.setImageResource(
            when {
                isEmpty    -> R.drawable.ic_search
                isSecure   -> R.drawable.ic_lock
                else       -> R.drawable.ic_lock_open
            }
        )
    }

    // ------------------------------------------------------------------
    // Navigation buttons
    // ------------------------------------------------------------------

    private fun setupNavButtons() {
        binding.btnBack.setOnClickListener {
            if (activeBrowserFragment?.canGoBack() == true) {
                activeBrowserFragment?.goBack()
            }
        }

        binding.btnForward.setOnClickListener {
            if (activeBrowserFragment?.canGoForward() == true) {
                activeBrowserFragment?.goForward()
            }
        }

        binding.btnReload.setOnClickListener {
            val tab = TabManager.getActive()
            if (tab?.isLoading == true) {
                activeBrowserFragment?.stopLoading()
            } else {
                activeBrowserFragment?.reload()
            }
        }

        binding.btnTabs.setOnClickListener {
            toggleTabSwitcher()
        }

        binding.btnNewTab.setOnClickListener {
            val tab = TabManager.addTab()
            showBrowserFragment(tab)
            hideTabSwitcher()
        }
    }

    // ------------------------------------------------------------------
    // Fragment management
    // ------------------------------------------------------------------

    private fun switchToTab(index: Int) {
        TabManager.switchTo(index)
        val tab = TabManager.getActive() ?: return
        showBrowserFragment(tab)
    }

    private fun showBrowserFragment(tab: BrowserTab) {
        val tag = "tab_${tab.id}"
        val fm = supportFragmentManager
        val tx = fm.beginTransaction()

        // Hide all other fragments
        fm.fragments.forEach { f ->
            if (f.tag != tag) tx.hide(f)
        }

        var fragment = fm.findFragmentByTag(tag) as? BrowserFragment
        if (fragment == null) {
            fragment = BrowserFragment.newInstance(tab.id)
            fragment.onUrlChanged = { url ->
                runOnUiThread { updateUrlDisplay() }
            }
            fragment.onProgressChanged = { progress ->
                runOnUiThread { updateProgress(progress) }
            }
            fragment.onTitleChanged = { _ ->
                runOnUiThread { updateTabCountBadge() }
            }
            tx.add(R.id.fragment_container, fragment, tag)
        } else {
            tx.show(fragment)
        }

        tx.commitNowAllowingStateLoss()
        activeBrowserFragment = fragment

        // If the tab has a URL, load it now (first time only)
        if (tab.url.isNotEmpty() && tab.webView == null) {
            fragment.loadUrl(tab.url)
        }

        updateUrlDisplay()
        updateTabCountBadge()
    }

    private fun loadUrlInActiveTab(url: String) {
        val tab = TabManager.getActive() ?: return
        tab.url = url
        activeBrowserFragment?.loadUrl(url) ?: run {
            // Fragment not yet created — create it now
            showBrowserFragment(tab)
            activeBrowserFragment?.loadUrl(url)
        }
        updateUrlDisplay()
    }

    // ------------------------------------------------------------------
    // Progress bar
    // ------------------------------------------------------------------

    private fun updateProgress(progress: Int) {
        val tab = TabManager.getActive() ?: return
        if (progress in 1..99) {
            binding.progressBar.visibility = View.VISIBLE
            binding.progressBar.progress = progress
            // Switch reload icon to "stop"
            binding.btnReload.setImageResource(R.drawable.ic_stop)
        } else {
            binding.progressBar.visibility = View.GONE
            binding.btnReload.setImageResource(R.drawable.ic_reload)
        }
    }

    // ------------------------------------------------------------------
    // Tab switcher
    // ------------------------------------------------------------------

    private fun setupTabSwitcher() {
        binding.tabSwitcherPanel.visibility = View.GONE

        binding.btnNewTabSwitcher.setOnClickListener {
            TabManager.addTab()
            hideTabSwitcher()
            switchToTab(TabManager.activeIndex.value ?: 0)
        }

        binding.btnNewIncognitoTab.setOnClickListener {
            TabManager.addTab(incognito = true)
            hideTabSwitcher()
            switchToTab(TabManager.activeIndex.value ?: 0)
        }

        binding.btnCloseTabSwitcher.setOnClickListener {
            hideTabSwitcher()
        }
    }

    private fun toggleTabSwitcher() {
        if (binding.tabSwitcherPanel.visibility == View.VISIBLE) {
            hideTabSwitcher()
        } else {
            showTabSwitcher()
        }
    }

    private fun showTabSwitcher() {
        refreshTabList()
        binding.tabSwitcherPanel.visibility = View.VISIBLE
    }

    private fun hideTabSwitcher() {
        binding.tabSwitcherPanel.visibility = View.GONE
    }

    private fun refreshTabList() {
        val adapter = TabSwitcherAdapter(
            tabs = TabManager.getAll(),
            activeIndex = TabManager.activeIndex.value ?: 0,
            onTabSelected = { index ->
                switchToTab(index)
                hideTabSwitcher()
            },
            onTabClosed = { index ->
                TabManager.closeTab(index)
                refreshTabList()
                // Make sure the active fragment matches new active tab
                val newActive = TabManager.activeIndex.value ?: 0
                showBrowserFragment(TabManager.getAll()[newActive])
            }
        )
        binding.rvTabs.layoutManager = LinearLayoutManager(this)
        binding.rvTabs.adapter = adapter
    }

    private fun updateTabCountBadge() {
        binding.btnTabs.text = TabManager.getCount().toString()
    }

    // ------------------------------------------------------------------
    // LiveData observation
    // ------------------------------------------------------------------

    private fun observeTabManager() {
        TabManager.tabs.observe(this) { _ ->
            updateTabCountBadge()
        }

        TabManager.activeIndex.observe(this) { _ ->
            updateUrlDisplay()
        }
    }

    // ------------------------------------------------------------------
    // Popup menu
    // ------------------------------------------------------------------

    private fun showPopupMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.browser_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.menu_new_tab -> {
                    TabManager.addTab()
                    switchToTab(TabManager.getCount() - 1)
                    true
                }
                R.id.menu_incognito -> {
                    TabManager.addTab(incognito = true)
                    switchToTab(TabManager.getCount() - 1)
                    true
                }
                R.id.menu_clear_data -> {
                    BrowserSettings.clearBrowsingData(this)
                    android.widget.Toast.makeText(this, "Data cleared 🔥", android.widget.Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_desktop_mode -> {
                    BrowserSettings.desktopMode = !BrowserSettings.desktopMode
                    // Reload current page to apply new UA
                    activeBrowserFragment?.reload()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    // ------------------------------------------------------------------
    // Back press
    // ------------------------------------------------------------------

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            when {
                binding.tabSwitcherPanel.visibility == View.VISIBLE -> hideTabSwitcher()
                activeBrowserFragment?.canGoBack() == true -> activeBrowserFragment?.goBack()
                else -> {
                    // Close current tab; if it was the last one, exit app
                    val idx = TabManager.activeIndex.value ?: 0
                    if (TabManager.getCount() <= 1) {
                        finish()
                    } else {
                        TabManager.closeTab(idx)
                        switchToTab(TabManager.activeIndex.value ?: 0)
                    }
                }
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // ------------------------------------------------------------------
    // On exit: optionally clear data
    // ------------------------------------------------------------------

    override fun onStop() {
        super.onStop()
        if (BrowserSettings.clearDataOnExit) {
            BrowserSettings.clearBrowsingData(this)
        }
    }

    // ------------------------------------------------------------------
    // Utility
    // ------------------------------------------------------------------

    private fun hideKeyboard() {
        currentFocus?.let { v ->
            val imm = getSystemService(InputMethodManager::class.java)
            imm.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }
}
