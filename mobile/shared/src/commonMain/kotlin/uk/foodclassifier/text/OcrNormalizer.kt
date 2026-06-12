package uk.foodclassifier.text

import uk.foodclassifier.functionalLabelPrefixes
import uk.foodclassifier.isFunctionalLabel
import uk.foodclassifier.splitRespectingBrackets

internal object OcrNormalizer {
    private val allergenNames = setOf(
        "milk", "soy", "soya", "gluten", "eggs", "egg", "nuts", "nut", "fish", "celery",
        "mustard", "sesame", "sulphites", "sulphite", "lupin", "molluscs", "crustaceans",
    )

    private val parenthesisInWord = Regex("""([A-Za-z])\(([a-z]{2,})""")
    private val bracketRoundMismatch = Regex("""\(([^)\]]+)\]\.?""")
    private val duplicateCloseBeforeComma = Regex("""\)\),""")
    private val fatReducedNoiseBeforeCocoa =
        Regex("""(?i)Fat-\s*\w+,\s*(?:palr\s+)?Cocoa Powder""")
    private val sodiumCarbonatesTail = Regex("""(?i)\bSodium\s+\w*\s*nates\b""")
    private val proteinBlendContinuation =
        Regex("""(?i)(Protein Blend\s*\[)([^\]]+)(\])\s+(Whey\s+[^,;]+)""")
    private val missingCommaBeforeFunctionalLabel =
        Regex("""(?i)\b(\w+)\s+(?=(?:Stabilis?|Stablis|Sweeteners|Humectant|Bulking|Emulsifier|Raising|Acidity)\w*)""")
    private val ingredientColonBetweenItems =
        Regex("""(?i)([A-Z][\w-]*(?:\s+[A-Z][\w-]*)*):\s+(?=[A-Z])""")
    private val extraWhitespace = Regex("""\s+""")

    fun normalize(text: String): String {
        var result = normalizeStructure(text)
        result = normalizeMilkArtifacts(result)
        result = normalizeLagenToCollagen(result)
        return collapseWhitespace(result)
    }

    private fun normalizeStructure(text: String): String {
        var result = text
        result = result.replace(Regex("""(?i)\bC\(hese\b"""), "Cheese")
        result = parenthesisInWord.replace(result) { match ->
            val inner = match.groupValues[2]
            if (inner.lowercase() in allergenNames) {
                match.value
            } else {
                "${match.groupValues[1]}$inner"
            }
        }
        result = bracketRoundMismatch.replace(result) { "(${it.groupValues[1]})" }
        result = result.replace(
            Regex("""(?i)\[([^\]]+\([^)]+\))\.(?=\s*Whey)"""),
            "[$1,",
        )
        result = duplicateCloseBeforeComma.replace(result, "),")
        result = result.replace(
            Regex("""(?i)\bIsolate\s+(Milk)\b(?!\))"""),
            "Isolate ($1)",
        )
        result = proteinBlendContinuation.replace(result) { match ->
            "${match.groupValues[1]}${match.groupValues[2]}, ${match.groupValues[3]}${match.groupValues[4]}"
        }
        result = result.replace(
            Regex("""(?i)(Protein Blend\s*\[)([^;\]]+?)\s+(Whey\s+[^,;]+)"""),
        ) { match ->
            if (match.groupValues[2].contains(']')) {
                match.value
            } else {
                val body = match.groupValues[2].trim().trimEnd(',')
                val whey = match.groupValues[3].trim().trimEnd(',', ' ', ']')
                "${match.groupValues[1]}$body, $whey]"
            }
        }
        result = missingCommaBeforeFunctionalLabel.replace(result) { match ->
            val before = match.groupValues[1]
            val after = match.value.removePrefix(before).trimStart()
            "$before, $after"
        }
        result = fatReducedNoiseBeforeCocoa.replace(result, "Fat-reduced Cocoa Powder")
        result = sodiumCarbonatesTail.replace(result, "Sodium Carbonates")
        result = replaceNonFunctionalColons(result)
        result = ensureBovineCollagenPrefix(result)
        result = result.replace(
            Regex("""(?i)\bmicrocrysta[\w"']*\s*wine\s+(?:line\s+)?cellulose\b"""),
            "Microcrystalline Cellulose",
        )
        result = result.replace(
            Regex("""(?i)\bwine\s+(?:line\s+)?cellulose\b"""),
            "Microcrystalline Cellulose",
        )
        result = result.replace(Regex("""(?i)\bfanur\w*\b"""), "Flavourings")
        return collapseWhitespace(result)
    }

    private fun normalizeMilkArtifacts(text: String): String {
        var result = text
        result = result.replace(Regex("""\bl\s*milk\b""", RegexOption.IGNORE_CASE), "milk")
        result = result.replace(Regex("""\blmilk\b""", RegexOption.IGNORE_CASE), "milk")
        result = result.replace(Regex("""\b1ilk\b""", RegexOption.IGNORE_CASE), "milk")
        result = result.replace(Regex("""\bmi\s+lk\b""", RegexOption.IGNORE_CASE), "milk")
        result = result.replace(Regex("""\bmi\s*me-ngr\b""", RegexOption.IGNORE_CASE), "milk")
        return result
    }

    private fun normalizeLagenToCollagen(text: String): String {
        return text.replace(Regex("""(?i)\b(\w*)lagen\b""")) { match ->
            val word = match.value
            if (word.contains("collagen", ignoreCase = true)) {
                word
            } else {
                val suffix = word.substring(word.indexOf("lagen", ignoreCase = true))
                "col$suffix"
            }
        }
    }

    private fun ensureBovineCollagenPrefix(text: String): String {
        return Regex("""(?i)\b(\w*collagen\s+hydrolysate)\b""").replace(text) { match ->
            val before = text.substring(0, match.range.first).lowercase()
            if (before.contains("bovine") || before.contains("porcine") || before.contains("fish")) {
                match.value
            } else {
                "Bovine ${match.value}"
            }
        }
    }

    private fun replaceNonFunctionalColons(text: String): String {
        return ingredientColonBetweenItems.replace(text) { match ->
            val label = match.groupValues[1]
            if (isFunctionalLabelFuzzy(label)) {
                match.value
            } else {
                "$label, "
            }
        }
    }

    private fun isFunctionalLabelFuzzy(label: String): Boolean {
        if (isFunctionalLabel(label)) {
            return true
        }
        val lower = label.lowercase().trim()
        return functionalLabelPrefixes.any { prefix ->
            lower == prefix ||
                lower.startsWith(prefix) ||
                prefix.startsWith(lower) ||
                lower.removeSuffix("s") == prefix.removeSuffix("s")
        }
    }

    private fun collapseWhitespace(text: String): String {
        return text
            .replace('\n', ' ')
            .replace("•", ", ")
            .replace(extraWhitespace, " ")
            .replace(Regex("""\s+,"""), ",")
            .replace(Regex(""",\s*,"""), ",")
            .trim()
    }
}
