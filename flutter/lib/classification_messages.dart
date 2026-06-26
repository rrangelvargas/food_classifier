import 'models/diet_classification.dart';

class _IngredientMatchCategory {
  const _IngredientMatchCategory({
    required this.terms,
    required this.phrase,
  });

  final Set<String> terms;
  final String phrase;
}

const _matchCategories = [
  _IngredientMatchCategory(
    terms: {
      'bovine',
      'beef',
      'collagen',
      'gelatine',
      'gelatin',
      'veal',
      'suet',
      'tallow',
    },
    phrase: 'ingredients derived from cattle (such as beef collagen or gelatine)',
  ),
  _IngredientMatchCategory(
    terms: {
      'pork',
      'bacon',
      'ham',
      'pancetta',
      'prosciutto',
      'salami',
      'lard',
    },
    phrase: 'pork or pork-derived ingredients',
  ),
  _IngredientMatchCategory(
    terms: {
      'chicken',
      'turkey',
      'duck',
      'goose',
      'poultry',
      'poultry fat',
      'poultry stock',
    },
    phrase: 'chicken or other poultry',
  ),
  _IngredientMatchCategory(
    terms: {
      'lamb',
      'rabbit',
      'venison',
      'boar',
      'hare',
      'meat',
      'meat extract',
      'meat stock',
      'offal',
      'liver',
      'blood',
      'sausage',
    },
    phrase: 'meat or meat-derived ingredients',
  ),
  _IngredientMatchCategory(
    terms: {
      'fish',
      'fish oil',
      'fish sauce',
      'fish extract',
      'fish stock',
      'salmon',
      'tuna',
      'cod',
      'haddock',
      'hake',
      'herring',
      'mackerel',
      'trout',
      'sardine',
      'sardines',
      'anchovy',
      'anchovies',
    },
    phrase: 'fish or fish-derived ingredients',
  ),
  _IngredientMatchCategory(
    terms: {
      'prawn',
      'prawns',
      'shrimp',
      'crab',
      'lobster',
      'mollusc',
      'mollusk',
      'oyster',
      'scallop',
      'squid',
      'calamari',
      'octopus',
      'shellfish',
    },
    phrase: 'shellfish or other seafood',
  ),
  _IngredientMatchCategory(
    terms: {
      'milk',
      'whey',
      'butter',
      'cream',
      'cheese',
      'casein',
      'lactose',
      'milk powder',
      'milk solids',
      'skimmed milk',
      'semi-skimmed milk',
      'whole milk',
      'whey powder',
      'yogurt',
      'yoghurt',
      'lactoglobulin',
      'lactalbumin',
      'paneer',
      'quark',
      'fromage',
      'custard',
      'buttermilk',
      'ghee',
      'curds',
      'parmesan',
      'parmigiano reggiano',
      'grana padano',
    },
    phrase: "cow's milk or other dairy",
  ),
  _IngredientMatchCategory(
    terms: {
      'egg',
      'eggs',
      'egg white',
      'egg yolk',
      'albumen',
      'mayonnaise',
    },
    phrase: 'eggs or egg-derived ingredients',
  ),
  _IngredientMatchCategory(
    terms: {'honey', 'beeswax', 'bee wax'},
    phrase: 'honey or beeswax',
  ),
  _IngredientMatchCategory(
    terms: {'carmine', 'cochineal', 'e120', 'shellac', 'e904'},
    phrase: 'ingredients derived from insects (such as carmine or shellac)',
  ),
  _IngredientMatchCategory(
    terms: {
      'animal rennet',
      'animal fat',
      'animal fats',
      'isinglass',
      'lanolin',
    },
    phrase: 'animal-derived ingredients',
  ),
];

List<String> describeClassificationMatches(
  List<String> matchedNonVegetarian,
  List<String> matchedNonVegan,
) {
  final terms = [...matchedNonVegetarian, ...matchedNonVegan]
      .map((term) => term.toLowerCase())
      .toSet()
      .toList();
  return _describeMatchedTerms(terms);
}

List<String> _describeMatchedTerms(List<String> terms) {
  if (terms.isEmpty) {
    return const [];
  }

  final remaining = terms.toSet();
  final messages = <String>[];

  for (final category in _matchCategories) {
    if (remaining.any(category.terms.contains)) {
      messages.add('This product contains ${category.phrase}.');
      remaining.removeWhere(category.terms.contains);
    }
  }

  for (final term in remaining.toList()..sort()) {
    messages.add('This product contains ${_humanizeTerm(term)}.');
  }

  return messages;
}

String _humanizeTerm(String term) {
  return switch (term) {
    'e471' => 'E471 (mono- and diglycerides of fatty acids)',
    'e472' => 'E472 (esters of mono- and diglycerides)',
    _ => term,
  };
}

String describeUncertainResult(DietClassification definitiveClassification) {
  return switch (definitiveClassification) {
    DietClassification.vegan =>
      'This product is likely vegan, but some ingredients may come from animal sources. '
          'Check the label if you need to be sure.',
    DietClassification.vegetarian =>
      'This product is likely vegetarian, but some ingredients may come from animal sources. '
          'Check the label if you need to be sure.',
    _ =>
      'Some ingredients could not be confirmed. Check the label if you need to be sure.',
  };
}
