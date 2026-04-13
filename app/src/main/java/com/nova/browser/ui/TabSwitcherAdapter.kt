package com.nova.browser.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nova.browser.R
import com.nova.browser.tabs.BrowserTab

/**
 * Adapter for the tab switcher overlay (full-screen grid / list).
 */
class TabSwitcherAdapter(
    private val tabs: List<BrowserTab>,
    private val activeIndex: Int,
    private val onTabSelected: (Int) -> Unit,
    private val onTabClosed: (Int) -> Unit
) : RecyclerView.Adapter<TabSwitcherAdapter.TabViewHolder>() {

    inner class TabViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tab_title)
        val url: TextView = view.findViewById(R.id.tab_url)
        val closeBtn: ImageButton = view.findViewById(R.id.btn_close_tab)
        val incognitoBadge: View = view.findViewById(R.id.incognito_badge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tab, parent, false)
        return TabViewHolder(view)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        val tab = tabs[position]

        holder.title.text = tab.title.ifBlank { "New Tab" }
        holder.url.text = tab.url
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
            .ifBlank { "about:blank" }

        // Highlight active tab
        holder.itemView.isSelected = (position == activeIndex)
        holder.itemView.alpha = if (position == activeIndex) 1.0f else 0.75f

        // Incognito badge
        holder.incognitoBadge.visibility = if (tab.isIncognito) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onTabSelected(position) }
        holder.closeBtn.setOnClickListener { onTabClosed(position) }
    }

    override fun getItemCount(): Int = tabs.size
}
