RegExp ic(String pattern, {bool dotAll = false, bool multiline = false}) {
  return RegExp(
    pattern,
    caseSensitive: false,
    dotAll: dotAll,
    multiLine: multiline,
  );
}
