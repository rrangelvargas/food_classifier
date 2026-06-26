import 'regex_utils.dart';

class AllergyBoundary {
  AllergyBoundary._();

  static final sectionEndPatterns = [
    ic(r'\bALLERGY(?:VICE|VCE|ADV\w*)?\b'),
    ic(r'\ballergy(?:\s+|\s*[-\s]*)(?:adv[i1l]ce|advice|vice|vce)\b'),
    ic(r'\ballergy\s+advice\b'),
    ic(r'\ballergy\s+adv[i1l]ce\b'),
    ic(r'\ballergy\s+a[yvi][a-z]*\b'),
  ];

  static String trimTail(String text) {
    final allergyStart = sectionEndPatterns
        .map((pattern) => pattern.firstMatch(text)?.start)
        .whereType<int>()
        .fold<int?>(null, (min, start) {
      if (min == null || start < min) {
        return start;
      }
      return min;
    });

    final trimmed = allergyStart != null
        ? text.substring(0, allergyStart)
        : text;
    return trimmed.replaceAll(RegExp(r'^[ ,.;:]+|[ ,.;:]+$'), '');
  }

  static String stripTrailingGarbage(String text) {
    return text
        .replaceAll(
          ic(r'[,.;\s]*\bALLERGY(?:VICE|VCE|ADV\w*)?\b.*$'),
          '',
        )
        .replaceAll(
          ic(r'[,.;\s]*\ballergy(?:\s+adv\w*|vice|vce)\b.*$'),
          '',
        )
        .replaceAll(RegExp(r'\s+'), ' ')
        .trim();
  }
}
