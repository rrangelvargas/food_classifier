package uk.foodclassifier.text

import uk.foodclassifier.correctCosmeticOcrChips
import uk.foodclassifier.expandFunctionalLabelToIngredients
import uk.foodclassifier.isFunctionalLabel
import uk.foodclassifier.splitRespectingBrackets

internal object IngredientTokenizer {
    private val ingredientsHeader = Regex("""(?i)^[\w-]*?gredients\s*:\s*""")
    private val periodBeforeFunctionalLabel = Regex(
        """\.\s+(?=(?:Stabilis|Stablis|Sabil|Sweeteners|Humectant|Bulking|Emulsifier|Raising|Acidity|Bovine)\w*)""",
        RegexOption.IGNORE_CASE,
    )
    private val periodBeforeCapitalWord = Regex("""\.\s+(?=[A-Z][a-z])""")

    fun toChips(text: String): List<String> {
        if (text.isBlank()) {
            return emptyList()
        }

        var normalized = AllergyBoundary.trimTail(text.trim())
        normalized = ingredientsHeader.replaceFirst(normalized, "")

        val chips = splitIntoClauses(normalized)
            .flatMap { clause ->
                splitRespectingBrackets(clause.replace('\n', ','))
                    .flatMap { segment ->
                        expandFunctionalLabelToIngredients(segment)
                            .flatMap { ChipOcrCorrector.splitMergedChip(it) }
                            .flatMap { expandFunctionalLabelToIngredients(it) }
                    }
            }
            .map(::cleanChip)
            .filter { it.isNotBlank() }
            .filterNot(::isFunctionalLabelOnly)

        return correctCosmeticOcrChips(chips)
    }

    private fun splitIntoClauses(text: String): List<String> {
        var prepared = periodBeforeFunctionalLabel.replace(text, "; ")
        prepared = periodBeforeCapitalWord.replace(prepared, ", ")
        return prepared.split(Regex("""\s*;\s*""")).filter { it.isNotBlank() }
    }

    private fun cleanChip(chip: String): String {
        var result = chip.trim().trimEnd(',', '.', ';', ':')
        if (isFunctionalLabelOnly(result)) {
            return ""
        }
        if (Regex("""(?i)\bflavourings\b""").containsMatchIn(result)) {
            return "Flavourings"
        }
        if (Regex("""(?i)^polydextrose\s+\w{1,3}$""").containsMatchIn(result)) {
            return ""
        }
        return ChipOcrCorrector.correct(result)
    }

    private fun isFunctionalLabelOnly(chip: String): Boolean {
        val label = chip.substringBefore(':').trim()
        if (isFunctionalLabel(label) || isFunctionalLabel(chip.trim())) {
            return true
        }
        val lower = label.lowercase()
        return lower.contains("acidity") && (lower.contains("reg") || lower.contains("req"))
    }
}
