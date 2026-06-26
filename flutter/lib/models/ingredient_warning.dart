class IngredientWarning {
  const IngredientWarning({
    required this.ingredient,
    required this.term,
    required this.message,
    required this.mayBeNonVegan,
    required this.mayBeNonVegetarian,
  });

  final String ingredient;
  final String term;
  final String message;
  final bool mayBeNonVegan;
  final bool mayBeNonVegetarian;
}
