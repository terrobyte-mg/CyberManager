package com.terrobytes.cybermanaver2.network

/**
 * [raw] is MikrotikRawClient.read()'s output: one API word per line.
 * Extracts the value of a "=key=value" word, e.g. parseApiValue(raw, "name").
 */
internal fun parseApiValue(raw: String, key: String): String? {
    val prefix = "=$key="
    return raw.lineSequence().firstOrNull { it.startsWith(prefix) }?.removePrefix(prefix)
}

internal fun isApiTrap(raw: String): Boolean = raw.lineSequence().any { it == "!trap" }

internal fun apiTrapMessage(raw: String): String = parseApiValue(raw, "message") ?: raw.trim()