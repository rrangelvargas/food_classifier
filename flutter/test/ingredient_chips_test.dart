import 'package:flutter_test/flutter_test.dart';
import 'package:food_classifier/food_classifier.dart';

void main() {
  test('parsesGarbledCameraOcrIntoMultipleChips', () {
    const garbled =
        'B-NMGREDIENTS:Protein Blend (Calcium Caseinate (MUK, NL-IM Whey Protein Isolate Milk;; '
        'Sweeteners: Maltitol, Sucralose; Bovine Collagen Hydrolysate;; '
        'Humectant: Glycerol; Water, PRln, Cocoa Butter, Whole Milk Powder;; '
        'Emulsifier Soy Lecithin; Panuings. ALLERGN VICE,';

    final chips = parseIngredientsToChips(garbled);

    expect(chips.length > 5, isTrue, reason: 'Expected multiple chips but got: $chips');
    expect(chips.any((chip) => chip.toLowerCase().contains('maltitol')), isTrue);
    expect(chips.any((chip) => chip.toLowerCase().contains('glycerol')), isTrue);
    expect(chips.any((chip) => chip.toLowerCase().contains('collagen')), isTrue);
    expect(
      chips.any((chip) => chip.toLowerCase().contains('b-nmgredients')),
      isFalse,
    );
  });

  test('parsesMultilineExtractedIngredientsIntoChips', () {
    final chips = parseIngredientsToChips('''
Reduced Fat Soft Cheese (Milk),
Salt,
Stabilisers: Guar Gum;
Carrageenan,
''');
    expect(chips, hasLength(4));
    expect(chips[0], 'Reduced Fat Soft Cheese (Milk)');
    expect(chips[2], 'Guar Gum');
  });

  test('stripsFunctionalLabelsFromChips', () {
    final chips = parseIngredientsToChips(
      'Sweeteners: Maltitol, Sucralose, Emulsifier: Soy Lecithin, Acidity Regulator: Sodium Hydroxide',
    );
    expect(
      chips,
      ['Maltitol', 'Sucralose', 'Soy Lecithin', 'Sodium Hydroxide'],
    );
  });

  test('formatsChipsForClassification', () {
    expect(
      formatIngredientsFromChips(['water', 'oats', 'salt']),
      'water, oats, salt',
    );
  });
}
