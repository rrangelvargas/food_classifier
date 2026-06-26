import '../ingredient_text.dart';
import 'allergy_boundary.dart';
import 'chip_ocr_corrector.dart';
import 'regex_utils.dart';

class IngredientTokenizer {
  IngredientTokenizer._();

  static final _ingredientsHeader = ic(r'^[\w-]*?gredients\s*:\s*');
  static final _periodBeforeFunctionalLabel = RegExp(
    r'\.\s+(?=(?:Stabilis|Stablis|Sabil|Sweeteners|Humectant|Bulking|Emulsifier|Raising|Acidity|Bovine)\w*)',
    caseSensitive: false,
  );
  static final _periodBeforeCapitalWord = RegExp(r'\.\s+(?=[A-Z][a-z])');

  static List<String> toChips(String text) {
    if (text.trim().isEmpty) {
      return const [];
    }

    var normalized = AllergyBoundary.trimTail(text.trim());
    normalized = normalized.replaceFirst(_ingredientsHeader, '');

    final chips = _splitIntoClauses(normalized)
        .expand((clause) => splitRespectingBrackets(clause.replaceAll('\n', ',')))
        .expand((segment) => expandFunctionalLabelToIngredients(segment))
        .expand((segment) => ChipOcrCorrector.splitMergedChip(segment))
        .expand((segment) => expandFunctionalLabelToIngredients(segment))
        .map(_cleanChip)
        .where((chip) => chip.isNotEmpty)
        .where((chip) => !_isFunctionalLabelOnly(chip))
        .toList();

    return correctCosmeticOcrChips(chips);
  }

  static List<String> _splitIntoClauses(String text) {
    var prepared = text.replaceAll(_periodBeforeFunctionalLabel, '; ');
    prepared = prepared.replaceAll(_periodBeforeCapitalWord, ', ');
    return prepared.split(RegExp(r'\s*;\s*')).where((part) => part.isNotEmpty).toList();
  }

  static String _cleanChip(String chip) {
    var result = chip.trim().replaceAll(RegExp(r'[,.;:]+$'), '');
    if (_isFunctionalLabelOnly(result)) {
      return '';
    }
    if (ic(r'\bflavourings\b').hasMatch(result)) {
      return 'Flavourings';
    }
    if (ic(r'^polydextrose\s+\w{1,3}$').hasMatch(result)) {
      return '';
    }
    return ChipOcrCorrector.correct(result);
  }

  static bool _isFunctionalLabelOnly(String chip) {
    final label = chip.split(':').first.trim();
    if (isFunctionalLabel(label) || isFunctionalLabel(chip.trim())) {
      return true;
    }
    final lower = label.toLowerCase();
    return lower.contains('acidity') &&
        (lower.contains('reg') || lower.contains('req'));
  }
}
