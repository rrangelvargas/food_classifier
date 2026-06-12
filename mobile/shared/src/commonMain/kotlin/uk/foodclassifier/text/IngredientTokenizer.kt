package uk.foodclassifier.text

import uk.foodclassifier.correctCosmeticOcrChips
import uk.foodclassifier.expandFunctionalLabelToIngredients
import uk.foodclassifier.splitRespectingBrackets

internal object IngredientTokenizer {
    private val ingredientsHeader = Regex("""(?i)^[\w-]*?gredients\s*:\s*""")

    fun toChips(text: String): List<String> {
        if (text.isBlank()) {
            return emptyList()
        }

        var normalized = AllergyBoundary.trimTail(text.trim())
        normalized = ingredientsHeader.replaceFirst(normalized, "")

        val chips = normalized.split(Regex("""\s*;\s*"""))
            .filter { it.isNotBlank() }
            .flatMap { part ->
                splitRespectingBrackets(part.replace('\n', ','))
                    .flatMap { expandFunctionalLabelToIngredients(it) }
            }
            .map { it.trim().trimEnd(',', '.', ';', ':') }
            .filter { it.isNotBlank() }

        return correctCosmeticOcrChips(chips)
    }
}
