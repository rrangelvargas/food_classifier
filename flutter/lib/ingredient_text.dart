import 'text/regex_utils.dart';
import 'text/ocr_normalizer.dart';

String normalizeBrackets(String text) {
  return text
      .replaceAll('（', '(')
      .replaceAll('）', ')')
      .replaceAll('［', '[')
      .replaceAll('］', ']')
      .replaceAll('｛', '{')
      .replaceAll('｝', '}');
}

List<String> splitRespectingBrackets(String text) {
  final parts = <String>[];
  final current = StringBuffer();
  var roundDepth = 0;
  var squareDepth = 0;

  for (var index = 0; index < text.length; index++) {
    final char = text[index];
    if (char == '(') {
      roundDepth++;
      current.write(char);
    } else if (char == ')') {
      roundDepth = roundDepth > 0 ? roundDepth - 1 : 0;
      current.write(char);
    } else if (char == '[') {
      squareDepth++;
      current.write(char);
    } else if (char == ']') {
      squareDepth = squareDepth > 0 ? squareDepth - 1 : 0;
      current.write(char);
    } else if (char == ',' && roundDepth == 0 && squareDepth == 0) {
      parts.add(current.toString());
      current.clear();
    } else {
      current.write(char);
    }
  }

  if (current.isNotEmpty) {
    parts.add(current.toString());
  }

  return parts;
}

List<String> extractBracketedContent(String text, String open, String close) {
  final results = <String>[];
  var depth = 0;
  var start = -1;

  for (var index = 0; index < text.length; index++) {
    final char = text[index];
    if (char == open) {
      if (depth == 0) {
        start = index + 1;
      }
      depth++;
    } else if (char == close) {
      depth--;
      if (depth == 0 && start >= 0) {
        final content = text.substring(start, index);
        results.add(content);
        results.addAll(extractBracketedContent(content, '(', ')'));
        results.addAll(extractBracketedContent(content, '[', ']'));
        start = -1;
      }
    }
  }

  return results;
}

String normalizeIngredientForMatching(String value) {
  return value
      .trim()
      .toLowerCase()
      .replaceAll(RegExp(r'\s+'), ' ')
      .replaceAll(RegExp(r'^[ ,.;:]+|[ ,.;:]+$'), '');
}

const functionalLabelPrefixes = [
  'sweeteners',
  'sweetener',
  'humectant',
  'bulking agent',
  'emulsifier',
  'raising agents',
  'raising agent',
  'acidity regulator',
  'stabilisers',
  'stabilizers',
  'stablisers',
  'preservative',
  'antioxidant',
  'colour',
  'color',
  'thickener',
  'gelling agent',
];

final _functionalLabelOcrPatterns = [
  ic(r'^stabl\w*'),
  ic(r'^sabil\w*'),
  ic(r'\w*abilis\w*'),
  ic(r'sweetener'),
  ic(r'humectant'),
  ic(r'bulking\s*\S*\s*agen'),
  ic(r'emulsif'),
  ic(r'(?:rai[sz]|kais)\w*\s*agen'),
  ic(r'acidity\s*reg\w*'),
  ic(r'preserv'),
  ic(r'antioxid'),
  ic(r'thickener'),
  ic(r'gelling\s*agent'),
  ic(r'colou?r'),
];

bool isFunctionalLabel(String label) {
  final lower = label.toLowerCase().trim().replaceAll(RegExp(r'[.:,;]+$'), '');
  if (lower.isEmpty) {
    return false;
  }
  if (lower.contains(',') || lower.contains('(') || lower.contains('[')) {
    return false;
  }
  if (lower.length > 32) {
    return false;
  }
  if (functionalLabelPrefixes.any((prefix) {
    return lower == prefix ||
        lower.startsWith(prefix) ||
        prefix.startsWith(lower) ||
        _removeSuffix(lower, 's') == _removeSuffix(prefix, 's');
  })) {
    return true;
  }
  return _functionalLabelOcrPatterns.any((pattern) => pattern.hasMatch(lower)) ||
      (lower.contains('acidity') &&
          (lower.contains('reg') || lower.contains('req')));
}

bool isFunctionalLabelClause(String clause) {
  final colonIndex = clause.indexOf(':');
  final label =
      colonIndex >= 0 ? clause.substring(0, colonIndex) : clause;
  return isFunctionalLabel(label);
}

List<String> expandFunctionalLabelToIngredients(String segment) {
  final trimmed = segment.trim().replaceAll(RegExp(r'[,.;:]+$'), '');
  if (trimmed.isEmpty) {
    return const [];
  }

  final colonIndex = trimmed.indexOf(':');
  if (colonIndex >= 0) {
    final label = trimmed.substring(0, colonIndex).trim();
    final remainder = trimmed.substring(colonIndex + 1).trim();
    if (isFunctionalLabel(label)) {
      if (remainder.isEmpty) {
        return const [];
      }
      return _splitIngredientsAfterFunctionalLabel(remainder);
    }
  }

  final parenStart = trimmed.indexOf('(');
  if (parenStart > 0 && trimmed.endsWith(')')) {
    final label = trimmed.substring(0, parenStart).trim();
    final remainder = trimmed.substring(parenStart + 1, trimmed.length - 1).trim();
    if (isFunctionalLabel(label) && remainder.isNotEmpty) {
      return _splitIngredientsAfterFunctionalLabel(remainder);
    }
  }

  if (isFunctionalLabelClause(trimmed)) {
    return const [];
  }

  return [trimmed];
}

List<String> _splitIngredientsAfterFunctionalLabel(String remainder) {
  final ingredients = <String>[];
  for (final part in splitRespectingBrackets(remainder)) {
    final trimmed = part.trim().replaceAll(RegExp(r'[,.;:]+$'), '');
    if (trimmed.isEmpty) {
      continue;
    }
    if (isFunctionalLabelClause(trimmed)) {
      ingredients.addAll(expandFunctionalLabelToIngredients(trimmed));
    } else {
      ingredients.add(trimmed);
    }
  }
  return ingredients;
}

const _cosmeticFragranceMarkers = {
  'linalool',
  'citronellol',
  'linalyl acetate',
  'parfum',
  'benzyl alcohol',
};

List<String> correctCosmeticOcrChips(List<String> chips) {
  final lowered = chips.map((chip) => chip.toLowerCase()).toList();
  final hasFragranceTail = lowered.any(
        (chip) =>
            _cosmeticFragranceMarkers.any((marker) => chip.contains(marker)),
      ) &&
      lowered.where((chip) => chip.contains('linalool')).length >= 1;

  return chips.map((chip) {
    var corrected = chip;
    if (chip.toLowerCase() == 'lanolin' && hasFragranceTail) {
      corrected = 'Vanillin';
    }
    corrected = corrected.replaceAll(
      ic(r'^Microcrysta[\w"\x27\s]*Wine\s+Cellulose$'),
      'Microcrystalline Cellulose',
    );
    corrected = corrected.replaceAll(
      ic(r'^Wine\s+Cellulose$'),
      'Microcrystalline Cellulose',
    );
    return corrected;
  }).toList();
}

List<String> ingredientSegmentsForMatching(String raw) {
  if (raw.trim().isEmpty) {
    return const [];
  }

  var text = raw.trim();
  text = text.replaceFirst(
    RegExp(r'^ingredients\s*:\s*', caseSensitive: false),
    '',
  );
  text = normalizeBrackets(text);
  text = OcrNormalizer.normalize(text);

  final segments = <String>{
    text,
    ...splitRespectingBrackets(text),
    ...extractBracketedContent(text, '(', ')'),
    ...extractBracketedContent(text, '[', ']'),
  };

  return segments
      .expand(expandFunctionalLabelToIngredients)
      .map(normalizeIngredientForMatching)
      .where((segment) => segment.isNotEmpty)
      .toSet()
      .toList();
}

String _removeSuffix(String value, String suffix) {
  if (value.endsWith(suffix)) {
    return value.substring(0, value.length - suffix.length);
  }
  return value;
}
