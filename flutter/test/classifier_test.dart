import 'package:food_classifier/food_classifier.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('Classifier', () {
    test('veganIngredients', () {
      final result = classifyIngredients('water, oats, sunflower oil, salt');
      expect(result.classification, DietClassification.vegan);
      expect(result.hasWarnings, isFalse);
    });

    test('vegetarianIngredients', () {
      final result = classifyIngredients('flour, milk, sugar, butter');
      expect(result.classification, DietClassification.vegetarian);
      expect(result.matchedNonVegan, contains('milk'));
    });

    test('nonVegetarianIngredients', () {
      final result = classifyIngredients('tomatoes, chicken stock, basil');
      expect(result.classification, DietClassification.neither);
      expect(result.matchedNonVegetarian, contains('chicken'));
    });

    test('parmigianoReggianoIsUncertain', () {
      final result =
          classifyIngredients("Parmigiano Reggiano Cheese (Cows' Milk)");
      expect(result.classification, DietClassification.uncertain);
      expect(result.definitiveClassification, DietClassification.vegetarian);
    });

    test('bovineCollagenIsNeither', () {
      final result =
          classifyIngredients('Bovine Collagen Hydrolysate, milk');
      expect(result.classification, DietClassification.neither);
      expect(result.matchedNonVegetarian, contains('bovine'));
    });

    test('parentheticalMilkIsDetected', () {
      final result = classifyIngredients('Whey Protein Isolate (MILK), cocoa');
      expect(result.classification, DietClassification.vegetarian);
      expect(result.matchedNonVegan, contains('milk'));
    });

    test('parentheticalMilkOnlyIsDetected', () {
      final result =
          classifyIngredients('Protein Blend (Calcium Caseinate (Milk))');
      expect(result.classification, DietClassification.vegetarian);
      expect(result.matchedNonVegan, contains('milk'));
    });

    test('squareBracketMilkIsDetected', () {
      final result = classifyIngredients(
        'Protein Blend [Calcium Caseinate (Milk), Whey Protein Isolate (Milk)]',
      );
      expect(result.classification, DietClassification.vegetarian);
      expect(result.matchedNonVegan, contains('milk'));
    });

    test('ocrStyleMilkIsDetected', () {
      final result = classifyIngredients(
        'Protein Blend [Calcium Caseinate Mi ME-NGR, Whey Protein Isolate lMilk]',
      );
      expect(result.classification, DietClassification.vegetarian);
      expect(result.matchedNonVegan, contains('milk'));
    });
  });

  group('ClassificationMessages', () {
    test('describesDairyInPlainLanguage', () {
      final messages = describeClassificationMatches([], ['milk', 'whey']);
      expect(messages, hasLength(1));
      expect(messages.first, contains("cow's milk or other dairy"));
    });

    test('describesBovineInPlainLanguage', () {
      final messages =
          describeClassificationMatches(['bovine', 'collagen'], []);
      expect(messages, hasLength(1));
      expect(messages.first, contains('derived from cattle'));
    });

    test('describesMilkAndBovineSeparately', () {
      final messages = describeClassificationMatches(
        ['bovine'],
        ['milk'],
      );
      expect(messages, hasLength(2));
      expect(messages.any((message) => message.contains('cattle')), isTrue);
      expect(messages.any((message) => message.contains('dairy')), isTrue);
    });

    test('describesChickenInPlainLanguage', () {
      final messages = describeClassificationMatches(['chicken'], []);
      expect(messages, hasLength(1));
      expect(messages.first, contains('poultry'));
    });

    test('describesLikelyVeganWhenUncertain', () {
      final message = describeUncertainResult(DietClassification.vegan);
      expect(message, contains('likely vegan'));
      expect(message, contains('Check the label'));
    });

    test('describesLikelyVegetarianWhenUncertain', () {
      final message = describeUncertainResult(DietClassification.vegetarian);
      expect(message, contains('likely vegetarian'));
      expect(message, contains('Check the label'));
    });
  });
}
