import 'package:flutter_test/flutter_test.dart';
import 'package:food_classifier/food_classifier.dart';

void main() {
  test('extractsTextAfterIngredientsHeader', () {
    const text = '''
Product name
Ingredients: water, oats, sunflower oil, salt
Allergens: may contain nuts
''';

    final extracted = IngredientsTextExtractor.extract(text);
    expect(extracted.contains('water'), isTrue);
    expect(extracted.contains('oats'), isTrue);
    expect(extracted.toLowerCase().contains('may contain'), isFalse);
  });

  test('handlesOcrMisspelledIngredientsHeader', () {
    const text = '''
Marketing copy about sun protection
oreredients: Aqua, Glycerin, Alcohol Denat.
Art-No. 85581
''';

    final extracted = IngredientsTextExtractor.extract(text);
    expect(extracted.contains('Aqua'), isTrue);
    expect(extracted.contains('Glycerin'), isTrue);
    expect(extracted.toLowerCase().contains('art-no'), isFalse);
    expect(extracted.toLowerCase().contains('marketing'), isFalse);
  });

  test('extractsOnlyIngredientsFromSunscreenLabel', () {
    const text = '''
CITRACELL-PROTECT dermatologically tested
IMPORTANT USAGE INSTRUCTIONS: Apply generously
oreredients: Aqua, Isopropyl Palmitate, Glycerin, Alcohol Denat., Phenoxyethanol, Parfum
4 005808423040S Art-No. 85581 200 ml e Beiersdorf UK Ltd.
''';

    final extracted = IngredientsTextExtractor.extract(text);
    expect(extracted.contains('Aqua'), isTrue);
    expect(extracted.contains('Phenoxyethanol'), isTrue);
    expect(extracted.contains('Parfum'), isTrue);
    expect(extracted.toUpperCase().contains('USAGE'), isFalse);
    expect(extracted.contains('Beiersdorf'), isFalse);
    expect(extracted.contains('85581'), isFalse);
    expect(extracted.contains('005808423040'), isFalse);
  });

  test('returnsEmptyWhenNoIngredientsHeaderFound', () {
    final extracted = IngredientsTextExtractor.extract('water, oats, sunflower oil');
    expect(extracted, '');
  });

  test('extractsParentheticalAllergenStyleMilk', () {
    const text = '''
Ingredients: Protein Blend [Calcium Caseinate (Milk), Whey Protein Isolate (Milk)]
''';

    final parsed = IngredientLabelParser.parse(text);
    expect(parsed.chips.any((chip) => chip.contains('Protein Blend')), isTrue);
    expect(
      parsed.chips.any((chip) => chip.toLowerCase().contains('milk')),
      isTrue,
    );
  });

  test('stopsAtAllergyAdvice', () {
    const text = '''
Ingredients: Protein Blend [Calcium Caseinate (Milk)], Cocoa
Allergy advice: Contains milk. May contain nuts.
Nutrition information: Energy 200kJ
''';

    final parsed = IngredientLabelParser.parse(text);
    expect(parsed.chips.any((chip) => chip.contains('Protein Blend')), isTrue);
    expect(parsed.chips.any((chip) => chip.contains('Cocoa')), isTrue);
    expect(parsed.displayText.toLowerCase().contains('allergy'), isFalse);
    expect(parsed.displayText.toLowerCase().contains('nutrition'), isFalse);
  });

  test('stopsAtAllergenInformation', () {
    const text = '''
Ingredients: oats, sugar, salt
 Allergen information: see ingredients in bold
''';

    final parsed = IngredientLabelParser.parse(text);
    expect(parsed.chips, ['oats', 'sugar', 'salt']);
  });

  test('stopsAtAllergyAdviceOnSameLine', () {
    const text = 'Ingredients: water, salt Allergy advice: Contains celery';

    final parsed = IngredientLabelParser.parse(text);
    expect(parsed.chips, ['water', 'salt']);
  });

  test('ignoresIngredientsMentionInsideAllergyAdvice', () {
    const text = '''
Protein Blend [Calcium Caseinate (Milk)], Cocoa
Allergy advice: Contains milk. For allergens see ingredients in bold.
Nutrition information: Energy 200kJ
''';

    final parsed = IngredientLabelParser.parse(text);
    expect(parsed.chips.any((chip) => chip.contains('Protein Blend')), isTrue);
    expect(parsed.chips.any((chip) => chip.contains('Cocoa')), isTrue);
    expect(parsed.displayText.toLowerCase().contains('bold'), isFalse);
    expect(parsed.displayText.toLowerCase().contains('nutrition'), isFalse);
  });

  test('usesTextBeforeAllergyAdviceWhenHeaderMissing', () {
    const text = '''
Protein Blend [Calcium Caseinate (Milk)], Cocoa
Allergy advice: Contains milk
''';

    final parsed = IngredientLabelParser.parse(text);
    expect(parsed.chips.any((chip) => chip.contains('Protein Blend')), isTrue);
    expect(parsed.chips.any((chip) => chip.contains('Cocoa')), isTrue);
  });

  test('extractsSoftCheeseLabelDespiteMergedNutritionColumn', () {
    const ocrText =
        'REDUCED FAT SOFT CHEESE NUTRITION:Typical v 632kJ/152kcal. Fat 10 '
        '(arbohydrate 3.8a, of Protein 95a, Sat 070 Energy 190k/46kcal '
        'NGREDIENTS: Reduced Fat Soft C(hese (Milk), Salt Stablisers: '
        'Guar Gum, Carrageenan; Citrus Fibre';

    final parsed = IngredientLabelParser.parse(ocrText);
    expect(parsed.chips.contains('Reduced Fat Soft Cheese (Milk)'), isTrue);
    expect(parsed.chips.contains('Salt'), isTrue);
    expect(parsed.chips.contains('Guar Gum'), isTrue);
    expect(parsed.chips.contains('Carrageenan'), isTrue);
    expect(parsed.chips.contains('Citrus Fibre'), isTrue);
  });
}
