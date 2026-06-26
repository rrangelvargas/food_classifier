import 'package:flutter_test/flutter_test.dart';
import 'package:food_classifier/food_classifier.dart';

void main() {
  test('splitRespectingBracketsKeepsNestedStructure', () {
    final parts = splitRespectingBrackets(
      'Protein Blend [Calcium Caseinate (Milk), Whey Protein Isolate (Milk)]',
    );
    expect(parts, hasLength(1));
    expect(parts.first.contains('['), isTrue);
    expect(parts.first.contains('(Milk)'), isTrue);
  });

  test('extractsMilkFromSquareAndRoundBrackets', () {
    const raw =
        'Protein Blend [Calcium Caseinate (Milk), Whey Protein Isolate (Milk)]';
    final segments = ingredientSegmentsForMatching(raw);
    expect(segments.any((segment) => segment.contains('milk')), isTrue);
  });

  test('repairsOcrMilkArtifacts', () {
    final segments = ingredientSegmentsForMatching(
      'Whey Protein Isolate lMilk, Calcium Caseinate Mi ME-NGR',
    );
    expect(segments.any((segment) => segment.contains('milk')), isTrue);
  });

  test('structuralRepairKeepsWordSpacing', () {
    const input = 'Reduced Fat Soft C(hese (Milk), Salt Stablisers: Guar Gum';
    final structural = OcrNormalizer.normalize(input);
    expect(
      structural,
      'Reduced Fat Soft Cheese (Milk), Salt, Stablisers: Guar Gum',
    );
  });

  test('detectsGarbledStabilisersAsFunctionalLabel', () {
    expect(isFunctionalLabel('Sabilisers'), isTrue);
    expect(
      expandFunctionalLabelToIngredients('Sabilisers: Guar Gum'),
      ['Guar Gum'],
    );
  });

  test('detectsGarbledAcidityRegulatorAsFunctionalLabel', () {
    expect(isFunctionalLabel('Acidity Requlator'), isTrue);
    expect(
      expandFunctionalLabelToIngredients('Acidity Requlator: Sodium Hydroxide'),
      ['Sodium Hydroxide'],
    );
  });

  test('expandsFunctionalLabelWithParentheses', () {
    expect(
      expandFunctionalLabelToIngredients(
        'sweetener (steviol glycosides from stevia)',
      ),
      ['steviol glycosides from stevia'],
    );
  });

  test('parserFormatsIngredientsOnePerLine', () {
    expect(
      IngredientLabelParser.parseBlock(
        'Reduced Fat Soft C(hese (Milk), Salt Stablisers: Guar Gum',
      ).displayText,
      'Reduced Fat Soft Cheese (Milk)\nSalt\nGuar Gum',
    );
  });
}
