import 'package:flutter_test/flutter_test.dart';
import 'package:food_classifier/image/normalized_rect.dart';

void main() {
  test('translatedClampsToImageBounds', () {
    const rect = NormalizedRect(left: 0.1, top: 0.2, right: 0.5, bottom: 0.7);
    final moved = rect.translated(normalizedDeltaX: 0.8, normalizedDeltaY: 0.8);
    expect(moved.right, 1.0);
    expect(moved.bottom, 1.0);
  });

  test('fitImageBoundsLetterboxesWideImage', () {
    final bounds = fitImageBounds(
      containerWidth: 100,
      containerHeight: 200,
      imageWidth: 200,
      imageHeight: 100,
    );
    expect(bounds.top, 75);
    expect(bounds.height, 50);
  });

  test('mapViewGuideToImageGuideAccountsForLetterboxing', () {
    const guide = NormalizedRect.ingredientsGuide;
    final mapped = mapViewGuideToImageGuide(
      guide: guide,
      viewWidth: 100,
      viewHeight: 200,
      imageWidth: 200,
      imageHeight: 100,
    );
    expect(mapped.left, 0.04);
    expect(mapped.top, 0.0);
    expect(mapped.right, 0.96);
    expect(mapped.bottom, closeTo(0.82, 0.001));
  });
}
