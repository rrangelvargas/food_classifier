class AmbiguousIngredient {
  const AmbiguousIngredient({
    required this.term,
    required this.message,
    this.mayBeNonVegan = true,
    this.mayBeNonVegetarian = false,
  });

  final String term;
  final String message;
  final bool mayBeNonVegan;
  final bool mayBeNonVegetarian;
}
