import 'ingredient_label_parser.dart';

class IngredientsTextExtractor {
  IngredientsTextExtractor._();

  static String extract(String rawText) {
    return IngredientLabelParser.parse(rawText).displayText;
  }
}
