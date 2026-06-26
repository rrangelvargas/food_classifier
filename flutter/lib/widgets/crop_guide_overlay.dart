import 'package:flutter/material.dart';
import 'package:food_classifier/image/normalized_rect.dart';

class CropGuideOverlay extends StatelessWidget {
  const CropGuideOverlay({
    super.key,
    required this.guide,
    this.contentWidth,
    this.contentHeight,
  });

  final NormalizedRect guide;
  final double? contentWidth;
  final double? contentHeight;

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      painter: _CropGuidePainter(
        guide: guide,
        contentWidth: contentWidth,
        contentHeight: contentHeight,
      ),
      child: const SizedBox.expand(),
    );
  }
}

class _CropGuidePainter extends CustomPainter {
  _CropGuidePainter({
    required this.guide,
    this.contentWidth,
    this.contentHeight,
  });

  final NormalizedRect guide;
  final double? contentWidth;
  final double? contentHeight;

  @override
  void paint(Canvas canvas, Size size) {
    final contentBounds = contentWidth != null && contentHeight != null
        ? fitImageBounds(
            containerWidth: size.width,
            containerHeight: size.height,
            imageWidth: contentWidth!,
            imageHeight: contentHeight!,
          )
        : ImageBounds(
            left: 0,
            top: 0,
            width: size.width,
            height: size.height,
          );

    final screenRect = contentBounds.toScreenRect(guide);
    final hole = Rect.fromLTRB(
      screenRect.left,
      screenRect.top,
      screenRect.right,
      screenRect.bottom,
    );

    final overlayPath = Path()
      ..fillType = PathFillType.evenOdd
      ..addRect(Offset.zero & size)
      ..addRect(hole);

    canvas.drawPath(
      overlayPath,
      Paint()..color = Colors.black.withValues(alpha: 0.55),
    );
    canvas.drawRect(
      hole,
      Paint()
        ..color = Colors.white
        ..style = PaintingStyle.stroke
        ..strokeWidth = 3,
    );
  }

  @override
  bool shouldRepaint(covariant _CropGuidePainter oldDelegate) {
    return oldDelegate.guide != guide ||
        oldDelegate.contentWidth != contentWidth ||
        oldDelegate.contentHeight != contentHeight;
  }
}
