package uk.foodclassifier

import uk.foodclassifier.data.AmbiguousIngredient
import uk.foodclassifier.data.UkIngredients

enum class DietClassification(val label: String) {
    Vegan("Vegan"),
    Vegetarian("Vegetarian"),
    Neither("Neither"),
    Uncertain("Uncertain"),
}

data class IngredientWarning(
    val ingredient: String,
    val term: String,
    val message: String,
    val mayBeNonVegan: Boolean,
    val mayBeNonVegetarian: Boolean,
)

data class ClassificationResult(
    val classification: DietClassification,
    val definitiveClassification: DietClassification,
    val matchedNonVegetarian: List<String>,
    val matchedNonVegan: List<String>,
    val warnings: List<IngredientWarning>,
) {
    val hasWarnings: Boolean get() = warnings.isNotEmpty()
}

fun parseIngredients(raw: String): List<String> = ingredientSegmentsForMatching(raw)

fun classifyIngredients(
    ingredients: List<String>,
    nonVegetarian: Set<String> = UkIngredients.nonVegetarian,
    nonVegan: Set<String> = UkIngredients.nonVegan,
    veganExceptions: Set<String> = UkIngredients.veganExceptions,
    ambiguous: List<AmbiguousIngredient> = UkIngredients.ambiguous,
): ClassificationResult {
    return classifyIngredientsInternal(
        ingredients = ingredients,
        nonVegetarian = nonVegetarian,
        nonVegan = nonVegan,
        veganExceptions = veganExceptions,
        ambiguous = ambiguous,
    )
}

fun classifyIngredients(
    raw: String,
    nonVegetarian: Set<String> = UkIngredients.nonVegetarian,
    nonVegan: Set<String> = UkIngredients.nonVegan,
    veganExceptions: Set<String> = UkIngredients.veganExceptions,
    ambiguous: List<AmbiguousIngredient> = UkIngredients.ambiguous,
): ClassificationResult {
    return classifyIngredientsInternal(
        ingredients = ingredientSegmentsForMatching(raw),
        nonVegetarian = nonVegetarian,
        nonVegan = nonVegan,
        veganExceptions = veganExceptions,
        ambiguous = ambiguous,
    )
}

private fun classifyIngredientsInternal(
    ingredients: List<String>,
    nonVegetarian: Set<String>,
    nonVegan: Set<String>,
    veganExceptions: Set<String>,
    ambiguous: List<AmbiguousIngredient>,
): ClassificationResult {
    val matchedNonVegetarian = linkedSetOf<String>()
    val matchedNonVegan = linkedSetOf<String>()
    val warnings = mutableListOf<IngredientWarning>()
    val seenWarningGroups = linkedSetOf<String>()

    for (ingredient in ingredients) {
        val normalizedIngredient = normalizeIngredientForMatching(ingredient)
        if (normalizedIngredient.isEmpty()) {
            continue
        }

        if (
            isException(normalizedIngredient, veganExceptions) &&
            !isIngredientListBlob(normalizedIngredient)
        ) {
            continue
        }

        var matchedNonVegetarianOnIngredient = false

        for (term in nonVegetarian) {
            if (termMatches(term, normalizedIngredient)) {
                matchedNonVegetarian += term
                matchedNonVegetarianOnIngredient = true
                break
            }
        }

        for (term in nonVegan) {
            if (termMatches(term, normalizedIngredient)) {
                matchedNonVegan += term
                break
            }
        }

        if (matchedNonVegetarianOnIngredient || isIngredientListBlob(normalizedIngredient)) {
            continue
        }

        for (entry in ambiguous) {
            if (!ambiguousTermMatches(entry.term, normalizedIngredient)) {
                continue
            }

            val warningGroup = ambiguousWarningGroup(entry.term)
            if (warningGroup in seenWarningGroups) {
                break
            }

            seenWarningGroups += warningGroup
            warnings += IngredientWarning(
                ingredient = ingredient,
                term = entry.term,
                message = entry.message,
                mayBeNonVegan = entry.mayBeNonVegan,
                mayBeNonVegetarian = entry.mayBeNonVegetarian,
            )
            break
        }
    }

    val definitiveClassification = when {
        matchedNonVegetarian.isNotEmpty() -> DietClassification.Neither
        matchedNonVegan.isNotEmpty() -> DietClassification.Vegetarian
        else -> DietClassification.Vegan
    }

    val classification = applyWarnings(definitiveClassification, warnings)

    return ClassificationResult(
        classification = classification,
        matchedNonVegetarian = matchedNonVegetarian.toList(),
        matchedNonVegan = matchedNonVegan.toList(),
        warnings = warnings,
        definitiveClassification = definitiveClassification,
    )
}

private fun applyWarnings(
    definitiveClassification: DietClassification,
    warnings: List<IngredientWarning>,
): DietClassification {
    if (warnings.isEmpty() || definitiveClassification == DietClassification.Neither) {
        return definitiveClassification
    }

    if (
        definitiveClassification == DietClassification.Vegan &&
        warnings.any { it.mayBeNonVegan || it.mayBeNonVegetarian }
    ) {
        return DietClassification.Uncertain
    }

    if (
        definitiveClassification == DietClassification.Vegetarian &&
        warnings.any { it.mayBeNonVegetarian }
    ) {
        return DietClassification.Uncertain
    }

    return definitiveClassification
}

private fun isException(ingredient: String, exceptions: Set<String>): Boolean {
    return exceptions.any { exception -> ingredient.contains(exception) }
}

private val glycerolFamilyTerms = setOf("glycerol", "glycerin", "glycerine", "e422")
private val beverageTerms = setOf("wine", "wine vinegar", "cider", "beer")

private fun isIngredientListBlob(ingredient: String): Boolean {
    return ingredient.count { it == ',' } >= 1
}

private fun ambiguousWarningGroup(term: String): String = when (term) {
    in glycerolFamilyTerms -> "glycerol-family"
    in beverageTerms -> "beverage"
    else -> term
}

private fun ambiguousTermMatches(term: String, ingredient: String): Boolean {
    return when (term) {
        in glycerolFamilyTerms -> matchesStandaloneGlycerol(term, ingredient)
        in beverageTerms -> matchesBeverageIngredient(term, ingredient)
        else -> termMatches(term, ingredient)
    }
}

private fun matchesStandaloneGlycerol(term: String, ingredient: String): Boolean {
    if (isGlycerolDerivativeCompound(ingredient)) {
        return false
    }
    return termMatches(term, ingredient)
}

private fun isGlycerolDerivativeCompound(ingredient: String): Boolean {
    val normalized = ingredient.trim().lowercase()
    if (normalized in glycerolFamilyTerms) {
        return false
    }
    return normalized.endsWith("glycerin") ||
        normalized.contains("glyceryl") ||
        normalized.contains("polyglyceryl")
}

private fun matchesBeverageIngredient(term: String, ingredient: String): Boolean {
    val normalized = ingredient.trim().lowercase()
    if (normalized.isBlank()) {
        return false
    }
    if (term == "wine" && normalized.contains("cellulose")) {
        return false
    }
    if (normalized in beverageTerms || normalized == term) {
        return true
    }
    if (term.contains(' ') || term.length > 8) {
        return normalized.contains(term)
    }
    return termMatches(term, normalized)
}

internal fun termMatches(term: String, ingredient: String): Boolean {
    if (term.contains(' ') || term.length > 8) {
        return ingredient.contains(term)
    }

    val pattern = Regex("(?<![a-z0-9])${Regex.escape(term)}(?![a-z0-9])")
    return pattern.containsMatchIn(ingredient)
}
