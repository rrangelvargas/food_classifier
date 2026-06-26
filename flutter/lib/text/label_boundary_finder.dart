import '../ingredient_text.dart';
import 'allergy_boundary.dart';
import 'regex_utils.dart';

class _HeaderMatch {
  const _HeaderMatch(this.start, this.end);

  final int start;
  final int end;
}

class LabelBoundaryFinder {
  LabelBoundaryFinder._();

  static final _ingredientsHeaders = [
    ic(r'(?:^|[\n\r])\s*Ingredients\s*:\s*'),
    ic(r'\bIngredients\s*:\s*'),
    ic(r'\bEN-INGREDIENTS?\s*:?\s*'),
    ic(r'\bNGREDIENTS?\s*:?\s*'),
    ic(r'\b[\w-]{0,12}gredients\s*:\s*'),
    ic(r'\bngredients\s*:\s*'),
    ic(r'\b1ngredients\s*:\s*'),
    ic(r'(?:^|[\n\r])\s*oredients\s*:\s*'),
    ic(r'\boredients\s*:\s*'),
    ic(r'(?:^|[\n\r])\s*lngredients\s*:\s*'),
    ic(r'\bComposition\s*:\s*'),
    ic(r'\bINCI\s*:\s*'),
  ];

  static final _explicitIngredientsSection = ic(
    r'\bingredients\s*:\s*(.+?)(?=\bfor\s+allergens\b|\ballergen\s+information\b|\ballergen\b|\bsuitable\s+for\b|\bkeep\s+refrigerated\b|\bnutrition|\ballergy|\bstorage\b|\bmay\s+contain\b|\bimportant\s+usage\b|\busage\s+instructions\b|\bart[.\-]?\s*no\b|\bmade\s+in\b|\bwww\.|\bcontact\b|\bbest\s+before\b|\buse\s+by\b|$)',
    dotAll: true,
  );

  static const _headerFalseStarts = [
    'in bold',
    'in capital',
    'listed',
    'list ',
    'above',
    'below',
    'for allergen',
    'for allerg',
  ];

  static final _sectionEndPatterns = [
    ...AllergyBoundary.sectionEndPatterns,
    ic(r'\bincluding\s+cereals\b'),
    ic(r'\ballergen\s+information\b'),
    ic(r'\ballergen\s+adv[i1l]ce\b'),
    ic(r'\bfor\s+allergens\b'),
    ic(r'\ballergens?\s*:'),
    ic(r'\bmay\s+contain\b'),
    ic(r'\bdietary\s+advice\b'),
    ic(r'\bsuitable\s+for\b'),
    ic(r'\bkeep\s+refrigerated\b'),
    ic(r'\bnutrition(?:al)?\s+information\b'),
    ic(r'\bnutritional\s+values\b'),
    ic(r'\bstorage\b'),
    ic(r'\bimportant\s+usage\b'),
    ic(r'\busage\s+instructions\b'),
    ic(r'\bcontact\b'),
    ic(r'\b00800\b'),
    ic(r'\bbest\s+before\b'),
    ic(r'\buse\s+by\b'),
    ic(r'\bpacked\s+in\b'),
    ic(r'\bart[.\-]?\s*no\b'),
    ic(r'\bmade\s+in\b'),
    ic(r'\bwww\.'),
    RegExp(r'\s\d\s+\d{6,}'),
    RegExp(r'\b\d{8,}\b'),
    ic(r'\b\d+\s*ml\s*e\b'),
  ];

  static final _afterAllergyBoilerplate = ic(
    r'(?:for\s+allergens[^.]*\.|see\s+ingredients\s+in\s+bold\.?)\s*(.+)',
    dotAll: true,
  );

  static String? findBlock(String rawOcr) {
    final text = _normalizeInput(rawOcr);
    if (text.trim().isEmpty) {
      return null;
    }

    final explicit = _extractExplicitIngredientsSection(text);
    if (explicit != null) {
      return explicit;
    }

    final headerMatch = _findBestHeader(text);
    if (headerMatch != null) {
      final start = headerMatch.end + 1;
      final end = _endIndex(text, start) ?? text.length;
      if (start < end) {
        final block = text.substring(start, end).trim();
        if (_isUsefulIngredientBlock(block)) {
          return block;
        }
      }
    }

    final afterBoilerplate = _extractAfterAllergyBoilerplate(text);
    if (afterBoilerplate != null) {
      return afterBoilerplate;
    }

    final bestSpan = _findBestIngredientSpan(text);
    if (bestSpan != null) {
      return bestSpan;
    }

    final sectionEnd = _endIndex(text, 0);
    if (sectionEnd != null && sectionEnd > 0) {
      final candidate = text.substring(0, sectionEnd);
      if (_isUsefulIngredientBlock(candidate)) {
        return candidate;
      }
    }

    return null;
  }

  static String? _extractExplicitIngredientsSection(String text) {
    final matches = _explicitIngredientsSection
        .allMatches(text)
        .map((match) =>
            match.group(1)!.trim().replaceAll(RegExp(r'[.;:]+$'), ''))
        .where(_isUsefulIngredientBlock)
        .toList();
    if (matches.isEmpty) {
      return null;
    }
    matches.sort((a, b) => b.length.compareTo(a.length));
    return matches.first;
  }

  static String? _extractAfterAllergyBoilerplate(String text) {
    final matches = _afterAllergyBoilerplate
        .allMatches(text)
        .map((match) => _trimLineAtSectionEnd(match.group(1)!.trim()))
        .where(_isUsefulIngredientBlock)
        .toList();
    if (matches.isEmpty) {
      return null;
    }
    matches.sort((a, b) {
      return ','.allMatches(b).length.compareTo(','.allMatches(a).length);
    });
    return matches.first;
  }

  static String? _findBestIngredientSpan(String text) {
    final spans = text
        .split(RegExp(r'[\n\r]+'))
        .map((line) => _trimLineAtSectionEnd(line.trim()))
        .where((line) => _isUsefulIngredientBlock(line) && !_isSimpleLowercaseList(line))
        .toList();
    if (spans.isEmpty) {
      return null;
    }
    spans.sort((a, b) {
      return ','.allMatches(b).length.compareTo(','.allMatches(a).length);
    });
    return spans.first;
  }

  static String _trimLineAtSectionEnd(String line) {
    final end = _endIndex(line, 0) ?? line.length;
    return line.substring(0, end).trim().replaceAll(RegExp(r'[.;]+$'), '');
  }

  static bool _isUsefulIngredientBlock(String block) {
    if (block.trim().isEmpty) {
      return false;
    }
    final lower = block.toLowerCase();
    if (lower.startsWith('for allergen') ||
        lower.startsWith('see ingredient') ||
        lower.startsWith('suitable for') ||
        lower.startsWith('keep refrigerated')) {
      return false;
    }
    return _looksLikeIngredientList(block) ||
        ','.allMatches(block).length >= 2 ||
        _hasMultipleIngredientLines(block);
  }

  static bool _hasMultipleIngredientLines(String block) {
    final lines = block
        .split(RegExp(r'[\n\r]+'))
        .map((line) => line.trim().replaceAll(RegExp(r'[,.;]+$'), ''))
        .where((line) => line.isNotEmpty)
        .toList();
    if (lines.length < 2) {
      return false;
    }
    return lines.where((line) {
      final lower = line.toLowerCase();
      return line.length >= 3 &&
          !lower.startsWith('for allergen') &&
          !lower.startsWith('see ingredient') &&
          !lower.startsWith('ingredients') &&
          !lower.startsWith('suitable for');
    }).length >=
        2;
  }

  static bool _isSimpleLowercaseList(String block) {
    final items = block.split(',').map((item) => item.trim()).toList();
    if (items.length < 2) {
      return false;
    }
    return items.every((item) {
      return item.isNotEmpty &&
          item.split('').every((char) {
            return char == char.toLowerCase() ||
                char.trim().isEmpty ||
                char == '-';
          });
    });
  }

  static String _normalizeInput(String rawOcr) {
    return normalizeBrackets(
      rawOcr
          .replaceAll('\r', '\n')
          .replaceAllMapped(RegExp(r'([A-Za-z])-\s+([A-Za-z])'), (match) {
        return '${match.group(1)}${match.group(2)}';
      }),
    );
  }

  static _HeaderMatch? _findBestHeader(String text) {
    final matches = <_HeaderMatch>[];
    for (final pattern in _ingredientsHeaders) {
      for (final match in pattern.allMatches(text)) {
        if (_isValidHeaderMatch(text, match.start, match.end)) {
          matches.add(_HeaderMatch(match.start, match.end - 1));
        }
      }
    }
    if (matches.isEmpty) {
      return null;
    }
    matches.sort((a, b) => a.start.compareTo(b.start));
    return matches.first;
  }

  static bool _isValidHeaderMatch(String text, int start, int endExclusive) {
    final beforeStart = (start - 24).clamp(0, text.length);
    final before = text.substring(beforeStart, start).toLowerCase();
    if (before.contains('see ') || before.contains('for allergen')) {
      return false;
    }

    final afterEnd = (endExclusive + 40).clamp(0, text.length);
    final after = text.substring(endExclusive, afterEnd).trimLeft().toLowerCase();
    if (_headerFalseStarts.any(after.startsWith)) {
      return false;
    }
    return true;
  }

  static int? _endIndex(String text, int startIndex) {
    if (startIndex >= text.length) {
      return null;
    }
    int? minIndex;
    for (final pattern in _sectionEndPatterns) {
      for (final match in pattern.allMatches(text.substring(startIndex))) {
        final index = startIndex + match.start;
        if (minIndex == null || index < minIndex) {
          minIndex = index;
        }
      }
    }
    return minIndex;
  }

  static bool _looksLikeIngredientList(String text) {
    return text.contains(',') || text.contains('(') || text.contains('[');
  }
}
