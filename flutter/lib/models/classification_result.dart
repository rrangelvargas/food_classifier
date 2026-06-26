import 'diet_classification.dart';
import 'ingredient_warning.dart';

class ClassificationResult {
  const ClassificationResult({
    required this.classification,
    required this.definitiveClassification,
    required this.matchedNonVegetarian,
    required this.matchedNonVegan,
    required this.warnings,
  });

  final DietClassification classification;
  final DietClassification definitiveClassification;
  final List<String> matchedNonVegetarian;
  final List<String> matchedNonVegan;
  final List<IngredientWarning> warnings;

  bool get hasWarnings => warnings.isNotEmpty;
}
