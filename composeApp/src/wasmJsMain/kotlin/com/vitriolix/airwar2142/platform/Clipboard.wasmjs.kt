package com.vitriolix.airwar2142.platform

import kotlinx.coroutines.await

actual object Clipboard {
    actual fun copy(text: String) {
        clipboardWrite(text)
    }

    actual suspend fun paste(): String? = try {
        val res: JsString = clipboardRead().await()
        res.toString().ifEmpty { null }
    } catch (e: Throwable) {
        null
    }
}

private fun clipboardWrite(text: String): Unit =
    js("{ if (navigator.clipboard) { navigator.clipboard.writeText(text); } }")

// Resolve to '' (not null) when unavailable so await()'s type arg infers as non-null JsString.
private fun clipboardRead(): kotlin.js.Promise<JsString> =
    js("(navigator.clipboard ? navigator.clipboard.readText() : Promise.resolve(''))")
