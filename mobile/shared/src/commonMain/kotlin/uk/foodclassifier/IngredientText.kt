package uk.foodclassifier



internal fun normalizeBrackets(text: String): String {

    return text

        .replace('（', '(')

        .replace('）', ')')

        .replace('［', '[')

        .replace('］', ']')

        .replace('｛', '{')

        .replace('｝', '}')

}



internal fun splitRespectingBrackets(text: String): List<String> {

    val parts = mutableListOf<String>()

    val current = StringBuilder()

    var roundDepth = 0

    var squareDepth = 0



    for (char in text) {

        when (char) {

            '(' -> {

                roundDepth++

                current.append(char)

            }

            ')' -> {

                roundDepth = (roundDepth - 1).coerceAtLeast(0)

                current.append(char)

            }

            '[' -> {

                squareDepth++

                current.append(char)

            }

            ']' -> {

                squareDepth = (squareDepth - 1).coerceAtLeast(0)

                current.append(char)

            }

            ',' -> if (roundDepth == 0 && squareDepth == 0) {

                parts += current.toString()

                current.clear()

            } else {

                current.append(char)

            }

            else -> current.append(char)

        }

    }



    if (current.isNotEmpty()) {

        parts += current.toString()

    }



    return parts

}



internal fun extractBracketedContent(

    text: String,

    open: Char,

    close: Char,

): List<String> {

    val results = mutableListOf<String>()

    var depth = 0

    var start = -1



    for (index in text.indices) {

        when (text[index]) {

            open -> {

                if (depth == 0) {

                    start = index + 1

                }

                depth++

            }

            close -> {

                depth--

                if (depth == 0 && start >= 0) {

                    val content = text.substring(start, index)

                    results += content

                    results += extractBracketedContent(content, '(', ')')

                    results += extractBracketedContent(content, '[', ']')

                    start = -1

                }

            }

        }

    }



    return results

}



internal fun ingredientSegmentsForMatching(raw: String): List<String> {

    if (raw.isBlank()) {

        return emptyList()

    }



    var text = raw.trim()

    text = Regex("(?i)^ingredients\\s*:\\s*").replace(text, "")

    text = normalizeBrackets(text)

    text = uk.foodclassifier.text.OcrNormalizer.normalize(text)



    val segments = mutableListOf<String>()

    segments += text

    segments += splitRespectingBrackets(text)

    segments += extractBracketedContent(text, '(', ')')

    segments += extractBracketedContent(text, '[', ']')



    return segments

        .flatMap { expandFunctionalLabelToIngredients(it) }

        .map(::normalizeIngredientForMatching)

        .filter { it.isNotEmpty() }

        .distinct()

}



internal fun normalizeIngredientForMatching(value: String): String {

    return value.trim().lowercase()

        .replace(Regex("\\s+"), " ")

        .trim(' ', '.', ',', ';', ':')
}

internal val functionalLabelPrefixes = listOf(
    "sweeteners",
    "humectant",
    "bulking agent",
    "emulsifier",
    "raising agents",
    "raising agent",
    "acidity regulator",
    "stabilisers",
    "stabilizers",
    "stablisers",
    "preservative",
    "antioxidant",
    "colour",
    "color",
    "thickener",
    "gelling agent",
)

internal fun isFunctionalLabel(label: String): Boolean {
    val lower = label.lowercase().trim()
    return functionalLabelPrefixes.any { prefix ->
        lower == prefix ||
            lower.startsWith(prefix) ||
            prefix.startsWith(lower) ||
            lower.removeSuffix("s") == prefix.removeSuffix("s")
    }
}

internal fun isFunctionalLabelClause(clause: String): Boolean {
    val colonIndex = clause.indexOf(':')
    val label = if (colonIndex >= 0) {
        clause.substring(0, colonIndex)
    } else {
        clause
    }
    return isFunctionalLabel(label)
}

private fun splitIngredientsAfterFunctionalLabel(remainder: String): List<String> {
    val ingredients = mutableListOf<String>()
    for (part in splitRespectingBrackets(remainder)) {
        val trimmed = part.trim().trimEnd(',', '.', ';', ':')
        if (trimmed.isBlank()) {
            continue
        }
        if (isFunctionalLabelClause(trimmed)) {
            ingredients += expandFunctionalLabelToIngredients(trimmed)
        } else {
            ingredients += trimmed
        }
    }
    return ingredients
}

internal fun expandFunctionalLabelToIngredients(segment: String): List<String> {
    val trimmed = segment.trim().trimEnd(',', '.', ';', ':')
    if (trimmed.isBlank()) {
        return emptyList()
    }

    val colonIndex = trimmed.indexOf(':')
    if (colonIndex >= 0) {
        val label = trimmed.substring(0, colonIndex).trim()
        val remainder = trimmed.substring(colonIndex + 1).trim()
        if (isFunctionalLabel(label)) {
            if (remainder.isBlank()) {
                return emptyList()
            }
            return splitIngredientsAfterFunctionalLabel(remainder)
        }
    }

    if (isFunctionalLabelClause(trimmed)) {
        return emptyList()
    }

    return listOf(trimmed)
}

private val cosmeticFragranceMarkers =
    setOf("linalool", "citronellol", "linalyl acetate", "parfum", "benzyl alcohol")

internal fun correctCosmeticOcrChips(chips: List<String>): List<String> {
    val lowered = chips.map { it.lowercase() }
    val hasFragranceTail = lowered.any { chip ->
        cosmeticFragranceMarkers.any { marker -> chip.contains(marker) }
    } && lowered.count { it.contains("linalool") } >= 1

    return chips.map { chip ->
        var corrected = chip
        if (chip.equals("Lanolin", ignoreCase = true) && hasFragranceTail) {
            corrected = "Vanillin"
        }
        corrected.replace(
            Regex("""(?i)^Microcrysta[\w"'\s]*Wine\s+Cellulose$"""),
            "Microcrystalline Cellulose",
        ).replace(
            Regex("""(?i)^Wine\s+Cellulose$"""),
            "Microcrystalline Cellulose",
        )
    }
}

fun parseIngredientsToChips(raw: String): List<String> =
    uk.foodclassifier.text.IngredientLabelParser.parseBlock(raw).chips

fun formatIngredientsFromChips(chips: List<String>): String = chips.joinToString(", ")
