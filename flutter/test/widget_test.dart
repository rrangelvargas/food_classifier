import 'package:flutter_test/flutter_test.dart';
import 'package:food_classifier/screens/food_classifier_screen.dart';

void main() {
  testWidgets('main screen renders with classify button', (tester) async {
    await tester.pumpWidget(const FoodClassifierApp());
    expect(find.text('Food Classifier'), findsOneWidget);
    expect(find.text('Classify'), findsOneWidget);
    expect(find.text('Ingredients'), findsOneWidget);
  });

  testWidgets('classify opens result dialog', (tester) async {
    await tester.pumpWidget(const FoodClassifierApp());
    await tester.tap(find.text('Classify'));
    await tester.pumpAndSettle();
    expect(find.text('Vegan'), findsOneWidget);
    expect(find.text('Close'), findsOneWidget);
  });
}
