package uk.foodclassifier.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import uk.foodclassifier.text.IngredientLabelParser

class SkyrLabelTest {
    private val skyrOcr =
        "WITH SWEETENER: INGREDIENTS: Lactose free skyr (yogurt (MILK)), grape juice concentrate, " +
            "blueberry puree from concentrate 1.8%, waxy maize starch, natural flavouring, " +
            "black carrot juice concentrate, hibiscus concentrate, " +
            "sweetener (steviol glycosides from stevia), lactase enzymes. " +
            "For allergens, see ingredients in BOLD. Suitable for vegetarians. Keep refrigerated."

    private val expectedChips = listOf(
        "Lactose free skyr (yogurt (MILK))",
        "grape juice concentrate",
        "blueberry puree from concentrate 1.8%",
        "waxy maize starch",
        "natural flavouring",
        "black carrot juice concentrate",
        "hibiscus concentrate",
        "steviol glycosides from stevia",
        "lactase enzymes",
    )

    @Test
    fun parsesSkyrLabelWithNestedAllergenParens() {
        val parsed = IngredientLabelParser.parse(skyrOcr)
        assertEquals(expectedChips, parsed.chips)
    }

    @Test
    fun ignoresAllergyFooterWhenIngredientsHeaderMissing() {
        val footerOnly =
            "For allergens, see ingredients in BOLD. Suitable for vegetarians. Keep refrigerated."
        val parsed = IngredientLabelParser.parse(footerOnly)
        assertTrue(parsed.chips.isEmpty())
    }

    @Test
    fun parsesSkyrWhenHeaderIsFollowedByAllergyLineInOcr() {
        val garbledOrder =
            "WITH SWEETENER: INGREDIENTS: For allergens, see ingredients in BOLD. " +
                "Lactose free skyr (yogurt (MILK)), grape juice concentrate, lactase enzymes. " +
                "Suitable for vegetarians."
        val parsed = IngredientLabelParser.parse(garbledOrder)
        assertTrue(parsed.chips.any { it.contains("skyr", ignoreCase = true) })
        assertTrue(parsed.chips.any { it.contains("grape juice", ignoreCase = true) })
    }

    @Test
    fun repairsPartialSkyrOcrWithLineBreaks() {
        val partial =
            "INGREDIENTS: Concentrate 1.83\nwary maize starch\nnatural"
        val parsed = IngredientLabelParser.parse(partial)
        assertEquals(
            listOf(
                "from concentrate 1.8%",
                "waxy maize starch",
                "natural flavouring",
            ),
            parsed.chips,
        )
    }

    @Test
    fun repairsPartialSkyrOcrWithCommas() {
        val partial = "Concentrate 1.83, wary maize starch, natural"
        val parsed = IngredientLabelParser.parse(partial)
        assertEquals(
            listOf(
                "from concentrate 1.8%",
                "waxy maize starch",
                "natural flavouring",
            ),
            parsed.chips,
        )
    }
}
