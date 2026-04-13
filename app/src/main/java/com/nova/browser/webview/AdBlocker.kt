package com.nova.browser.webview

/**
 * AdBlocker — Phase 1: static domain blocklist.
 *
 * Phase 2 (future): load EasyList filter rules from a file / network,
 * parse into a trie for O(1) lookup, update weekly in the background.
 */
object AdBlocker {

    var enabled: Boolean = true

    /**
     * Domains whose requests should be blocked outright.
     * Extend this list freely — it's the only thing that needs changing
     * when you want to add more blocked domains.
     */
    private val blockedDomains = setOf(
        // Google advertising
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "pagead2.googlesyndication.com",
        // AppNexus / Xandr
        "adnxs.com",
        // Amazon advertising
        "adsystem.com",
        "amazon-adsystem.com",
        // Tracking & analytics
        "scorecardresearch.com",
        "quantserve.com",
        "adsafeprotected.com",
        "moatads.com",
        "taboola.com",
        "outbrain.com",
        "revcontent.com",
        // Social trackers
        "connect.facebook.net",
        "platform.twitter.com",
        // Generic
        "ads.yahoo.com",
        "ads.pubmatic.com",
        "rubiconproject.com",
        "openx.net",
        "criteo.com",
        "pubads.g.doubleclick.net"
    )

    /**
     * Returns true if the given URL should be blocked.
     * Checks whether any blocked domain is a substring of the request URL.
     */
    fun shouldBlock(url: String): Boolean {
        if (!enabled) return false
        return blockedDomains.any { domain -> url.contains(domain) }
    }
}
