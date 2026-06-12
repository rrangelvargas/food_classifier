package uk.foodclassifier

private data class IngredientMatchCategory(
    val terms: Set<String>,
    val phrase: String,
)

private val matchCategories = listOf(
    IngredientMatchCategory(
        terms = setOf("bovine", "beef", "collagen", "gelatine", "gelatin", "veal", "suet", "tallow"),
        phrase = "ingredients derived from cattle (such as beef collagen or gelatine)",
    ),
    IngredientMatchCategory(
        terms = setOf("pork", "bacon", "ham", "pancetta", "prosciutto", "salami", "lard"),
        phrase = "pork or pork-derived ingredients",
    ),
    IngredientMatchCategory(
        terms = setOf("chicken", "turkey", "duck", "goose", "poultry", "poultry fat", "poultry stock"),
        phrase = "chicken or other poultry",
    ),
    IngredientMatchCategory(
        terms = setOf(
            "lamb", "rabbit", "venison", "boar", "hare", "meat", "meat extract",
            "meat stock", "offal", "liver", "blood", "sausage",
        ),
        phrase = "meat or meat-derived ingredients",
    ),
    IngredientMatchCategory(
        terms = setOf(
            "fish", "fish oil", "fish sauce", "fish extract", "fish stock",
            "salmon", "tuna", "cod", "haddock", "hake", "herring", "mackerel",
            "trout", "sardine", "sardines", "anchovy", "anchovies",
        ),
        phrase = "fish or fish-derived ingredients",
    ),
    IngredientMatchCategory(
        terms = setOf(
            "prawn", "prawns", "shrimp", "crab", "lobster", "mollusc", "mollusk",
            "oyster", "scallop", "squid", "calamari", "octopus", "shellfish",
        ),
        phrase = "shellfish or other seafood",
    ),
    IngredientMatchCategory(
        terms = setOf(
            "milk", "whey", "butter", "cream", "cheese", "casein", "lactose",
            "milk powder", "milk solids", "skimmed milk", "semi-skimmed milk",
            "whole milk", "whey powder", "yogurt", "yoghurt", "lactoglobulin",
            "lactalbumin", "paneer", "quark", "fromage", "custard", "buttermilk",
            "ghee", "curds", "parmesan", "parmigiano reggiano", "grana padano",
        ),
        phrase = "cow's milk or other dairy",
    ),
    IngredientMatchCategory(
        terms = setOf("egg", "eggs", "egg white", "egg yolk", "albumen", "mayonnaise"),
        phrase = "eggs or egg-derived ingredients",
    ),
    IngredientMatchCategory(
        terms = setOf("honey", "beeswax", "bee wax"),
        phrase = "honey or beeswax",
    ),
    IngredientMatchCategory(
        terms = setOf("carmine", "cochineal", "e120", "shellac", "e904"),
        phrase = "ingredients derived from insects (such as carmine or shellac)",
    ),
    IngredientMatchCategory(
        terms = setOf("animal rennet", "animal fat", "animal fats", "isinglass", "lanolin"),
        phrase = "animal-derived ingredients",
    ),
)

fun describeClassificationMatches(
    matchedNonVegetarian: List<String>,
    matchedNonVegan: List<String>,
): List<String> {
    val terms = (matchedNonVegetarian + matchedNonVegan)
        .map { it.lowercase() }
        .distinct()
    return describeMatchedTerms(terms)
}

private fun describeMatchedTerms(terms: List<String>): List<String> {
    if (terms.isEmpty()) {
        return emptyList()
    }

    val remaining = terms.toMutableSet()
    val messages = mutableListOf<String>()

    for (category in matchCategories) {
        if (remaining.any { it in category.terms }) {
            messages += "This product contains ${category.phrase}."
            remaining.removeAll { it in category.terms }
        }
    }

    for (term in remaining.sorted()) {
        messages += "This product contains ${humanizeTerm(term)}."
    }

    return messages
}

private fun humanizeTerm(term: String): String = when (term) {
    "e471" -> "E471 (mono- and diglycerides of fatty acids)"
    "e472" -> "E472 (esters of mono- and diglycerides)"
    else -> term
}

fun describeUncertainResult(definitiveClassification: DietClassification): String = when (
    definitiveClassification
) {
    DietClassification.Vegan ->
        "This product is likely vegan, but some ingredients may come from animal sources. " +
            "Check the label if you need to be sure."
    DietClassification.Vegetarian ->
        "This product is likely vegetarian, but some ingredients may come from animal sources. " +
            "Check the label if you need to be sure."
    else ->
        "Some ingredients could not be confirmed. Check the label if you need to be sure."
}
