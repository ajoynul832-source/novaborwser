# Nova Browser — Native Android (Kotlin)

A lightweight, fast Android browser built with native Kotlin + Android WebView (Chromium).
Converted from Flutter. Phase 1: Via-level feature parity.

---

## Quick Start

### Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34
- Device / emulator running Android 8.0+ (API 26+)

### Open in Android Studio
1. `File → Open` → select the `NovaBrowser/` folder
2. Let Gradle sync finish (~1 min first time)
3. Run on device or emulator: `Shift+F10`

---

## Project Structure

```
NovaBrowser/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/nova/browser/
│   │   ├── ui/
│   │   │   ├── MainActivity.kt          # Central hub — toolbar, fragments, back press
│   │   │   ├── BrowserFragment.kt       # Hosts one WebView per tab
│   │   │   ├── TabSwitcherAdapter.kt    # RecyclerView adapter for tab drawer
│   │   │   └── SettingsActivity.kt      # Settings screen
│   │   ├── tabs/
│   │   │   ├── BrowserTab.kt            # Data model for a single tab
│   │   │   └── TabManager.kt            # LiveData-backed tab state (singleton)
│   │   ├── webview/
│   │   │   ├── WebViewManager.kt        # Builds + configures WebView instances
│   │   │   └── AdBlocker.kt             # Domain blocklist (expandable)
│   │   └── settings/
│   │       └── BrowserSettings.kt       # SharedPreferences wrapper
│   └── res/
│       ├── layout/
│       │   ├── activity_main.xml        # Toolbar + fragment container + tab switcher
│       │   ├── activity_settings.xml    # Settings screen
│       │   ├── item_tab.xml             # Single tab row in switcher
│       │   └── item_settings_switch.xml # Reusable switch row
│       ├── drawable/                    # Vector icons + shape backgrounds
│       ├── menu/browser_menu.xml        # Three-dot popup menu
│       ├── values/
│       │   ├── themes.xml               # Dark Material theme (#0A0A0A)
│       │   ├── colors.xml
│       │   └── strings.xml
│       └── xml/network_security_config.xml
```

---

## Architecture

```
MainActivity
    │
    ├── TabManager (singleton LiveData)
    │       ├── BrowserTab[]   ← data models
    │       └── activeIndex
    │
    ├── BrowserFragment (one per tab, retained across rotation)
    │       └── WebView  ←  WebViewManager.buildWebView()
    │                            ├── WebViewClient  (AdBlocker, HTTPS upgrade, navigation)
    │                            └── WebChromeClient (progress, title)
    │
    ├── SettingsActivity
    │       └── BrowserSettings (SharedPreferences)
    │
    └── TabSwitcherAdapter (RecyclerView)
```

**Data flow:**
1. User types in URL bar → `WebViewManager.resolveInput()` → fragment loads URL
2. WebView fires `onPageStarted/Finished` → updates `BrowserTab` model → `TabManager.notifyChanged()`
3. `MainActivity` observes `TabManager.tabs` LiveData → refreshes badge + URL bar

---

## Features (Phase 1)

| Feature | Status |
|---|---|
| Multi-tab browsing | ✅ |
| Incognito tabs (no history/cookies) | ✅ |
| Ad blocker (domain blocklist) | ✅ |
| Force HTTPS upgrade | ✅ |
| DuckDuckGo / Google / Bing / Brave search | ✅ |
| Back / Forward / Reload / Stop | ✅ |
| Loading progress bar | ✅ |
| HTTPS lock icon | ✅ |
| Desktop mode (UA switch) | ✅ |
| Data saver (hide images/video via JS) | ✅ |
| Clear data on exit | ✅ |
| Supernova clear button | ✅ |
| Dark theme (#0A0A0A) | ✅ |
| Act as default browser | ✅ |

---

## Phase 2 Roadmap

### Performance
- [ ] Preload next-tab WebView in background
- [ ] WebView pool — reuse destroyed tab WebViews

### Ad Blocking
- [ ] Load EasyList filter rules from a local file
- [ ] Parse into a trie for O(1) URL matching
- [ ] Weekly background update via WorkManager

### Features
- [ ] Bookmarks (Room database)
- [ ] History (Room database)  
- [ ] Download manager integration
- [ ] Share page / copy URL
- [ ] Find-in-page (`WebView.findAllAsync`)
- [ ] Reader mode (inject Readability.js)
- [ ] Split-screen view (two WebViews stacked)
- [ ] Custom homepage / new-tab wallpaper
- [ ] Vault (locked incognito tabs, BiometricPrompt)

### Polish
- [ ] Smooth tab switcher animation (shared element)
- [ ] Swipe-to-close tabs
- [ ] Pull-to-refresh
- [ ] Bottom sheet URL bar expansion

---

## Extending the Ad Blocker

`AdBlocker.kt` uses a simple `Set<String>` for Phase 1.
To upgrade to EasyList in Phase 2:

```kotlin
// In AdBlocker.kt
private val filterTrie = FilterTrie()

fun loadRules(inputStream: InputStream) {
    inputStream.bufferedReader().forEachLine { line ->
        if (!line.startsWith("!") && line.isNotBlank()) {
            filterTrie.insert(line)
        }
    }
}

fun shouldBlock(url: String): Boolean {
    if (!enabled) return false
    return filterTrie.matches(url)
}
```

---

## Why No Heavy Libraries?

| Flutter dependency | Native replacement |
|---|---|
| `webview_flutter` | Android `WebView` (built-in) |
| `provider` / `riverpod` | `LiveData` + `ViewModel` (AndroidX) |
| `shared_preferences` | `SharedPreferences` (built-in) |
| `go_router` | `FragmentManager` (built-in) |

Total added dependencies: **4** (core-ktx, appcompat, material, constraintlayout, viewpager2, lifecycle, coroutines).
No OkHttp, no Retrofit, no image loaders.

---

## License
MIT
