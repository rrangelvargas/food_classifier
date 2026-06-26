import 'package:flutter_test/flutter_test.dart';
import 'package:food_classifier/food_classifier.dart';

void main() {
  const skyrOcr =
      'WITH SWEETENER: INGREDIENTS: Lactose free skyr (yogurt (MILK)), grape juice concentrate, '
      'blueberry puree from concentrate 1.8%, waxy maize starch, natural flavouring, '
      'black carrot juice concentrate, hibiscus concentrate, '
      'sweetener (steviol glycosides from stevia), lactase enzymes. '
      'For allergens, see ingredients in BOLD. Suitable for vegetarians. Keep refrigerated.';

  const expectedChips = [
    'Lactose free skyr (yogurt (MILK))',
    'grape juice concentrate',
    'blueberry puree from concentrate 1.8%',
    'waxy maize starch',
    'natural flavouring',
    'black carrot juice concentrate',
    'hibiscus concentrate',
    'steviol glycosides from stevia',
    'lactase enzymes',
  ];

  test('parsesSkyrLabelWithNestedAllergenParens', () {
    final parsed = IngredientLabelParser.parse(skyrOcr);
    expect(parsed.chips, expectedChips);
  });

  test('ignoresAllergyFooterWhenIngredientsHeaderMissing', () {
    const footerOnly =
        'For allergens, see ingredients in BOLD. Suitable for vegetarians. Keep refrigerated.';
    final parsed = IngredientLabelParser.parse(footerOnly);
    expect(parsed.chips, isEmpty);
  });

  test('parsesSkyrWhenHeaderIsFollowedByAllergyLineInOcr', () {
    const garbledOrder =
        'WITH SWEETENER: INGREDIENTS: For allergens, see ingredients in BOLD. '
        'Lactose free skyr (yogurt (MILK)), grape juice concentrate, lactase enzymes. '
        'Suitable for vegetarians.';
    final parsed = IngredientLabelParser.parse(garbledOrder);
    expect(
      parsed.chips.any((chip) => chip.toLowerCase().contains('skyr')),
      isTrue,
    );
    expect(
      parsed.chips.any((chip) => chip.toLowerCase().contains('grape juice')),
      isTrue,
    );
  });

  test('repairsPartialSkyrOcrWithLineBreaks', () {
    const partial = 'INGREDIENTS: Concentrate 1.83\nwary maize starch\nnatural';
    final parsed = IngredientLabelParser.parse(partial);
    expect(
      parsed.chips,
      [
        'from concentrate 1.8%',
        'waxy maize starch',
        'natural flavouring',
      ],
    );
  });

  test('repairsPartialSkyrOcrWithCommas', () {
    const partial = 'Concentrate 1.83, wary maize starch, natural';
    final parsed = IngredientLabelParser.parse(partial);
    expect(
      parsed.chips,
      [
        'from concentrate 1.8%',
        'waxy maize starch',
        'natural flavouring',
      ],
    );
  });
}
