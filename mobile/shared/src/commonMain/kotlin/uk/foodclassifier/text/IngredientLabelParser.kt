package uk.foodclassifier.text

data class ParsedIngredientLabel(
    val chips: List<String>,
    val displayText: String,
)

object IngredientLabelParser {
    /**
     * Full pipeline: raw ML Kit OCR text → cleaned ingredient chips.
     */
    fun parse(rawOcr: String): ParsedIngredientLabel {
        val block = LabelBoundaryFinder.findBlock(rawOcr) ?: return empty()
        return parseBlock(block)
    }

    /**
     * Parse text that is already an ingredients block (manual entry or tests).
     */
    fun parseBlock(ingredientsBlock: String): ParsedIngredientLabel {
        if (ingredientsBlock.isBlank()) {
            return empty()
        }

        val cleaned = AllergyBoundary.stripTrailingGarbage(
            AllergyBoundary.trimTail(
                OcrNormalizer.normalize(
                    ForeignColumnFilter.strip(ingredientsBlock),
                ),
            ),
        )

        if (cleaned.isBlank()) {
            return empty()
        }

        val chips = IngredientTokenizer.toChips(cleaned)
        return ParsedIngredientLabel(
            chips = chips,
            displayText = chips.joinToString("\n"),
        )
    }

    private fun empty() = ParsedIngredientLabel(emptyList(), "")
}
