package uk.foodclassifier.text

/**
 * Removes adjacent-language column markers (e.g. NL-INGREDIENTS) without
 * stripping valid ingredient words from the English column.
 */
internal object ForeignColumnFilter {
    private val foreignColumnHeader =
        Regex("""(?i)\b(?!EN)[A-Z]{2}-ING(?:REDIENTS?|REL)\s*:?\s*""")
    private val hyphenatedForeignFragment = Regex("""\b[a-z]+(?:-[a-z]+)+\b""")

    fun strip(text: String): String {
        return text
            .replace(foreignColumnHeader, " ")
            .replace(hyphenatedForeignFragment, " ")
            .replace(Regex("""\s+ne\s+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
