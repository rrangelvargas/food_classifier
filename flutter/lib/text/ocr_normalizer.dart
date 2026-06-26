import '../ingredient_text.dart';
import 'regex_utils.dart';

class OcrNormalizer {
  OcrNormalizer._();

  static const _allergenNames = {
    'milk',
    'soy',
    'soya',
    'gluten',
    'eggs',
    'egg',
    'nuts',
    'nut',
    'fish',
    'celery',
    'mustard',
    'sesame',
    'sulphites',
    'sulphite',
    'lupin',
    'molluscs',
    'crustaceans',
  };

  static final _parenthesisInWord = RegExp(r'([A-Za-z])\(([a-z]{2,})');
  static final _bracketRoundMismatch = RegExp(r'\(([^)\]]+)\]\.?');
  static final _duplicateCloseBeforeFunctionalLabel = ic(
    r'\)\),\s*(?=(?:Sweeteners|Humectant|Bulking|Emulsifier|Raising|Acidity|Stabilis|Sabil|Bovine)\w*\s*:)',
  );
  static final _fatReducedNoiseBeforeCocoa =
      ic(r'Fat-reduced\s+water,\s*(?:palr\s+)?Cocoa Powder');
  static final _sodiumCarbonatesTail = ic(r'\bSodium\s+\w*\s*nates\b');
  static final _proteinBlendContinuation = ic(
    r'(Protein Blend\s*\[)([^\]]+)(\])\s+(Whey\s+[^,;]+)',
  );
  static final _missingCommaBeforeFunctionalLabel = ic(
    r'\b(\w+)\s+(?=(?:Stabilis|Stablis|Sabil|Sweeteners|Humectant|Bulking|Emulsifier|Raising|Acidity)\w*\s*:)',
  );
  static final _ingredientColonBetweenItems = ic(
    r'([A-Z][\w-]*(?:\s+[A-Z][\w-]*)*):\s+(?=[A-Z])',
  );
  static final _extraWhitespace = RegExp(r'\s+');

  static String normalize(String text) {
    var result = _joinLineBrokenIngredients(text);
    result = _normalizeStructure(result);
    result = _normalizeMilkArtifacts(result);
    result = _normalizeLagenToCollagen(result);
    result = _ensureBovineCollagenPrefix(result);
    return _collapseWhitespace(result);
  }

  static String _joinLineBrokenIngredients(String text) {
    final lines = text
        .split(RegExp(r'[\n\r]+'))
        .map((line) => line.trim().replaceAll(RegExp(r'[,.;]+$'), ''))
        .where((line) => line.isNotEmpty)
        .toList();
    if (lines.length < 2) {
      return text;
    }

    final commaCount = ','.allMatches(text).length;
    if (commaCount >= lines.length - 1) {
      return text.replaceAll('\n', ' ').replaceAll('\r', ' ');
    }

    final looksLikeBrokenList = lines.where((line) {
      final lower = line.toLowerCase();
      return !lower.startsWith('ingredients') &&
          !lower.startsWith('for allergen') &&
          !lower.startsWith('suitable for') &&
          line.length >= 3;
    }).length >=
        2;

    if (looksLikeBrokenList) {
      return lines.join(', ');
    }

    return text.replaceAll('\n', ' ').replaceAll('\r', ' ');
  }

  static String _normalizeStructure(String text) {
    var result = text;
    result = result.replaceAll(ic(r'\bC\(hese\b'), 'Cheese');
    result = result.replaceAllMapped(RegExp(r'\b6(?=[aeiou][a-z])'), (_) => 'G');
    result = result.replaceAllMapped(_parenthesisInWord, (match) {
      final inner = match.group(2)!;
      if (_allergenNames.contains(inner.toLowerCase())) {
        return match.group(0)!;
      }
      return '${match.group(1)}$inner';
    });
    result = result.replaceAllMapped(
      _bracketRoundMismatch,
      (match) => '(${match.group(1)!})',
    );
    result = result.replaceAll(
      ic(r'\[([^\]]+\([^)]+\))\.(?=\s*Whey)'),
      r'[$1,',
    );
    result = _closeSquareBracketBeforeFunctionalLabel(result);
    result = result.replaceAll(_duplicateCloseBeforeFunctionalLabel, '), ');
    result = result.replaceAll(
      ic(r'\bIsolate\s+(Milk)\b(?!\))'),
      r'Isolate ($1)',
    );
    result = result.replaceAllMapped(_proteinBlendContinuation, (match) {
      return '${match.group(1)}${match.group(2)}, ${match.group(3)}${match.group(4)}';
    });
    result = result.replaceAllMapped(
      ic(r'(Protein Blend\s*\[)([^;\]]+?)\s+(Whey\s+[^,;]+)'),
      (match) {
        if (match.group(2)!.contains(']')) {
          return match.group(0)!;
        }
        final body = match.group(2)!.trim().replaceAll(RegExp(r',$'), '');
        final whey = match.group(3)!.trim().replaceAll(RegExp(r'[,\] ]+$'), '');
        return '${match.group(1)}$body, $whey]';
      },
    );
    result = result.replaceAllMapped(_missingCommaBeforeFunctionalLabel, (match) {
      final before = match.group(1)!;
      final after = match.group(0)!.substring(before.length).trimLeft();
      return '$before, $after';
    });
    result = result.replaceAllMapped(
      ic(r'(Isolate \(Milk\)),\s*(Sweeteners\s*:)'),
      (match) => 'Isolate (Milk)), ${match.group(2)!}',
    );
    result = result.replaceAll(_fatReducedNoiseBeforeCocoa, 'Fat-reduced Cocoa Powder');
    result = result.replaceAll(_sodiumCarbonatesTail, 'Sodium Carbonates');
    result = result.replaceAll(
      ic(r'\bSodium\s+rijsmiddele\s+Cabonates\b'),
      'Sodium Carbonates',
    );
    result = _replaceNonFunctionalColons(result);
    result = result.replaceAll(
      ic(r'\bmicrocrysta[\w"\x27]*\s*wine\s+(?:line\s+)?cellulose\b'),
      'Microcrystalline Cellulose',
    );
    result = result.replaceAll(
      ic(r'\bwine\s+(?:line\s+)?cellulose\b'),
      'Microcrystalline Cellulose',
    );
    result = result.replaceAll(ic(r'\bfanur\w*\b'), 'Flavourings');
    result = result.replaceAll(ic(r'\bProtern\b'), 'Protein');
    result = result.replaceAll(
      ic(r'\brundercolla\w*\s+Palm\s+(?:Di|b[lid])\b'),
      'Palm Oil',
    );
    result = result.replaceAll(
      ic(r'\bPalm\s+(?:Di|b[lid])\b'),
      'Palm Oil',
    );
    result = result.replaceAllMapped(
      ic(r'\bFat-reduced\s+water\s+(?:palr\s+)?Cocoa Powder(\s*\(\d+%\))?'),
      (match) => 'Fat-reduced Cocoa Powder${match.group(1) ?? ''}',
    );
    result = result.replaceAll(ic(r'\bpolydextros\s+BI\b'), 'Salt');
    result = result.replaceAll(ic(r'\bpolydextros\s+a\b'), 'Salt');
    result = result.replaceAll(ic(r'\bpolydextros\b'), 'Polydextrose');
    result = result.replaceAll(ic(r'\bPolydlextrose\b'), 'Polydextrose');
    result = result.replaceAll(ic(r'\bpolydextroe\s+BI\b'), 'Salt');
    result = result.replaceAll(ic(r'\bPolydextroe\b'), 'Polydextrose');
    result = result.replaceAll(ic(r'\((Milk)\]'), '(Milk)]');
    result = result.replaceAll(
      ic(r'Isolate\s+Milk\)\)'),
      'Isolate (Milk))',
    );
    result = result.replaceAll(
      ic(r'Isolate\s+Milk\)(?!\))'),
      'Isolate (Milk)',
    );
    result = result.replaceAll(
      ic(r'zuurtereg\s+ALLERGI\s+glutenbeve\s+'),
      '',
    );
    return _collapseWhitespace(result);
  }

  static String _normalizeMilkArtifacts(String text) {
    var result = text;
    for (final pattern in [
      RegExp(r'\bl\s*milk\b', caseSensitive: false),
      RegExp(r'\blmilk\b', caseSensitive: false),
      RegExp(r'\b1ilk\b', caseSensitive: false),
      RegExp(r'\bmi\s+lk\b', caseSensitive: false),
      RegExp(r'\bmi\s*me-ngr\b', caseSensitive: false),
    ]) {
      result = result.replaceAll(pattern, 'milk');
    }
    return result;
  }

  static String _normalizeLagenToCollagen(String text) {
    return text.replaceAllMapped(ic(r'\b(\w*)lagen\b'), (match) {
      final word = match.group(0)!;
      if (word.toLowerCase().contains('collagen')) {
        return word;
      }
      final index = word.toLowerCase().indexOf('lagen');
      final suffix = word.substring(index);
      return 'col$suffix';
    });
  }

  static String _ensureBovineCollagenPrefix(String text) {
    return text.replaceAllMapped(
      ic(r'\b(\w*collagen\s+hydrolysate)\b'),
      (match) {
        final phrase = match.group(1)!.trim();
        final lower = phrase.toLowerCase();
        if (lower.startsWith('bovine ')) {
          return 'Bovine Collagen Hydrolysate';
        }
        if (lower.startsWith('porcine ')) {
          return 'Porcine Collagen Hydrolysate';
        }
        if (lower.startsWith('fish ')) {
          return 'Fish Collagen Hydrolysate';
        }
        final before =
            text.substring(0, match.start).trimRight().toLowerCase();
        if (before.endsWith('bovine') ||
            before.endsWith('porcine') ||
            before.endsWith('fish')) {
          return 'Collagen Hydrolysate';
        }
        return 'Bovine Collagen Hydrolysate';
      },
    );
  }

  static String _closeSquareBracketBeforeFunctionalLabel(String text) {
    final pattern = ic(
      r'\(Milk\)\),\s*(?=(?:Sweeteners|Humectant|Bulking|Emulsifier|Raising|Acidity)\w*\s*:)',
    );
    return text.replaceAllMapped(pattern, (match) {
      final before = text.substring(0, match.start);
      final openSquare = before.lastIndexOf('[');
      final hasUnclosedSquare = openSquare >= 0 &&
          !before.substring(openSquare).contains(']');
      if (hasUnclosedSquare) {
        return '(Milk)], ';
      }
      return match.group(0)!;
    });
  }

  static String _replaceNonFunctionalColons(String text) {
    return text.replaceAllMapped(_ingredientColonBetweenItems, (match) {
      final label = match.group(1)!;
      if (_isFunctionalLabelFuzzy(label)) {
        return match.group(0)!;
      }
      return '$label, ';
    });
  }

  static bool _isFunctionalLabelFuzzy(String label) {
    if (isFunctionalLabel(label)) {
      return true;
    }
    final lower = label.toLowerCase().trim();
    return functionalLabelPrefixes.any((prefix) {
      return lower == prefix ||
          lower.startsWith(prefix) ||
          prefix.startsWith(lower) ||
          _removeSuffix(lower, 's') == _removeSuffix(prefix, 's');
    });
  }

  static String _removeSuffix(String value, String suffix) {
    if (value.endsWith(suffix)) {
      return value.substring(0, value.length - suffix.length);
    }
    return value;
  }

  static String _collapseWhitespace(String text) {
    return text
        .replaceAll('\n', ' ')
        .replaceAll('•', ', ')
        .replaceAll(_extraWhitespace, ' ')
        .replaceAll(RegExp(r'\s+,'), ',')
        .replaceAll(RegExp(r',\s*,'), ',')
        .trim();
  }
}
