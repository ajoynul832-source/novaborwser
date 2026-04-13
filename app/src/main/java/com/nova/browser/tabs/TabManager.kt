package com.nova.browser.tabs

import androidx.lifecycle.MutableLiveData

/**
 * TabManager owns the list of open tabs and the active tab index.
 * It is the single source of truth — UI observes LiveData from here.
 */
object TabManager {

    // Observed by UI to re-render when tabs change
    val tabs = MutableLiveData<MutableList<BrowserTab>>(mutableListOf())
    val activeIndex = MutableLiveData(0)

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    fun getAll(): MutableList<BrowserTab> = tabs.value ?: mutableListOf()

    fun getActive(): BrowserTab? {
        val list = getAll()
        val idx = activeIndex.value ?: 0
        return list.getOrNull(idx)
    }

    fun getCount(): Int = getAll().size

    // ------------------------------------------------------------------
    // Mutations — always post back to LiveData so observers fire
    // ------------------------------------------------------------------

    fun addTab(incognito: Boolean = false): BrowserTab {
        val tab = BrowserTab(isIncognito = incognito, title = if (incognito) "Incognito" else "New Tab")
        val list = getAll()
        list.add(tab)
        tabs.postValue(list)
        activeIndex.postValue(list.size - 1)
        return tab
    }

    fun closeTab(index: Int) {
        val list = getAll()
        if (list.size <= 1) {
            // Always keep at least one tab — replace with a fresh one
            list[0] = BrowserTab()
            tabs.postValue(list)
            activeIndex.postValue(0)
            return
        }
        // Destroy WebView to free memory before removing
        list.getOrNull(index)?.webView?.destroy()
        list.removeAt(index)
        val newActive = when {
            (activeIndex.value ?: 0) >= list.size -> list.size - 1
            else -> activeIndex.value ?: 0
        }
        tabs.postValue(list)
        activeIndex.postValue(newActive)
    }

    fun switchTo(index: Int) {
        if (index in 0 until getCount()) {
            activeIndex.postValue(index)
        }
    }

    /** Called when a tab's URL / title changes so the switcher refreshes */
    fun notifyChanged() {
        tabs.postValue(getAll())
    }
}
