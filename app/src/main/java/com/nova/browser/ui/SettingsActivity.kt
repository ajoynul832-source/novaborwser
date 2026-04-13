package com.nova.browser.ui

import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.nova.browser.R
import com.nova.browser.databinding.ActivitySettingsBinding
import com.nova.browser.settings.BrowserSettings
import com.nova.browser.webview.AdBlocker

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private lateinit var switchAdBlock: SwitchMaterial
    private lateinit var switchDataSaver: SwitchMaterial
    private lateinit var switchHttpsUpgrade: SwitchMaterial
    private lateinit var switchClearOnExit: SwitchMaterial
    private lateinit var switchDesktopMode: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        setupSwitchRows()
        loadCurrentSettings()
        setupListeners()
    }

    private fun setupSwitchRows() {
        fun configureRow(rowId: Int, label: String): SwitchMaterial {
            val row = binding.root.findViewById<View>(rowId)
            row.findViewById<TextView>(R.id.setting_title).text = label
            return row.findViewById(R.id.setting_switch)
        }

        switchAdBlock      = configureRow(R.id.row_ad_block,      "Ad Blocker")
        switchDataSaver    = configureRow(R.id.row_data_saver,    "Data Saver")
        switchHttpsUpgrade = configureRow(R.id.row_https_upgrade, "Force HTTPS")
        switchClearOnExit  = configureRow(R.id.row_clear_on_exit, "Clear Data on Exit")
        switchDesktopMode  = configureRow(R.id.row_desktop_mode,  "Desktop Mode")
    }

    private fun loadCurrentSettings() {
        switchAdBlock.isChecked      = BrowserSettings.adBlockEnabled
        switchDataSaver.isChecked    = BrowserSettings.dataSaver
        switchHttpsUpgrade.isChecked = BrowserSettings.upgradeHttps
        switchClearOnExit.isChecked  = BrowserSettings.clearDataOnExit
        switchDesktopMode.isChecked  = BrowserSettings.desktopMode

        if (BrowserSettings.toolbarPosition == "top")
            binding.root.findViewById<RadioButton>(R.id.radio_top).isChecked = true
        else
            binding.root.findViewById<RadioButton>(R.id.radio_bottom).isChecked = true

        val radioId = when (BrowserSettings.searchEngine) {
            "Google" -> R.id.radio_google
            "Bing"   -> R.id.radio_bing
            "Brave"  -> R.id.radio_brave
            else     -> R.id.radio_ddg
        }
        binding.root.findViewById<RadioButton>(radioId).isChecked = true
    }

    private fun setupListeners() {
        switchAdBlock.setOnCheckedChangeListener      { _, v -> BrowserSettings.adBlockEnabled = v; AdBlocker.enabled = v }
        switchDataSaver.setOnCheckedChangeListener    { _, v -> BrowserSettings.dataSaver = v }
        switchHttpsUpgrade.setOnCheckedChangeListener { _, v -> BrowserSettings.upgradeHttps = v }
        switchClearOnExit.setOnCheckedChangeListener  { _, v -> BrowserSettings.clearDataOnExit = v }
        switchDesktopMode.setOnCheckedChangeListener  { _, v -> BrowserSettings.desktopMode = v }

        binding.root.findViewById<RadioGroup>(R.id.radio_group_toolbar)
            .setOnCheckedChangeListener { _, id ->
                BrowserSettings.toolbarPosition = if (id == R.id.radio_top) "top" else "bottom"
            }

        binding.root.findViewById<RadioGroup>(R.id.radio_group_search)
            .setOnCheckedChangeListener { _, id ->
                BrowserSettings.searchEngine = when (id) {
                    R.id.radio_google -> "Google"
                    R.id.radio_bing   -> "Bing"
                    R.id.radio_brave  -> "Brave"
                    else              -> "DuckDuckGo"
                }
            }

        binding.btnClearData.setOnClickListener {
            BrowserSettings.clearBrowsingData(this)
            Toast.makeText(this, "Browsing data cleared 🔥", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
