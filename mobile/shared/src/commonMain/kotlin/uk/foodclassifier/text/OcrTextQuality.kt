package uk.foodclassifier.text

/**
 * Heuristic score for comparing OCR passes — higher means more likely a full ingredient list.
 */
internal fun scoreOcrText(text: String): Int {
    if (text.isBlank()) {
        return 0
    }
    var score = text.length
    score += text.count { it == ',' } * 25
    score += text.count { it == '(' || it == '[' } * 15
    if (Regex("""(?i)\bingredients\b""").containsMatchIn(text)) {
        score += 120
    }
    if (Regex("""(?i)\b(?:skyr|yogurt|milk|concentrate|starch|flavouring|sweetener)\b""").containsMatchIn(text)) {
        score += 40
    }
    return score
}

internal fun pickBetterOcrText(vararg candidates: String): String {
    return candidates
        .filter { it.isNotBlank() }
        .maxByOrNull { scoreOcrText(it) }
        .orEmpty()
}
