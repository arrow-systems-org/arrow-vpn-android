package org.arrowx.vpn.domain.model

private val countryTokenMap = mapOf(
    "de" to "DE",
    "germany" to "DE",
    "alemania" to "DE",
    "at" to "AT",
    "austria" to "AT",
    "se" to "SE",
    "sweden" to "SE",
    "suecia" to "SE",
    "ch" to "CH",
    "switzerland" to "CH",
    "suiza" to "CH",
    "us" to "US",
    "usa" to "US",
    "eeuu" to "US",
    "estadosunidos" to "US",
    "unitedstates" to "US"
)

fun inferCountryCode(
    id: String,
    name: String,
    hint: String? = null
): String {
    extractCountryCode(hint)?.let { return it }
    extractCountryCode(id)?.let { return it }

    val normalizedTokens = "$id $name"
        .lowercase()
        .split(Regex("[^a-z0-9]+"))
        .filter { it.isNotBlank() }

    normalizedTokens.firstNotNullOfOrNull { token -> countryTokenMap[token] }?.let { return it }
    return "UN"
}

fun String.toFlagEmoji(): String {
    val code = trim().uppercase()
    if (code.length != 2 || code.any { !it.isLetter() }) return "\uD83C\uDF10"
    val first = Character.codePointAt(code, 0) - 'A'.code + REGIONAL_INDICATOR_A
    val second = Character.codePointAt(code, 1) - 'A'.code + REGIONAL_INDICATOR_A
    return String(Character.toChars(first)) + String(Character.toChars(second))
}

private fun extractCountryCode(value: String?): String? {
    val raw = value?.trim().orEmpty()
    if (raw.isBlank()) return null

    if (raw.length == 2 && raw.all { it.isLetter() }) {
        return raw.uppercase()
    }

    val pathMatch = Regex("""/([a-z]{2})(?:\.[a-z0-9]+)?$""").find(raw.lowercase())
        ?.groupValues
        ?.getOrNull(1)
    if (!pathMatch.isNullOrBlank()) return pathMatch.uppercase()

    val tokenMatch = raw.lowercase().split(Regex("[^a-z]+")).firstOrNull { it.length == 2 }
    return tokenMatch?.uppercase()
}

private const val REGIONAL_INDICATOR_A = 0x1F1E6
