import 'package:flutter/foundation.dart';
import 'package:food_classifier/image/normalized_rect.dart';
import 'package:image/image.dart' as img;

class CameraCaptureResult {
  const CameraCaptureResult({
    required this.imageBytes,
    required this.initialCrop,
  });

  final Uint8List imageBytes;
  final NormalizedRect initialCrop;
}

bool prefersInAppCamera() {
  if (kIsWeb) {
    return false;
  }
  switch (defaultTargetPlatform) {
    case TargetPlatform.android:
    case TargetPlatform.iOS:
      return true;
    default:
      return false;
  }
}

Uint8List normalizeJpegOrientation(Uint8List jpegBytes) {
  final decoded = img.decodeImage(jpegBytes);
  if (decoded == null) {
    return jpegBytes;
  }
  final baked = img.bakeOrientation(decoded);
  return Uint8List.fromList(img.encodeJpg(baked, quality: 92));
}

Uint8List? cropImageBytes(Uint8List imageBytes, NormalizedRect crop) {
  final decoded = img.decodeImage(imageBytes);
  if (decoded == null) {
    return null;
  }
  final source = img.bakeOrientation(decoded);
  final left = (crop.left * source.width).round().clamp(0, source.width - 1);
  final top = (crop.top * source.height).round().clamp(0, source.height - 1);
  final right =
      (crop.right * source.width).round().clamp(left + 1, source.width);
  final bottom =
      (crop.bottom * source.height).round().clamp(top + 1, source.height);
  final width = right - left;
  final height = bottom - top;

  final cropped = img.copyCrop(source, x: left, y: top, width: width, height: height);
  return Uint8List.fromList(img.encodeJpg(cropped, quality: 90));
}
