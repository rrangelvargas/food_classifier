import '../ingredient_text.dart';
import 'regex_utils.dart';

class ChipOcrCorrector {
  ChipOcrCorrector._();

  static final _leadingParenMissingLetter = ic(r'^\(([a-z])([a-z]*)');
  static final _digitSixBeforeVowelWord = RegExp(r'\b6(?=[aeiou][a-z])');
  static final _nonAsciiLetterSuffix = RegExp(r'[\u0080-\uFFFF]+$');
  static final _embeddedFunctionalLabelBeforeColon = ic(
    r',\s*(?=(?:Sweeteners|Humectant|Bulking|Emulsifier|Raising|Acidity)\w*\s*:)',
  );
  static final _functionalLabelTail = ic(
    r'\s+(?:Paising|Raising|Kaising)\w*\s*Agents?\b.*$',
  );
  static final _saltGarbleTail =
      ic(r'\s+(?:polydextros|polydextrose)\s+a$');
  static final _ocrLetterNoise = RegExp(r'[şğı]');
  static final _dutchGarbagePrefix =
      ic(r'^(?:rundercolla\w*|rijsmiddele\s+)');
  static final _commonAdditiveFixes = [
    (ic(r'^Carageenan$'), 'Carrageenan'),
    (ic(r'^Guar\s+Gum$'), 'Guar Gum'),
    (ic(r'^Citrus\s+Fibre$'), 'Citrus Fibre'),
    (ic(r'^Cabonates$'), 'Carbonates'),
    (ic(r'^Sodium\s+Cabonates$'), 'Sodium Carbonates'),
  ];

  static List<String> splitMergedChip(String chip) {
    if (chip.trim().isEmpty) {
      return const [];
    }

    var working = _stripEmbeddedFunctionalLabel(chip);
    working = working.replaceAll(_functionalLabelTail, '').trim();
    working = working.replaceAll(_ocrLetterNoise, '');

    if (_embeddedFunctionalLabelBeforeColon.hasMatch(working)) {
      return working
          .split(_embeddedFunctionalLabelBeforeColon)
          .map((part) => part.trim().replaceAll(RegExp(r'[,.;:]+$'), ''))
          .where((part) => part.isNotEmpty)
          .toList();
    }

    if (_saltGarbleTail.hasMatch(working)) {
      final ingredient = working.replaceAll(_saltGarbleTail, '').trim();
      return [ingredient, 'Salt']
          .expand((part) => splitRespectingBrackets(part.replaceAll('\n', ',')))
          .map((part) => part.trim().replaceAll(RegExp(r'[,.;:]+$'), ''))
          .where((part) => part.isNotEmpty)
          .toList();
    }

    if (ic(r'\s+Salt$').hasMatch(working)) {
      final ingredient =
          working.replaceAll(ic(r'\s+Salt$'), '').trim();
      return [ingredient, 'Salt'].where((part) => part.isNotEmpty).toList();
    }

    return splitRespectingBrackets(working.replaceAll('\n', ','))
        .map((part) => part.trim().replaceAll(RegExp(r'[,.;:]+$'), ''))
        .where((part) => part.isNotEmpty)
        .toList();
  }

  static String correct(String chip) {
    if (chip.trim().isEmpty) {
      return chip;
    }

    var result = chip.trim();
    result = _stripEmbeddedFunctionalLabel(result);
    result = _fixLetterReplacedByParen(result);
    result = result.replaceAll(_digitSixBeforeVowelWord, 'G');
    result = result.replaceAll(_nonAsciiLetterSuffix, '');
    result = result.replaceAll(_ocrLetterNoise, '');
    result = result.replaceAll(_dutchGarbagePrefix, '');
    result = result.replaceAll(ic(r'^polydextros\s+a$'), 'Salt');
    result = result.replaceAll(
      ic(r'^Palm\s+(?:Di|b[lid])$'),
      'Palm Oil',
    );
    result = result.replaceAll(
      ic(r'^rundercolla\w*\s+Palm\s+(?:Di|b[lid])$'),
      'Palm Oil',
    );
    result = result.replaceAllMapped(
      ic(r'^Fat-reduced\s+water\s+(?:palr\s+)?Cocoa Powder\s*(\(\d+%\))?$'),
      (match) {
        final percent = match.group(1);
        if (percent != null && percent.isNotEmpty) {
          return 'Fat-reduced Cocoa Powder $percent';
        }
        return 'Fat-reduced Cocoa Powder';
      },
    );
    result = result.replaceAll(
      ic(r'^Sodium\s+(?:rijsmiddele\s+)?(?:Cabonates|Carbonates|\w*\s*nates)$'),
      'Sodium Carbonates',
    );
    for (final (pattern, replacement) in _commonAdditiveFixes) {
      if (pattern.hasMatch(result)) {
        result = replacement;
      }
    }
    result = _fixSkyrLabelGarbles(result);
    return result.trim();
  }

  static String _fixSkyrLabelGarbles(String text) {
    var result = text;
    result = result.replaceAll(ic(r'^wary maize\b'), 'waxy maize');
    result = result.replaceAll(ic(r'^natural$'), 'natural flavouring');
    result = result.replaceAllMapped(
      ic(r'^concentrate (\d+)\.(\d)(?:3|8)\b'),
      (match) =>
          'from concentrate ${match.group(1)}.${match.group(2)}%',
    );
    result = result.replaceAllMapped(
      ic(r'\bfrom concentrate (\d+)\.(\d)(?:3|8)\b'),
      (match) =>
          'from concentrate ${match.group(1)}.${match.group(2)}%',
    );
    return result;
  }

  static String _stripEmbeddedFunctionalLabel(String chip) {
    final colonIndex = chip.indexOf(':');
    if (colonIndex <= 0) {
      return chip;
    }
    final label = chip.substring(0, colonIndex).trim();
    if (!isFunctionalLabel(label)) {
      return chip;
    }
    return chip.substring(colonIndex + 1).trim();
  }

  static String _fixLetterReplacedByParen(String text) {
    return text.replaceAllMapped(_leadingParenMissingLetter, (match) {
      final letter = match.group(1)!;
      return 'C${letter.toUpperCase()}${match.group(2)!}';
    });
  }
}
