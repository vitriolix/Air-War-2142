package com.vitriolix.airwar2142.platform

/**
 * System clipboard, per-backend. `copy` is fire-and-forget (browser write is async); `paste` is a
 * `suspend` fun because browser clipboard *read* is async + permission/gesture-gated, and returns
 * null when unavailable or denied. Wired for JVM/JS/wasm; Android is a stub until an Activity
 * Context is plumbed through (only `Platform.kt` exists in androidMain today).
 */
expect object Clipboard {
    fun copy(text: String)
    suspend fun paste(): String?
}
