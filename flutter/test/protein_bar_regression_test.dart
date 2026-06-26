import 'package:flutter_test/flutter_test.dart';
import 'package:food_classifier/food_classifier.dart';

void main() {
  const userExtractedText =
      'Protein Blend [Calcium Caseinate (Milk) WheyProtern Isolate (Milk);; '
      'Sweeteners: Maltitol, Sucralose; Bovine Collagen Hydrolysate;; '
      'Humectant: Glycerol; Water, Palm bl, Cocoa Butter, Whole Milk Powder, '
      'Fat- water palr Cocoa Powder (3%), Wheat Flour, Rapeseed Oil, Bulking Agent- ; '
      'Wheat Starch;; Emulsifier: Soy Lecithin a Paising Agents, Ammonium Carbonates, '
      'Sodium Carbonates;; Acidity Regulator: Sodium Hydroxide, Fanurinas ALLERGYVICE,';

  test('stopsExtractionAtAllergyText', () {
    const rawOcr =
        'EN-INGREDIENTS: Protein Blend, Bovine Collagen Hydrolysate, Flavourings. '
        'ALLERGY ADVICE: For allergens see bold.';

    final extracted = IngredientsTextExtractor.extract(rawOcr);

    expect(extracted.toLowerCase().contains('allergy'), isFalse);
    expect(extracted.contains('Bovine Collagen Hydrolysate'), isTrue);
  });

  test('stopsExtractionAtGarbledAllergyVice', () {
    const rawOcr =
        'EN-INGREDIENTS: Protein Blend, Bovine Collagen Hydrolysate, Fanurinas ALLERGYVICE, '
        'For allergens see bold.';

    final parsed = IngredientLabelParser.parse(rawOcr);
    expect(parsed.displayText.toLowerCase().contains('allergy'), isFalse);
    expect(
      parsed.chips.any((chip) => chip.toLowerCase().contains('bovine collagen hydrolysate')),
      isTrue,
    );
    expect(parsed.displayText.toLowerCase().contains('for allergens'), isFalse);
  });

  test('classifiesUserExtractedTextAsNeitherDueToCollagen', () {
    final parsed = IngredientLabelParser.parseBlock(userExtractedText);

    expect(parsed.chips.any((chip) => chip.toLowerCase().contains('collagen')), isTrue);

    final result = classifyIngredients(parsed.chips);
    expect(result.classification, DietClassification.neither);
    expect(
      result.matchedNonVegetarian.any((term) => term == 'bovine' || term == 'collagen'),
      isTrue,
    );
  });

  test('classifiesUserExtractedTextViaStringPath', () {
    final parsed = IngredientLabelParser.parseBlock(userExtractedText);
    final result = classifyIngredients(parsed.chips);

    expect(result.classification, DietClassification.neither);
    expect(result.matchedNonVegetarian, isNotEmpty);
  });
}
