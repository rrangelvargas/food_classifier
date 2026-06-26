import 'regex_utils.dart';

int scoreOcrText(String text) {
  if (text.trim().isEmpty) {
    return 0;
  }
  var score = text.length;
  score += ','.allMatches(text).length * 25;
  score += RegExp(r'[(\[]').allMatches(text).length * 15;
  if (ic(r'\bingredients\b').hasMatch(text)) {
    score += 120;
  }
  if (ic(r'\b(?:skyr|yogurt|milk|concentrate|starch|flavouring|sweetener)\b')
      .hasMatch(text)) {
    score += 40;
  }
  return score;
}

String pickBetterOcrText(List<String> candidates) {
  return candidates
      .where((candidate) => candidate.trim().isNotEmpty)
      .fold<String?>(
        null,
        (best, candidate) {
          if (best == null || scoreOcrText(candidate) > scoreOcrText(best)) {
            return candidate;
          }
          return best;
        },
      ) ??
      '';
}
