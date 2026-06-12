package uk.foodclassifier.text

import uk.foodclassifier.isFunctionalLabel
import uk.foodclassifier.splitRespectingBrackets

/**
 * Generic per-chip OCR repairs applied after tokenization.
 */
internal object ChipOcrCorrector {
    private val leadingParenMissingLetter = Regex("""(?i)^\(([a-z])([a-z]*)""")
    private val digitSixBeforeVowelWord = Regex("""\b6(?=[aeiou][a-z])""")
    private val nonAsciiLetterSuffix = Regex("""[\u0080-\uFFFF]+$""")
    private val embeddedFunctionalLabelBeforeColon = Regex(
        """(?i),\s*(?=(?:Sweeteners|Humectant|Bulking|Emulsifier|Raising|Acidity)\w*\s*:)""",
    )
    private val functionalLabelTail = Regex(
        """(?i)\s+(?:Paising|Raising|Kaising)\w*\s*Agents?\b.*$""",
    )
    private val saltGarbleTail = Regex("""(?i)\s+(?:polydextros|polydextrose)\s+a$""")
    private val ocrLetterNoise = Regex("""[şğı]""")
    private val dutchGarbagePrefix = Regex("""(?i)^(?:rundercolla\w*|rijsmiddele\s+)""")
    private val commonAdditiveFixes = listOf(
        Regex("""(?i)^Carageenan$""") to "Carrageenan",
        Regex("""(?i)^Guar\s+Gum$""") to "Guar Gum",
        Regex("""(?i)^Citrus\s+Fibre$""") to "Citrus Fibre",
        Regex("""(?i)^Cabonates$""") to "Carbonates",
        Regex("""(?i)^Sodium\s+Cabonates$""") to "Sodium Carbonates",
    )

    fun splitMergedChip(chip: String): List<String> {
        if (chip.isBlank()) {
            return emptyList()
        }

        var working = stripEmbeddedFunctionalLabel(chip)
        working = functionalLabelTail.replace(working, "").trim()
        working = ocrLetterNoise.replace(working, "")

        if (embeddedFunctionalLabelBeforeColon.containsMatchIn(working)) {
            return working.split(embeddedFunctionalLabelBeforeColon)
                .map { it.trim().trimEnd(',', '.', ';', ':') }
                .filter { it.isNotBlank() }
        }

        if (saltGarbleTail.containsMatchIn(working)) {
            val ingredient = saltGarbleTail.replace(working, "").trim()
            return listOf(ingredient, "Salt").flatMap { part ->
                splitRespectingBrackets(part.replace('\n', ','))
                    .map { it.trim().trimEnd(',', '.', ';', ':') }
                    .filter { it.isNotBlank() }
            }
        }

        if (Regex("""(?i)\s+Salt$""").containsMatchIn(working)) {
            val ingredient = working.replace(Regex("""(?i)\s+Salt$"""), "").trim()
            return listOf(ingredient, "Salt").filter { it.isNotBlank() }
        }

        return splitRespectingBrackets(working.replace('\n', ','))
            .map { it.trim().trimEnd(',', '.', ';', ':') }
            .filter { it.isNotBlank() }
    }

    fun correct(chip: String): String {
        if (chip.isBlank()) {
            return chip
        }

        var result = chip.trim()
        result = stripEmbeddedFunctionalLabel(result)
        result = fixLetterReplacedByParen(result)
        result = fixDigitSixAsLetterG(result)
        result = stripNonAsciiSuffix(result)
        result = ocrLetterNoise.replace(result, "")
        result = fixDutchGarbage(result)
        result = fixSaltGarble(result)
        result = fixPalmOil(result)
        result = fixFatReducedCocoa(result)
        result = fixSodiumCarbonates(result)
        result = applyCommonAdditiveFixes(result)
        result = fixSkyrLabelGarbles(result)
        return result.trim()
    }

    private fun fixSkyrLabelGarbles(text: String): String {
        var result = text
        result = result.replace(Regex("""(?i)^wary maize\b"""), "waxy maize")
        result = result.replace(Regex("""(?i)^natural$"""), "natural flavouring")
        result = result.replace(
            Regex("""(?i)^concentrate (\d+)\.(\d)(?:3|8)\b"""),
        ) { match ->
            "from concentrate ${match.groupValues[1]}.${match.groupValues[2]}%"
        }
        result = result.replace(
            Regex("""(?i)\bfrom concentrate (\d+)\.(\d)(?:3|8)\b"""),
        ) { match ->
            "from concentrate ${match.groupValues[1]}.${match.groupValues[2]}%"
        }
        return result
    }

    private fun stripEmbeddedFunctionalLabel(chip: String): String {
        val colonIndex = chip.indexOf(':')
        if (colonIndex <= 0) {
            return chip
        }
        val label = chip.substring(0, colonIndex).trim()
        if (!isFunctionalLabel(label)) {
            return chip
        }
        return chip.substring(colonIndex + 1).trim()
    }

    private fun fixLetterReplacedByParen(text: String): String {
        return leadingParenMissingLetter.replace(text) { match ->
            "C${match.groupValues[1].uppercase()}${match.groupValues[2]}"
        }
    }

    private fun fixDigitSixAsLetterG(text: String): String {
        return digitSixBeforeVowelWord.replace(text, "G")
    }

    private fun stripNonAsciiSuffix(text: String): String {
        return text.replace(nonAsciiLetterSuffix, "")
    }

    private fun fixDutchGarbage(text: String): String {
        return dutchGarbagePrefix.replace(text, "")
    }

    private fun fixSaltGarble(text: String): String {
        return text.replace(Regex("""(?i)^polydextros\s+a$"""), "Salt")
    }

    private fun fixPalmOil(text: String): String {
        return text.replace(Regex("""(?i)^Palm\s+(?:Di|b[lid])$"""), "Palm Oil")
            .replace(Regex("""(?i)^rundercolla\w*\s+Palm\s+(?:Di|b[lid])$"""), "Palm Oil")
    }

    private fun fixFatReducedCocoa(text: String): String {
        return text.replace(
            Regex("""(?i)^Fat-reduced\s+water\s+(?:palr\s+)?Cocoa Powder\s*(\(\d+%\))?$"""),
        ) { match ->
            if (match.groupValues[1].isNotEmpty()) {
                "Fat-reduced Cocoa Powder ${match.groupValues[1]}"
            } else {
                "Fat-reduced Cocoa Powder"
            }
        }
    }

    private fun fixSodiumCarbonates(text: String): String {
        return text.replace(
            Regex("""(?i)^Sodium\s+(?:rijsmiddele\s+)?(?:Cabonates|Carbonates|\w*\s*nates)$"""),
            "Sodium Carbonates",
        )
    }

    private fun applyCommonAdditiveFixes(text: String): String {
        var result = text
        for ((pattern, replacement) in commonAdditiveFixes) {
            if (pattern.matches(result)) {
                result = replacement
            }
        }
        return result
    }
}
