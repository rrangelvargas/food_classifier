import 'dart:io';
import 'dart:typed_data';

import 'package:food_classifier/text/ingredient_label_parser.dart';
import 'package:food_classifier/text/ocr_text_quality.dart';
import 'package:google_mlkit_text_recognition/google_mlkit_text_recognition.dart';
import 'package:image/image.dart' as img;

class LabelScanResult {
  const LabelScanResult({
    required this.chips,
    required this.displayText,
    required this.rawOcrText,
  });

  final List<String> chips;
  final String displayText;
  final String rawOcrText;
}

class LabelScanService {
  LabelScanService({TextRecognizer? recognizer})
      : _recognizer = recognizer ?? TextRecognizer();

  final TextRecognizer _recognizer;

  Future<void> dispose() => _recognizer.close();

  Future<LabelScanResult> scanImage(Uint8List imageBytes) async {
    final tempDir = Directory.systemTemp;
    final originalFile = File(
      '${tempDir.path}/food_classifier_scan_${DateTime.now().millisecondsSinceEpoch}.jpg',
    );
    await originalFile.writeAsBytes(imageBytes, flush: true);

    final originalText = await _recognizeFile(originalFile.path);

    var invertedText = '';
    final decoded = img.decodeImage(imageBytes);
    if (decoded != null) {
      final invertedBytes = Uint8List.fromList(
        img.encodeJpg(_invertAndEnhanceContrast(decoded), quality: 92),
      );
      final invertedFile = File('${originalFile.path}.inverted.jpg');
      await invertedFile.writeAsBytes(invertedBytes, flush: true);
      invertedText = await _recognizeFile(invertedFile.path);
      try {
        await invertedFile.delete();
      } catch (_) {}
    }

    try {
      await originalFile.delete();
    } catch (_) {}

    final rawOcrText = pickBetterOcrText([originalText, invertedText]);
    return _parseOrThrow(rawOcrText);
  }

  LabelScanResult parseRawOcr(String rawOcrText) {
    return _parseOrThrow(rawOcrText);
  }

  LabelScanResult _parseOrThrow(String rawOcrText) {
    final parsed = IngredientLabelParser.parse(rawOcrText);
    if (parsed.chips.isEmpty) {
      throw LabelScanException(
        'No ingredients text found. Try a clearer photo of the label.',
      );
    }
    return LabelScanResult(
      chips: parsed.chips,
      displayText: parsed.displayText,
      rawOcrText: rawOcrText,
    );
  }

  Future<String> _recognizeFile(String path) async {
    final recognized = await _recognizer.processImage(InputImage.fromFilePath(path));
    return recognized.text;
  }

  img.Image _invertAndEnhanceContrast(img.Image source) {
    final output = img.Image.from(source);
    for (var y = 0; y < output.height; y++) {
      for (var x = 0; x < output.width; x++) {
        final pixel = output.getPixel(x, y);
        final r = pixel.r.toInt();
        final g = pixel.g.toInt();
        final b = pixel.b.toInt();
        var gray = (0.299 * r + 0.587 * g + 0.114 * b).round();
        gray = 255 - gray;
        gray = ((gray - 128) * 1.8 + 128).round().clamp(0, 255);
        output.setPixelRgba(x, y, gray, gray, gray, pixel.a.toInt());
      }
    }
    return output;
  }
}

class LabelScanException implements Exception {
  const LabelScanException(this.message);

  final String message;

  @override
  String toString() => message;
}
