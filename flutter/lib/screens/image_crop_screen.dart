import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:food_classifier/image/image_crop_utils.dart';
import 'package:food_classifier/image/normalized_rect.dart';
import 'package:image/image.dart' as img;

class ImageCropScreen extends StatefulWidget {
  const ImageCropScreen({
    super.key,
    required this.imageBytes,
    required this.initialCrop,
    required this.onConfirm,
    required this.onRetake,
    required this.onCancel,
  });

  final Uint8List imageBytes;
  final NormalizedRect initialCrop;
  final ValueChanged<Uint8List> onConfirm;
  final VoidCallback onRetake;
  final VoidCallback onCancel;

  @override
  State<ImageCropScreen> createState() => _ImageCropScreenState();
}

class _ImageCropScreenState extends State<ImageCropScreen> {
  late NormalizedRect _cropRect;

  @override
  void initState() {
    super.initState();
    _cropRect = widget.initialCrop;
  }

  @override
  void didUpdateWidget(covariant ImageCropScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.imageBytes != widget.imageBytes ||
        oldWidget.initialCrop != widget.initialCrop) {
      _cropRect = widget.initialCrop;
    }
  }

  void _confirm() {
    final cropped = cropImageBytes(widget.imageBytes, _cropRect) ?? widget.imageBytes;
    widget.onConfirm(cropped);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                TextButton(onPressed: widget.onCancel, child: const Text('Cancel')),
                TextButton(onPressed: widget.onRetake, child: const Text('Retake')),
              ],
            ),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 12),
                child: _CropImageCanvas(
                  imageBytes: widget.imageBytes,
                  cropRect: _cropRect,
                  onCropRectChange: (rect) => setState(() => _cropRect = rect),
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(16),
              child: FilledButton(
                onPressed: _confirm,
                child: const Text('Scan'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _CropImageCanvas extends StatelessWidget {
  const _CropImageCanvas({
    required this.imageBytes,
    required this.cropRect,
    required this.onCropRectChange,
  });

  final Uint8List imageBytes;
  final NormalizedRect cropRect;
  final ValueChanged<NormalizedRect> onCropRectChange;

  Size _imageDisplaySize() {
    final decoded = img.decodeImage(imageBytes);
    if (decoded == null) {
      return Size.zero;
    }
    final baked = img.bakeOrientation(decoded);
    return Size(baked.width.toDouble(), baked.height.toDouble());
  }

  @override
  Widget build(BuildContext context) {
    final imageSize = _imageDisplaySize();

    return LayoutBuilder(
      builder: (context, constraints) {
        final imageBounds = imageSize == Size.zero
            ? ImageBounds(
                left: 0,
                top: 0,
                width: constraints.maxWidth,
                height: constraints.maxHeight,
              )
            : fitImageBounds(
                containerWidth: constraints.maxWidth,
                containerHeight: constraints.maxHeight,
                imageWidth: imageSize.width,
                imageHeight: imageSize.height,
              );

        return ClipRRect(
          borderRadius: BorderRadius.circular(12),
          child: ColoredBox(
            color: Colors.black,
            child: Stack(
              fit: StackFit.expand,
              children: [
                Image.memory(
                  imageBytes,
                  fit: BoxFit.contain,
                ),
                GestureDetector(
                  onPanUpdate: (details) {
                    if (imageBounds.width <= 0 || imageBounds.height <= 0) {
                      return;
                    }
                    onCropRectChange(
                      cropRect.translated(
                        normalizedDeltaX: details.delta.dx / imageBounds.width,
                        normalizedDeltaY: details.delta.dy / imageBounds.height,
                      ),
                    );
                  },
                  child: CustomPaint(
                    painter: _CropOverlayPainter(
                      cropRect: cropRect,
                      imageBounds: imageBounds,
                    ),
                    child: const SizedBox.expand(),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

class _CropOverlayPainter extends CustomPainter {
  _CropOverlayPainter({
    required this.cropRect,
    required this.imageBounds,
  });

  final NormalizedRect cropRect;
  final ImageBounds imageBounds;

  @override
  void paint(Canvas canvas, Size size) {
    final screenRect = imageBounds.toScreenRect(cropRect);
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
  bool shouldRepaint(covariant _CropOverlayPainter oldDelegate) {
    return oldDelegate.cropRect != cropRect ||
        oldDelegate.imageBounds.left != imageBounds.left ||
        oldDelegate.imageBounds.top != imageBounds.top ||
        oldDelegate.imageBounds.width != imageBounds.width ||
        oldDelegate.imageBounds.height != imageBounds.height;
  }
}
