import 'package:flutter_test/flutter_test.dart';
import 'package:food_classifier/food_classifier.dart';

void main() {
  const ocrText =
      'Protein Blend [Calcium Caseinate (Milk]. NL-INGREL Whey Protern Isolate Milk)), '
      'Sweeteners: Maltitol, Sucralose: wei-eiwitiso ne Lolagen Hydrolysate. '
      'Humectant: Glycerol; Water, rundercolla Palm Di, Cocoa Butter: Whole Milk Powder, '
      'Fat-reduced water, palr Cocoa Powder (3%). Wheat Flour. Rapeseed Oil, '
      'Bulking cacaopoed Agen: Polydlextrose: Wheat Starch, Emulsifier: Soy Lecithin; '
      'polydextroe BI, kaising Agents: Ammonium Carbonates, Sodium rijsmiddele nates; '
      'Acidity Requlator: Sodium Hydroxide: zuurtereg ALLERGI glutenbeve Fanurings. '
      'including cereals cee inaredients in bld MaK also ALLERGY AyICE';

  test('extractsEnglishIngredientsFromBilingualOcr', () {
    final parsed = IngredientLabelParser.parse(ocrText);

    expect(parsed.chips.any((chip) => chip.contains('Protein Blend')), isTrue);
    expect(parsed.chips.any((chip) => chip.contains('Calcium Caseinate')), isTrue);
    expect(parsed.chips.any((chip) => chip.toLowerCase().contains('milk')), isTrue);
    expect(
      parsed.chips.any(
        (chip) =>
            chip.toLowerCase().contains('whey protein') ||
            chip.toLowerCase().contains('protern'),
      ),
      isTrue,
    );
    expect(parsed.chips.any((chip) => chip.contains('Maltitol')), isTrue);
    expect(parsed.chips.any((chip) => chip.contains('Sucralose')), isTrue);
    expect(parsed.chips.any((chip) => chip.toLowerCase().contains('collagen')), isTrue);
    expect(parsed.chips.any((chip) => chip.toLowerCase().contains('glycerol')), isTrue);
    expect(parsed.chips.any((chip) => chip.toLowerCase().contains('cocoa butter')), isTrue);
    expect(parsed.chips.any((chip) => chip.toLowerCase().contains('whole milk powder')), isTrue);
    expect(parsed.chips.any((chip) => chip.toLowerCase().contains('wheat flour')), isTrue);
    expect(parsed.chips.any((chip) => chip.toLowerCase().contains('soy lecithin')), isTrue);
    expect(parsed.chips.any((chip) => chip.toLowerCase().contains('ammonium carbonates')), isTrue);
    expect(
      parsed.chips.any(
        (chip) =>
            chip.toLowerCase().contains('sodium carbonates') ||
            chip.toLowerCase().contains('nates'),
      ),
      isTrue,
    );
    expect(parsed.displayText.toLowerCase().contains('nl-ingrel'), isFalse);
    expect(parsed.displayText.toLowerCase().contains('wei-eiwitiso'), isFalse);
    expect(parsed.displayText.toLowerCase().contains('allergy'), isFalse);
  });

  test('classifiesProteinBarAsNeitherDueToBovineCollagen', () {
    final parsed = IngredientLabelParser.parse(ocrText);
    final result = classifyIngredients(parsed.chips);
    expect(result.classification, DietClassification.neither);
    expect(result.matchedNonVegetarian, isNotEmpty);
  });

  test('classifiesParsedProteinBarIngredientsWithGlycerolAndFlavouringsWarnings', () {
    final parsed = IngredientLabelParser.parse(ocrText);
    final result = classifyIngredients(parsed.chips);

    expect(result.warnings.any((warning) => warning.term == 'glycerol'), isTrue);
    expect(result.warnings.any((warning) => warning.term == 'flavourings'), isTrue);
  });
}
