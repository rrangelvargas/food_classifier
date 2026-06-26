import 'package:flutter/material.dart';
import 'package:food_classifier/models/diet_classification.dart';

class AppColors {
  AppColors._();

  static const veganGreen = Color(0xFF2E7D32);
  static const vegetarianAmber = Color(0xFFF9A825);
  static const neitherRed = Color(0xFFC62828);
  static const uncertainPurple = Color(0xFF6A1B9A);
  static const ingredientRowGray = Color(0xFFF0F0F0);

  static Color forClassification(DietClassification classification) {
    return switch (classification) {
      DietClassification.vegan => veganGreen,
      DietClassification.vegetarian => vegetarianAmber,
      DietClassification.neither => neitherRed,
      DietClassification.uncertain => uncertainPurple,
    };
  }
}

ThemeData buildAppTheme() {
  return ThemeData(
    colorScheme: ColorScheme.fromSeed(
      seedColor: AppColors.veganGreen,
      secondary: AppColors.vegetarianAmber,
    ),
    useMaterial3: true,
  );
}

String ingredientListSummary(int count) {
  return switch (count) {
    0 => 'No ingredients listed',
    1 => '1 ingredient listed',
    _ => '$count ingredients listed',
  };
}
