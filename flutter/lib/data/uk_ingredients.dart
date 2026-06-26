import '../models/ambiguous_ingredient.dart';

class UkIngredients {
  UkIngredients._();

  static const nonVegetarian = {
    'anchovy',
    'anchovies',
    'bacon',
    'beef',
    'blood',
    'boar',
    'bovine',
    'collagen',
    'calamari',
    'carmine',
    'chicken',
    'cochineal',
    'cod',
    'crab',
    'duck',
    'e120',
    'e904',
    'fish',
    'gelatine',
    'gelatin',
    'goose',
    'haddock',
    'hake',
    'ham',
    'hare',
    'herring',
    'lamb',
    'lard',
    'liver',
    'mackerel',
    'meat',
    'mollusc',
    'mollusk',
    'octopus',
    'offal',
    'oyster',
    'pancetta',
    'pork',
    'prawn',
    'prawns',
    'prosciutto',
    'rabbit',
    'salami',
    'salmon',
    'sardine',
    'sardines',
    'sausage',
    'scallop',
    'shellfish',
    'shrimp',
    'squid',
    'suet',
    'tallow',
    'trout',
    'tuna',
    'turkey',
    'veal',
    'venison',
    'animal rennet',
    'animal fat',
    'animal fats',
    'fish oil',
    'fish sauce',
    'fish extract',
    'fish stock',
    'meat extract',
    'meat stock',
    'poultry',
    'poultry fat',
    'poultry stock',
    'shellac',
    'isinglass',
    'lanolin',
  };

  static const vegetarianOnly = {
    'albumen',
    'beeswax',
    'bee wax',
    'butter',
    'buttermilk',
    'casein',
    'cheese',
    'cream',
    'curds',
    'egg',
    'eggs',
    'egg white',
    'egg yolk',
    'ghee',
    'honey',
    'lactose',
    'milk',
    'milk powder',
    'milk solids',
    'skimmed milk',
    'semi-skimmed milk',
    'whole milk',
    'whey',
    'whey powder',
    'yogurt',
    'yoghurt',
    'lactoglobulin',
    'lactalbumin',
    'paneer',
    'quark',
    'fromage',
    'custard',
    'mayonnaise',
    'parmesan',
    'parmigiano reggiano',
    'grana padano',
  };

  static const veganExceptions = {
    'almond milk',
    'oat milk',
    'soya milk',
    'soy milk',
    'coconut milk',
    'rice milk',
    'cashew milk',
    'hazelnut milk',
    'pea milk',
    'plant milk',
    'plant-based milk',
    'vegan cheese',
    'vegan butter',
    'vegan cream',
    'vegan mayonnaise',
    'vegan mayo',
    'cocoa butter',
    'shea butter',
    'mango butter',
    'nut butter',
    'peanut butter',
    'almond butter',
    'cashew butter',
    'coconut cream',
    'coconut butter',
    'egg-free',
    'egg free',
    'dairy-free',
    'dairy free',
    'plant-based',
    'plant based',
    'microbial rennet',
    'vegetarian rennet',
    'non-animal rennet',
    'vegetarian parmesan',
    'vegan parmesan',
    'vegan hard cheese',
  };

  static final nonVegan = {...nonVegetarian, ...vegetarianOnly};

  static const ambiguous = [
    AmbiguousIngredient(
      term: 'parmigiano reggiano',
      message:
          "Contains milk (not vegan). Traditional Parmigiano Reggiano is also made with animal rennet, which is not vegetarian.",
      mayBeNonVegetarian: true,
    ),
    AmbiguousIngredient(
      term: 'grana padano',
      message:
          'Contains milk (not vegan). Grana Padano is often made with animal rennet, which is not vegetarian.',
      mayBeNonVegetarian: true,
    ),
    AmbiguousIngredient(
      term: 'parmesan',
      message:
          'Contains milk (not vegan). Parmesan-style hard cheese is often made with animal rennet unless labelled vegetarian.',
      mayBeNonVegetarian: true,
    ),
    AmbiguousIngredient(
      term: 'rennet',
      message:
          'Rennet may be animal, fungal, or microbial; only animal rennet is non-vegetarian.',
      mayBeNonVegetarian: true,
    ),
    AmbiguousIngredient(
      term: 'mono- and diglycerides',
      message: 'Emulsifiers may be derived from animal or plant fats.',
    ),
    AmbiguousIngredient(
      term: 'monoglycerides',
      message: 'Emulsifiers may be derived from animal or plant fats.',
    ),
    AmbiguousIngredient(
      term: 'diglycerides',
      message: 'Emulsifiers may be derived from animal or plant fats.',
    ),
    AmbiguousIngredient(
      term: 'e471',
      message:
          'E471 (mono- and diglycerides of fatty acids) may be animal or plant derived.',
    ),
    AmbiguousIngredient(
      term: 'e472',
      message:
          'E472 esters of mono- and diglycerides may be animal or plant derived.',
    ),
    AmbiguousIngredient(
      term: 'glycerol',
      message: 'Glycerol may be derived from animal fats or plants.',
    ),
    AmbiguousIngredient(
      term: 'glycerin',
      message: 'Glycerin may be derived from animal fats or plants.',
    ),
    AmbiguousIngredient(
      term: 'glycerine',
      message: 'Glycerine may be derived from animal fats or plants.',
    ),
    AmbiguousIngredient(
      term: 'e422',
      message: 'E422 (glycerol) may be derived from animal fats or plants.',
    ),
    AmbiguousIngredient(
      term: 'stearic acid',
      message: 'Stearic acid may be derived from animal or plant sources.',
    ),
    AmbiguousIngredient(
      term: 'e570',
      message: 'E570 (fatty acids) may be derived from animal or plant sources.',
    ),
    AmbiguousIngredient(
      term: 'l-cysteine',
      message:
          'L-cysteine is often animal-derived but can be synthetic or plant derived.',
    ),
    AmbiguousIngredient(
      term: 'e920',
      message:
          'E920 (L-cysteine) is often animal-derived but can be synthetic or plant derived.',
    ),
    AmbiguousIngredient(
      term: 'natural flavourings',
      message:
          'Natural flavourings may include animal-derived ingredients and are not fully declared.',
    ),
    AmbiguousIngredient(
      term: 'natural flavorings',
      message:
          'Natural flavorings may include animal-derived ingredients and are not fully declared.',
    ),
    AmbiguousIngredient(
      term: 'flavourings',
      message:
          'Flavourings may include animal-derived ingredients and are not fully declared.',
    ),
    AmbiguousIngredient(
      term: 'flavorings',
      message:
          'Flavorings may include animal-derived ingredients and are not fully declared.',
    ),
    AmbiguousIngredient(
      term: 'aroma',
      message: 'Aroma or flavouring components may be animal derived.',
    ),
    AmbiguousIngredient(
      term: 'cholecalciferol',
      message:
          'Vitamin D3 is often from lanolin (sheep wool) unless labelled as vegan.',
    ),
    AmbiguousIngredient(
      term: 'vitamin d3',
      message:
          'Vitamin D3 is often from lanolin (sheep wool) unless labelled as vegan.',
    ),
    AmbiguousIngredient(
      term: 'vitamin d',
      message:
          'Vitamin D may be D2 (usually vegan) or D3 (often from lanolin).',
    ),
    AmbiguousIngredient(
      term: 'sugar',
      message:
          'Some refined sugar is processed with bone char; UK labels rarely declare this.',
    ),
    AmbiguousIngredient(
      term: 'wine',
      message: 'Wine may be fined using egg, milk, fish, or gelatine.',
      mayBeNonVegetarian: true,
    ),
    AmbiguousIngredient(
      term: 'wine vinegar',
      message: 'Wine vinegar may come from wine fined using animal products.',
      mayBeNonVegetarian: true,
    ),
    AmbiguousIngredient(
      term: 'cider',
      message: 'Cider may be fined using animal-derived finings.',
      mayBeNonVegetarian: true,
    ),
    AmbiguousIngredient(
      term: 'beer',
      message:
          'Beer may be fined using isinglass or other animal-derived finings.',
      mayBeNonVegetarian: true,
    ),
    AmbiguousIngredient(
      term: 'e481',
      message:
          'E481 (sodium stearoyl lactylate) may be animal or plant derived.',
    ),
    AmbiguousIngredient(
      term: 'e482',
      message:
          'E482 (calcium stearoyl lactylate) may be animal or plant derived.',
    ),
    AmbiguousIngredient(
      term: 'e631',
      message: 'E631 (disodium inosinate) may be animal or plant derived.',
    ),
    AmbiguousIngredient(
      term: 'e635',
      message:
          "E635 (disodium 5'-ribonucleotides) may be animal or plant derived.",
    ),
  ];
}
