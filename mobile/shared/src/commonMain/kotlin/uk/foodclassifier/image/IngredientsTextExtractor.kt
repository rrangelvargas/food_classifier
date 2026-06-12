package uk.foodclassifier.image

import uk.foodclassifier.text.IngredientLabelParser

object IngredientsTextExtractor {
    fun extract(rawText: String): String = IngredientLabelParser.parse(rawText).displayText
}
