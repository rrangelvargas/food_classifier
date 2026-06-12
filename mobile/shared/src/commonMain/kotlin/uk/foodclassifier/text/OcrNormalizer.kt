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
    private val duplicateCloseBeforeFunctionalLabel =
        Regex(
            """(?i)\)\),\s*(?=(?:Sweeteners|Humectant|Bulking|Emulsifier|Raising|Acidity|Stabilis|Sabil|Bovine)\w*\s*:)""",
        )
    private val fatReducedNoiseBeforeCocoa =
        Regex("""(?i)Fat-reduced\s+water,\s*(?:palr\s+)?Cocoa Powder""")
    private val sodiumCarbonatesTail = Regex("""(?i)\bSodium\s+\w*\s*nates\b""")
    private val proteinBlendContinuation =
        Regex("""(?i)(Protein Blend\s*\[)([^\]]+)(\])\s+(Whey\s+[^,;]+)""")
    private val missingCommaBeforeFunctionalLabel =
        Regex(
            """(?i)\b(\w+)\s+(?=(?:Stabilis|Stablis|Sabil|Sweeteners|Humectant|Bulking|Emulsifier|Raising|Acidity)\w*\s*:)""",
        )
    private val ingredientColonBetweenItems =
        Regex("""(?i)([A-Z][\w-]*(?:\s+[A-Z][\w-]*)*):\s+(?=[A-Z])""")
    private val extraWhitespace = Regex("""\s+""")

    fun normalize(text: String): String {
        var result = joinLineBrokenIngredients(text)
        result = normalizeStructure(result)
        result = normalizeMilkArtifacts(result)
        result = normalizeLagenToCollagen(result)
        result = ensureBovineCollagenPrefix(result)
        return collapseWhitespace(result)
    }

    private fun joinLineBrokenIngredients(text: String): String {
        val lines = text.split(Regex("""[\n\r]+"""))
            .map { it.trim().trimEnd(',', '.', ';') }
            .filter { it.isNotBlank() }
        if (lines.size < 2) {
            return text
        }

        val commaCount = text.count { it == ',' }
        if (commaCount >= lines.size - 1) {
            return text.replace('\n', ' ').replace('\r', ' ')
        }

        val looksLikeBrokenList = lines.count { line ->
            val lower = line.lowercase()
            !lower.startsWith("ingredients") &&
                !lower.startsWith("for allergen") &&
                !lower.startsWith("suitable for") &&
                line.length >= 3
        } >= 2

        if (looksLikeBrokenList) {
            return lines.joinToString(", ")
        }

        return text.replace('\n', ' ').replace('\r', ' ')
    }

    private fun normalizeStructure(text: String): String {
        var result = text
        result = result.replace(Regex("""(?i)\bC\(hese\b"""), "Cheese")
        result = result.replace(Regex("""\b6(?=[aeiou][a-z])"""), "G")
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
        result = closeSquareBracketBeforeFunctionalLabel(result)
        result = duplicateCloseBeforeFunctionalLabel.replace(result, "), ")
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
        result = result.replace(
            Regex("""(?i)(Isolate \(Milk\)),\s*(Sweeteners\s*:)"""),
            "Isolate (Milk)), $2",
        )
        result = fatReducedNoiseBeforeCocoa.replace(result, "Fat-reduced Cocoa Powder")
        result = sodiumCarbonatesTail.replace(result, "Sodium Carbonates")
        result = result.replace(
            Regex("""(?i)\bSodium\s+rijsmiddele\s+Cabonates\b"""),
            "Sodium Carbonates",
        )
        result = replaceNonFunctionalColons(result)
        result = result.replace(
            Regex("""(?i)\bmicrocrysta[\w"']*\s*wine\s+(?:line\s+)?cellulose\b"""),
            "Microcrystalline Cellulose",
        )
        result = result.replace(
            Regex("""(?i)\bwine\s+(?:line\s+)?cellulose\b"""),
            "Microcrystalline Cellulose",
        )
        result = result.replace(Regex("""(?i)\bfanur\w*\b"""), "Flavourings")
        result = result.replace(Regex("""(?i)\bProtern\b"""), "Protein")
        result = result.replace(Regex("""(?i)\brundercolla\w*\s+Palm\s+(?:Di|b[lid])\b"""), "Palm Oil")
        result = result.replace(Regex("""(?i)\bPalm\s+(?:Di|b[lid])\b"""), "Palm Oil")
        result = result.replace(
            Regex("""(?i)\bFat-reduced\s+water\s+(?:palr\s+)?Cocoa Powder(\s*\(\d+%\))?"""),
            "Fat-reduced Cocoa Powder$1",
        )
        result = result.replace(Regex("""(?i)\bpolydextros\s+BI\b"""), "Salt")
        result = result.replace(Regex("""(?i)\bpolydextros\s+a\b"""), "Salt")
        result = result.replace(Regex("""(?i)\bpolydextros\b"""), "Polydextrose")
        result = result.replace(Regex("""(?i)\bPolydlextrose\b"""), "Polydextrose")
        result = result.replace(Regex("""(?i)\bpolydextroe\s+BI\b"""), "Salt")
        result = result.replace(Regex("""(?i)\bPolydextroe\b"""), "Polydextrose")
        result = result.replace(Regex("""(?i)\((Milk)\]"""), "(Milk)]")
        result = result.replace(Regex("""(?i)Isolate\s+Milk\)\)"""), "Isolate (Milk))")
        result = result.replace(Regex("""(?i)Isolate\s+Milk\)(?!\))"""), "Isolate (Milk)")
        result = result.replace(
            Regex("""(?i)zuurtereg\s+ALLERGI\s+glutenbeve\s+"""),
            "",
        )
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
            val phrase = match.groupValues[1].trim()
            val lower = phrase.lowercase()
            when {
                lower.startsWith("bovine ") -> "Bovine Collagen Hydrolysate"
                lower.startsWith("porcine ") -> "Porcine Collagen Hydrolysate"
                lower.startsWith("fish ") -> "Fish Collagen Hydrolysate"
                else -> {
                    val before = text.substring(0, match.range.first).trimEnd().lowercase()
                    if (before.endsWith("bovine") || before.endsWith("porcine") || before.endsWith("fish")) {
                        "Collagen Hydrolysate"
                    } else {
                        "Bovine Collagen Hydrolysate"
                    }
                }
            }
        }
    }

    private fun closeSquareBracketBeforeFunctionalLabel(text: String): String {
        val pattern = Regex(
            """(?i)\(Milk\)\),\s*(?=(?:Sweeteners|Humectant|Bulking|Emulsifier|Raising|Acidity)\w*\s*:)""",
        )
        return pattern.replace(text) { match ->
            val before = text.substring(0, match.range.first)
            val openSquare = before.lastIndexOf('[')
            val hasUnclosedSquare = openSquare >= 0 &&
                !before.substring(openSquare).contains(']')
            if (hasUnclosedSquare) {
                "(Milk)], "
            } else {
                match.value
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
