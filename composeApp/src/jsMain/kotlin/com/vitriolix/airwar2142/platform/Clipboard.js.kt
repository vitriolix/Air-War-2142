package com.vitriolix.airwar2142.platform

import kotlinx.browser.window
import kotlinx.coroutines.await

actual object Clipboard {
    actual fun copy(text: String) {
        val clip = window.navigator.asDynamic().clipboard
        if (clip != null) clip.writeText(text)
    }

    actual suspend fun paste(): String? = try {
        val clip = window.navigator.asDynamic().clipboard
        if (clip == null) null else (clip.readText() as kotlin.js.Promise<String>).await()
    } catch (e: Throwable) {
        null
    }
}
