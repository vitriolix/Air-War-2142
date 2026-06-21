package com.vitriolix.airwar2142.platform

/**
 * Android stub: a real ClipboardManager needs an Activity/Context, which isn't plumbed through yet
 * (androidMain currently has only Platform.kt). Wire this when a Context is available.
 */
actual object Clipboard {
    actual fun copy(text: String) { /* TODO: ClipboardManager — needs Context */ }
    actual suspend fun paste(): String? = null
}
