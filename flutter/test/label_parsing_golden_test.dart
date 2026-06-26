import 'package:flutter_test/flutter_test.dart';
import 'package:food_classifier/food_classifier.dart';

void main() {
  const softCheeseOcr =
      'REDUCED FAT SOFT CHEESE NUTRITION:Typical v 632kJ/152kcal. Fat 10 '
      '(arbohydrate 3.8a, of Protein 95a, Sat 070 Energy 190k/46kcal '
      'NGREDIENTS: Reduced Fat Soft C(hese (Milk), Salt Stablisers: '
      'Guar Gum, Carrageenan; Citrus Fibre';

  const proteinBarOcr =
      'Protein Blend [Calcium Caseinate (Milk]. NL-INGREL Whey Protern Isolate Milk)), '
      'Sweeteners: Maltitol, Sucralose: wei-eiwitiso ne Lolagen Hydrolysate. '
      'Humectant: Glycerol; Water, rundercolla Palm Di, Cocoa Butter: Whole Milk Powder, '
      'Fat-reduced water, palr Cocoa Powder (3%). Wheat Flour. Rapeseed Oil, '
      'Bulking cacaopoed Agen: Polydlextrose: Wheat Starch, Emulsifier: Soy Lecithin; '
      'polydextroe BI, kaising Agents: Ammonium Carbonates, Sodium rijsmiddele nates; '
      'Acidity Requlator: Sodium Hydroxide: zuurtereg ALLERGI glutenbeve Fanurings. '
      'including cereals cee inaredients in bld MaK also ALLERGY AyICE';

  const expectedSoftCheese = [
    'Reduced Fat Soft Cheese (Milk)',
    'Salt',
    'Guar Gum',
    'Carrageenan',
    'Citrus Fibre',
  ];

  const expectedProteinBar = [
    'Protein Blend [Calcium Caseinate (Milk), Whey Protein Isolate (Milk)]',
    'Maltitol',
    'Sucralose',
    'Bovine Collagen Hydrolysate',
    'Glycerol',
    'Water',
    'Palm Oil',
    'Cocoa Butter',
    'Whole Milk Powder',
    'Fat-reduced Cocoa Powder (3%)',
    'Wheat Flour',
    'Rapeseed Oil',
    'Polydextrose',
    'Wheat Starch',
    'Soy Lecithin',
    'Salt',
    'Ammonium Carbonates',
    'Sodium Carbonates',
    'Sodium Hydroxide',
    'Flavourings',
  ];

  test('parsesSoftCheeseLabelFromCameraOcr', () {
    const cameraOcr =
        'REDUCED FAT SOFT CHEESE NUTRITION:Typical v 632kJ/152kcal '
        'NGREDIENTS: Reduced Fat Soft C(hese (Milk), Salt Sabilisers: '
        '6uar Gum, Carageenan; (itrus Fibre';

    final parsed = IngredientLabelParser.parse(cameraOcr);
    expect(parsed.chips, expectedSoftCheese);
  });

  test('parsesSoftCheeseLabelFromGarbledOcr', () {
    final parsed = IngredientLabelParser.parse(softCheeseOcr);
    expect(parsed.chips, expectedSoftCheese);
    expect(
      parsed.displayText.toLowerCase().contains('stabilis'),
      isFalse,
    );
  });

  test('splitsProteinBlendFromSweetenersWhenSquareBracketClosedWithParen', () {
    final parsed = IngredientLabelParser.parseBlock(
      'Protein Blend [Calcium Caseinate (Milk), Whey Protern Isolate (Milk)), '
      'Sweeteners: Maltitol, Sucralose',
    );
    expect(
      parsed.chips,
      [
        'Protein Blend [Calcium Caseinate (Milk), Whey Protein Isolate (Milk)]',
        'Maltitol',
        'Sucralose',
      ],
    );
  });

  test('parsesProteinBarLabelFromCameraOcr', () {
    const cameraOcr =
        'EN-INGREDIENTS: Protein Blend (Calcium Caseinate (Milk), Whey Protein Isolate (Milk)), '
        'Sweeteners: Maltitol, Sucralose; Bovine Collagen Hydrolysate; Humectant: Glycerol; '
        'Water, rundercollas Palm bl, Cocoa Butter, Whole Milk Powder, '
        'Fat-reduced water palr Cocoa Powder (3%), Wheat Flour, Rapeseed Oil, '
        'Bulking Agent: Polydextrose; Wheat Starch, Emulsifier: Soy Lecithinş polydextros a Paising Agents, '
        'Ammonium Carbonates, Sodium rijsmiddele Cabonates; Acidity Regulator: Sodium Hydroxide; Flavourings. '
        'ALLERGY ADVICE: For allergens see bold.';

    final parsed = IngredientLabelParser.parse(cameraOcr);
    expect(
      parsed.chips,
      [
        'Protein Blend (Calcium Caseinate (Milk), Whey Protein Isolate (Milk))',
        'Maltitol',
        'Sucralose',
        'Bovine Collagen Hydrolysate',
        'Glycerol',
        'Water',
        'Palm Oil',
        'Cocoa Butter',
        'Whole Milk Powder',
        'Fat-reduced Cocoa Powder (3%)',
        'Wheat Flour',
        'Rapeseed Oil',
        'Polydextrose',
        'Wheat Starch',
        'Soy Lecithin',
        'Salt',
        'Ammonium Carbonates',
        'Sodium Carbonates',
        'Sodium Hydroxide',
        'Flavourings',
      ],
    );
  });

  test('parsesProteinBarLabelFromGarbledOcr', () {
    final parsed = IngredientLabelParser.parse(proteinBarOcr);
    expect(parsed.chips, expectedProteinBar);
    expect(parsed.displayText.toLowerCase().contains('sweeteners'), isFalse);
    expect(parsed.displayText.toLowerCase().contains('humectant'), isFalse);
    expect(parsed.displayText.toLowerCase().contains('allergy'), isFalse);
  });

  test('classifiesProteinBarWithGlycerolAndFlavouringsWarnings', () {
    final parsed = IngredientLabelParser.parse(proteinBarOcr);
    final result = classifyIngredients(parsed.chips);

    expect(result.classification, DietClassification.neither);
    expect(result.warnings.any((warning) => warning.term == 'glycerol'), isTrue);
    expect(
      result.warnings.any((warning) => warning.term == 'flavourings'),
      isTrue,
    );
  });
}
