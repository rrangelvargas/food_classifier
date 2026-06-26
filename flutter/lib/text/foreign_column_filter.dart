import 'regex_utils.dart';

class ForeignColumnFilter {
  ForeignColumnFilter._();

  static final _hyphenatedForeignFragment =
      RegExp(r'\b[a-z]+(?:-[a-z]+)+\b');

  static String strip(String text) {
    var result = text.replaceAllMapped(
      ic(r'\b([A-Z]{2})-ING(?:REDIENTS?|REL)\s*:?\s*'),
      (match) {
        if (match.group(1)!.toUpperCase() == 'EN') {
          return match.group(0)!;
        }
        return ' ';
      },
    );
    return result
        .replaceAll(_hyphenatedForeignFragment, ' ')
        .replaceAll(RegExp(r'\s+ne\s+'), ' ')
        .replaceAll(RegExp(r'[^\S\n]+'), ' ')
        .trim();
  }
}
