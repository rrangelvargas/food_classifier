package uk.foodclassifier.text

internal object AllergyBoundary {
    val sectionEndPatterns = listOf(
        Regex("""(?i)\bALLERGY(?:VICE|VCE|ADV\w*)?\b"""),
        Regex("""(?i)\ballergy(?:\s+|\s*[-\s]*)(?:adv[i1l]ce|advice|vice|vce)\b"""),
        Regex("""(?i)\ballergy\s+advice\b"""),
        Regex("""(?i)\ballergy\s+adv[i1l]ce\b"""),
        Regex("""(?i)\ballergy\s+a[yvi][a-z]*\b"""),
    )

    fun trimTail(text: String): String {
        val allergyStart = sectionEndPatterns
            .mapNotNull { pattern -> pattern.find(text)?.range?.first }
            .minOrNull()
        return if (allergyStart != null) {
            text.substring(0, allergyStart)
        } else {
            text
        }.trim(' ', '.', ';', ':', ',')
    }

    fun stripTrailingGarbage(text: String): String {
        return text
            .replace(Regex("""(?i)[,.;\s]*\bALLERGY(?:VICE|VCE|ADV\w*)?\b.*$"""), "")
            .replace(Regex("""(?i)[,.;\s]*\ballergy(?:\s+adv\w*|vice|vce)\b.*$"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
