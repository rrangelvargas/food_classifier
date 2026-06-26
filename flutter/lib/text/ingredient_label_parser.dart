import 'allergy_boundary.dart';
import 'foreign_column_filter.dart';
import 'ingredient_tokenizer.dart';
import 'label_boundary_finder.dart';
import 'ocr_normalizer.dart';

class ParsedIngredientLabel {
  const ParsedIngredientLabel({
    required this.chips,
    required this.displayText,
  });

  final List<String> chips;
  final String displayText;
}

class IngredientLabelParser {
  IngredientLabelParser._();

  static ParsedIngredientLabel parse(String rawOcr) {
    final block = LabelBoundaryFinder.findBlock(rawOcr);
    if (block == null) {
      return _empty();
    }
    return parseBlock(block);
  }

  static ParsedIngredientLabel parseBlock(String ingredientsBlock) {
    if (ingredientsBlock.trim().isEmpty) {
      return _empty();
    }

    final cleaned = AllergyBoundary.stripTrailingGarbage(
      AllergyBoundary.trimTail(
        OcrNormalizer.normalize(
          ForeignColumnFilter.strip(ingredientsBlock),
        ),
      ),
    );

    if (cleaned.trim().isEmpty) {
      return _empty();
    }

    final chips = IngredientTokenizer.toChips(cleaned);
    return ParsedIngredientLabel(
      chips: chips,
      displayText: chips.join('\n'),
    );
  }

  static ParsedIngredientLabel _empty() {
    return const ParsedIngredientLabel(chips: [], displayText: '');
  }
}

List<String> parseIngredientsToChips(String raw) {
  return IngredientLabelParser.parseBlock(raw).chips;
}

String formatIngredientsFromChips(List<String> chips) {
  return chips.join(', ');
}
