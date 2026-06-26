/// Normalized rectangle (0–1) for crop guides and image cropping.
class NormalizedRect {
  const NormalizedRect({
    required this.left,
    required this.top,
    required this.right,
    required this.bottom,
  })  : assert(left >= 0 && left <= 1),
        assert(top >= 0 && top <= 1),
        assert(right >= 0 && right <= 1),
        assert(bottom >= 0 && bottom <= 1),
        assert(right > left),
        assert(bottom > top);

  final double left;
  final double top;
  final double right;
  final double bottom;

  double get width => right - left;
  double get height => bottom - top;

  /// Wide horizontal band — matches a typical ingredients block on product labels.
  static const ingredientsGuide = NormalizedRect(
    left: 0.04,
    top: 0.36,
    right: 0.96,
    bottom: 0.58,
  );

  static const fullImage = NormalizedRect(
    left: 0,
    top: 0,
    right: 1,
    bottom: 1,
  );

  NormalizedRect translated({
    required double normalizedDeltaX,
    required double normalizedDeltaY,
  }) {
    var newLeft = left + normalizedDeltaX;
    var newTop = top + normalizedDeltaY;
    newLeft = newLeft.clamp(0.0, 1.0 - width);
    newTop = newTop.clamp(0.0, 1.0 - height);
    return NormalizedRect(
      left: newLeft,
      top: newTop,
      right: newLeft + width,
      bottom: newTop + height,
    );
  }
}

class ImageBounds {
  const ImageBounds({
    required this.left,
    required this.top,
    required this.width,
    required this.height,
  });

  final double left;
  final double top;
  final double width;
  final double height;

  ScreenRect toScreenRect(NormalizedRect normalized) => ScreenRect(
        left: left + normalized.left * width,
        top: top + normalized.top * height,
        right: left + normalized.right * width,
        bottom: top + normalized.bottom * height,
      );
}

class ScreenRect {
  const ScreenRect({
    required this.left,
    required this.top,
    required this.right,
    required this.bottom,
  });

  final double left;
  final double top;
  final double right;
  final double bottom;
}

ImageBounds fitImageBounds({
  required double containerWidth,
  required double containerHeight,
  required double imageWidth,
  required double imageHeight,
}) {
  if (containerWidth <= 0 ||
      containerHeight <= 0 ||
      imageWidth <= 0 ||
      imageHeight <= 0) {
    return ImageBounds(
      left: 0,
      top: 0,
      width: containerWidth,
      height: containerHeight,
    );
  }

  final imageAspect = imageWidth / imageHeight;
  final containerAspect = containerWidth / containerHeight;
  if (imageAspect > containerAspect) {
    final displayedWidth = containerWidth;
    final displayedHeight = containerWidth / imageAspect;
    final yOffset = (containerHeight - displayedHeight) / 2;
    return ImageBounds(
      left: 0,
      top: yOffset,
      width: displayedWidth,
      height: displayedHeight,
    );
  }

  final displayedHeight = containerHeight;
  final displayedWidth = containerHeight * imageAspect;
  final xOffset = (containerWidth - displayedWidth) / 2;
  return ImageBounds(
    left: xOffset,
    top: 0,
    width: displayedWidth,
    height: displayedHeight,
  );
}

NormalizedRect mapViewGuideToImageGuide({
  required NormalizedRect guide,
  required double viewWidth,
  required double viewHeight,
  required double imageWidth,
  required double imageHeight,
}) {
  if (viewWidth <= 0 ||
      viewHeight <= 0 ||
      imageWidth <= 0 ||
      imageHeight <= 0) {
    return guide;
  }

  final displayed = fitImageBounds(
    containerWidth: viewWidth,
    containerHeight: viewHeight,
    imageWidth: imageWidth,
    imageHeight: imageHeight,
  );

  final guideLeftPx = guide.left * viewWidth;
  final guideTopPx = guide.top * viewHeight;
  final guideRightPx = guide.right * viewWidth;
  final guideBottomPx = guide.bottom * viewHeight;

  final contentLeft = displayed.left;
  final contentTop = displayed.top;
  final contentRight = displayed.left + displayed.width;
  final contentBottom = displayed.top + displayed.height;

  final clippedLeft = guideLeftPx > contentLeft ? guideLeftPx : contentLeft;
  final clippedTop = guideTopPx > contentTop ? guideTopPx : contentTop;
  final clippedRight =
      guideRightPx < contentRight ? guideRightPx : contentRight;
  final clippedBottom =
      guideBottomPx < contentBottom ? guideBottomPx : contentBottom;

  if (clippedRight <= clippedLeft || clippedBottom <= clippedTop) {
    return guide;
  }

  return NormalizedRect(
    left: (clippedLeft - contentLeft) / displayed.width,
    top: (clippedTop - contentTop) / displayed.height,
    right: (clippedRight - contentLeft) / displayed.width,
    bottom: (clippedBottom - contentTop) / displayed.height,
  );
}
