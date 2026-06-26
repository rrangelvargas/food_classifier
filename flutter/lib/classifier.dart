import 'models/ambiguous_ingredient.dart';
import 'models/classification_result.dart';
import 'models/diet_classification.dart';
import 'models/ingredient_warning.dart';
import 'data/uk_ingredients.dart';
import 'ingredient_text.dart';

List<String> parseIngredients(String raw) => ingredientSegmentsForMatching(raw);

ClassificationResult classifyIngredients(
  Object input, {
  Set<String>? nonVegetarian,
  Set<String>? nonVegan,
  Set<String>? veganExceptions,
  List<AmbiguousIngredient>? ambiguous,
}) {
  final ingredients = switch (input) {
    String raw => ingredientSegmentsForMatching(raw),
    List<String> list => list,
    _ => throw ArgumentError('Expected String or List<String>'),
  };

  return _classifyIngredientsInternal(
    ingredients: ingredients,
    nonVegetarian: nonVegetarian ?? UkIngredients.nonVegetarian,
    nonVegan: nonVegan ?? UkIngredients.nonVegan,
    veganExceptions: veganExceptions ?? UkIngredients.veganExceptions,
    ambiguous: ambiguous ?? UkIngredients.ambiguous,
  );
}

ClassificationResult _classifyIngredientsInternal({
  required List<String> ingredients,
  required Set<String> nonVegetarian,
  required Set<String> nonVegan,
  required Set<String> veganExceptions,
  required List<AmbiguousIngredient> ambiguous,
}) {
  final matchedNonVegetarian = <String>{};
  final matchedNonVegan = <String>{};
  final warnings = <IngredientWarning>[];
  final seenWarningGroups = <String>{};

  for (final ingredient in ingredients) {
    final normalizedIngredient = normalizeIngredientForMatching(ingredient);
    if (normalizedIngredient.isEmpty) {
      continue;
    }

    if (_isException(normalizedIngredient, veganExceptions) &&
        !_isIngredientListBlob(normalizedIngredient)) {
      continue;
    }

    var matchedNonVegetarianOnIngredient = false;

    for (final term in nonVegetarian) {
      if (termMatches(term, normalizedIngredient)) {
        matchedNonVegetarian.add(term);
        matchedNonVegetarianOnIngredient = true;
        break;
      }
    }

    for (final term in nonVegan) {
      if (termMatches(term, normalizedIngredient)) {
        matchedNonVegan.add(term);
        break;
      }
    }

    if (matchedNonVegetarianOnIngredient ||
        _isIngredientListBlob(normalizedIngredient)) {
      continue;
    }

    for (final entry in ambiguous) {
      if (!_ambiguousTermMatches(entry.term, normalizedIngredient)) {
        continue;
      }

      final warningGroup = _ambiguousWarningGroup(entry.term);
      if (seenWarningGroups.contains(warningGroup)) {
        break;
      }

      seenWarningGroups.add(warningGroup);
      warnings.add(
        IngredientWarning(
          ingredient: ingredient,
          term: entry.term,
          message: entry.message,
          mayBeNonVegan: entry.mayBeNonVegan,
          mayBeNonVegetarian: entry.mayBeNonVegetarian,
        ),
      );
      break;
    }
  }

  final definitiveClassification = switch (null) {
    _ when matchedNonVegetarian.isNotEmpty => DietClassification.neither,
    _ when matchedNonVegan.isNotEmpty => DietClassification.vegetarian,
    _ => DietClassification.vegan,
  };

  final classification =
      _applyWarnings(definitiveClassification, warnings);

  return ClassificationResult(
    classification: classification,
    definitiveClassification: definitiveClassification,
    matchedNonVegetarian: matchedNonVegetarian.toList(),
    matchedNonVegan: matchedNonVegan.toList(),
    warnings: warnings,
  );
}

DietClassification _applyWarnings(
  DietClassification definitiveClassification,
  List<IngredientWarning> warnings,
) {
  if (warnings.isEmpty ||
      definitiveClassification == DietClassification.neither) {
    return definitiveClassification;
  }

  if (definitiveClassification == DietClassification.vegan &&
      warnings.any((warning) =>
          warning.mayBeNonVegan || warning.mayBeNonVegetarian)) {
    return DietClassification.uncertain;
  }

  if (definitiveClassification == DietClassification.vegetarian &&
      warnings.any((warning) => warning.mayBeNonVegetarian)) {
    return DietClassification.uncertain;
  }

  return definitiveClassification;
}

bool _isException(String ingredient, Set<String> exceptions) {
  return exceptions.any((exception) => ingredient.contains(exception));
}

const _glycerolFamilyTerms = {'glycerol', 'glycerin', 'glycerine', 'e422'};
const _beverageTerms = {'wine', 'wine vinegar', 'cider', 'beer'};

bool _isIngredientListBlob(String ingredient) {
  return ','.allMatches(ingredient).length >= 1;
}

String _ambiguousWarningGroup(String term) {
  if (_glycerolFamilyTerms.contains(term)) {
    return 'glycerol-family';
  }
  if (_beverageTerms.contains(term)) {
    return 'beverage';
  }
  return term;
}

bool _ambiguousTermMatches(String term, String ingredient) {
  if (_glycerolFamilyTerms.contains(term)) {
    return _matchesStandaloneGlycerol(term, ingredient);
  }
  if (_beverageTerms.contains(term)) {
    return _matchesBeverageIngredient(term, ingredient);
  }
  return termMatches(term, ingredient);
}

bool _matchesStandaloneGlycerol(String term, String ingredient) {
  if (_isGlycerolDerivativeCompound(ingredient)) {
    return false;
  }
  return termMatches(term, ingredient);
}

bool _isGlycerolDerivativeCompound(String ingredient) {
  final normalized = ingredient.trim().toLowerCase();
  if (_glycerolFamilyTerms.contains(normalized)) {
    return false;
  }
  return normalized.endsWith('glycerin') ||
      normalized.contains('glyceryl') ||
      normalized.contains('polyglyceryl');
}

bool _matchesBeverageIngredient(String term, String ingredient) {
  final normalized = ingredient.trim().toLowerCase();
  if (normalized.isEmpty) {
    return false;
  }
  if (term == 'wine' && normalized.contains('cellulose')) {
    return false;
  }
  if (_beverageTerms.contains(normalized) || normalized == term) {
    return true;
  }
  if (term.contains(' ') || term.length > 8) {
    return normalized.contains(term);
  }
  return termMatches(term, normalized);
}

bool termMatches(String term, String ingredient) {
  if (term.contains(' ') || term.length > 8) {
    return ingredient.contains(term);
  }

  final pattern = RegExp('(?<![a-z0-9])${RegExp.escape(term)}(?![a-z0-9])');
  return pattern.hasMatch(ingredient);
}
