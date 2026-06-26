import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:food_classifier/food_classifier.dart';
import 'package:food_classifier/image/image_crop_utils.dart';
import 'package:food_classifier/image/normalized_rect.dart';
import 'package:food_classifier/screens/image_crop_screen.dart';
import 'package:food_classifier/screens/in_app_camera_screen.dart';
import 'package:food_classifier/services/label_scan_service.dart';import 'package:food_classifier/theme/app_theme.dart';
import 'package:food_classifier/widgets/classification_result_dialog.dart';
import 'package:food_classifier/widgets/ingredient_list_card.dart';
import 'package:food_classifier/widgets/paste_label_dialog.dart';
import 'package:food_classifier/widgets/scan_extras.dart';
import 'package:image_picker/image_picker.dart';

class FoodClassifierApp extends StatelessWidget {
  const FoodClassifierApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Food Classifier',
      theme: buildAppTheme(),
      home: const FoodClassifierScreen(),
    );
  }
}

class FoodClassifierScreen extends StatefulWidget {
  const FoodClassifierScreen({super.key});

  @override
  State<FoodClassifierScreen> createState() => _FoodClassifierScreenState();
}

class _FoodClassifierScreenState extends State<FoodClassifierScreen> {
  final _picker = ImagePicker();
  final _newIngredientController = TextEditingController();
  late final LabelScanService _scanService;

  List<String> _ingredients = const [
    'water',
    'oats',
    'sunflower oil',
    'salt',
  ];
  bool _ingredientsExpanded = false;
  Uint8List? _scannedImageBytes;
  ClassificationResult? _result;
  bool _isScanning = false;
  String? _scanError;
  String? _extractedIngredientsText;
  bool _canAddIngredient = false;
  bool _showCamera = false;
  Uint8List? _pendingCropBytes;
  NormalizedRect _cropInitialRect = NormalizedRect.ingredientsGuide;

  @override
  void initState() {
    super.initState();
    _scanService = LabelScanService();
    _newIngredientController.addListener(_updateCanAddIngredient);
  }

  void _updateCanAddIngredient() {
    final canAdd = _newIngredientController.text.trim().isNotEmpty;
    if (canAdd != _canAddIngredient) {
      setState(() => _canAddIngredient = canAdd);
    }
  }

  @override
  void dispose() {
    _newIngredientController.dispose();
    _scanService.dispose();
    super.dispose();
  }

  void _classifyCurrentIngredients() {
    final classification = classifyIngredients(_ingredients);
    setState(() => _result = classification);
  }

  void _addIngredientFromInput() {
    final parsed = parseIngredientsToChips(_newIngredientController.text);
    if (parsed.isEmpty) {
      return;
    }
    setState(() {
      _ingredients = [..._ingredients, ...parsed];
      _newIngredientController.clear();
      _ingredientsExpanded = true;
    });
  }

  void _removeIngredient(int index) {
    setState(() {
      _ingredients = [
        for (var i = 0; i < _ingredients.length; i++)
          if (i != index) _ingredients[i],
      ];
    });
  }

  void _clearAll() {
    setState(() {
      _ingredients = const [];
      _scannedImageBytes = null;
      _extractedIngredientsText = null;
      _result = null;
      _scanError = null;
    });
  }

  void _applyScanResult(LabelScanResult result, {Uint8List? imageBytes}) {
    setState(() {
      if (imageBytes != null) {
        _scannedImageBytes = imageBytes;
      }
      _extractedIngredientsText = result.displayText;
      _ingredients = result.chips;
      _result = null;
      _scanError = null;
      _ingredientsExpanded = true;
    });
  }

  Future<void> _scanImage(Uint8List imageBytes) async {
    setState(() {
      _isScanning = true;
      _scanError = null;
      _scannedImageBytes = imageBytes;
    });

    try {
      final result = await _scanService.scanImage(imageBytes);
      _applyScanResult(result, imageBytes: imageBytes);
    } on LabelScanException catch (error) {
      setState(() => _scanError = error.message);
    } catch (error) {
      setState(() {
        _scanError = error.toString().contains('MissingPluginException')
            ? 'Label scanning is not available on this platform yet.'
            : 'Could not read text from the image.';
      });
    } finally {
      if (mounted) {
        setState(() => _isScanning = false);
      }
    }
  }

  Future<void> _pickImage(ImageSource source) async {
    final picked = await _picker.pickImage(source: source, imageQuality: 92);
    if (picked == null || !mounted) {
      return;
    }
    await _scanImage(await picked.readAsBytes());
  }

  Future<void> _pickImageForCrop(ImageSource source) async {
    final picked = await _picker.pickImage(source: source, imageQuality: 92);
    if (picked == null || !mounted) {
      return;
    }
    final bytes = normalizeJpegOrientation(await picked.readAsBytes());
    setState(() {
      _cropInitialRect = NormalizedRect.ingredientsGuide;
      _pendingCropBytes = bytes;
    });
  }

  void _takePhoto() {
    if (prefersInAppCamera()) {
      setState(() => _showCamera = true);
      return;
    }
    _pickImageForCrop(ImageSource.camera);
  }

  void _onCameraCaptured(CameraCaptureResult result) {
    setState(() {
      _showCamera = false;
      _cropInitialRect = result.initialCrop;
      _pendingCropBytes = result.imageBytes;
    });
  }

  void _retakePhoto() {
    setState(() {
      _pendingCropBytes = null;
      if (prefersInAppCamera()) {
        _showCamera = true;
      }
    });
    if (!prefersInAppCamera()) {
      _pickImageForCrop(ImageSource.camera);
    }
  }

  Future<void> _pasteLabelText() async {
    final rawText = await showPasteLabelDialog(context);
    if (rawText == null || !mounted) {
      return;
    }

    setState(() {
      _isScanning = true;
      _scanError = null;
    });

    try {
      _applyScanResult(_scanService.parseRawOcr(rawText));
    } on LabelScanException catch (error) {
      setState(() => _scanError = error.message);
    } finally {
      if (mounted) {
        setState(() => _isScanning = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_showCamera && prefersInAppCamera()) {
      return InAppCameraScreen(
        onCaptured: _onCameraCaptured,
        onDismiss: () => setState(() => _showCamera = false),
      );
    }

    if (_pendingCropBytes != null) {
      return ImageCropScreen(
        imageBytes: _pendingCropBytes!,
        initialCrop: _cropInitialRect,
        onConfirm: (cropped) {
          setState(() => _pendingCropBytes = null);
          _scanImage(cropped);
        },
        onRetake: _retakePhoto,
        onCancel: () => setState(() => _pendingCropBytes = null),
      );
    }

    final copyableText = _extractedIngredientsText ??
        (_ingredients.isNotEmpty
            ? formatIngredientsFromChips(_ingredients)
            : null);

    return Stack(
      children: [
        Scaffold(
          body: SafeArea(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(
                    'Food Classifier',
                    style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Add ingredients one at a time, or scan a food label to fill the list automatically.',
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          color: Theme.of(context).colorScheme.onSurfaceVariant,
                        ),
                  ),
                  const SizedBox(height: 16),
                  Row(
                    children: [
                      OutlinedButton(
                        onPressed:
                            _isScanning ? null : () => _pickImage(ImageSource.gallery),
                        child: const Icon(Icons.photo_outlined),
                      ),
                      const SizedBox(width: 12),
                      OutlinedButton(
                        onPressed: _isScanning ? null : _takePhoto,
                        child: const Icon(Icons.photo_camera_outlined),
                      ),
                      const SizedBox(width: 12),
                      OutlinedButton(
                        onPressed: _isScanning ? null : _pasteLabelText,
                        child: const Icon(Icons.content_paste_outlined),
                      ),
                      const Spacer(),
                      if (_ingredients.isNotEmpty || _scannedImageBytes != null)
                        TextButton(
                          onPressed: _isScanning ? null : _clearAll,
                          child: const Text('Clear'),
                        ),
                    ],
                  ),
                  if (_isScanning) ...[
                    const SizedBox(height: 16),
                    const Row(
                      children: [
                        SizedBox(
                          width: 24,
                          height: 24,
                          child: CircularProgressIndicator(strokeWidth: 2.5),
                        ),
                        SizedBox(width: 12),
                        Text('Scanning label…'),
                      ],
                    ),
                  ],
                  if (_scanError != null) ...[
                    const SizedBox(height: 16),
                    Text(
                      _scanError!,
                      style: TextStyle(color: Theme.of(context).colorScheme.error),
                    ),
                  ],
                  if (_scannedImageBytes != null) ...[
                    const SizedBox(height: 16),
                    ScannedLabelImage(imageBytes: _scannedImageBytes!),
                  ],
                  const SizedBox(height: 16),
                  IngredientListCard(
                    ingredients: _ingredients,
                    expanded: _ingredientsExpanded,
                    onExpandedChange: (expanded) {
                      setState(() => _ingredientsExpanded = expanded);
                    },
                    onRemove: _removeIngredient,
                    enabled: !_isScanning,
                  ),
                  const SizedBox(height: 16),
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        child: TextField(
                          controller: _newIngredientController,
                          enabled: !_isScanning,
                          decoration: const InputDecoration(
                            labelText: 'Add ingredient',
                            hintText: 'e.g. milk',
                            border: OutlineInputBorder(),
                          ),
                          onSubmitted: (_) => _addIngredientFromInput(),
                        ),
                      ),
                      const SizedBox(width: 12),
                      FilledButton(
                        onPressed:
                            _isScanning || !_canAddIngredient ? null : _addIngredientFromInput,
                        child: const Text('Add'),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  FilledButton(
                    onPressed: _isScanning || _ingredients.isEmpty
                        ? null
                        : _classifyCurrentIngredients,
                    child: const Text('Classify'),
                  ),
                  if (copyableText != null) ...[
                    const SizedBox(height: 16),
                    ExtractedIngredientsText(text: copyableText),
                  ],
                ],
              ),
            ),
          ),
        ),
        if (_result != null)
          ClassificationResultDialog(
            result: _result!,
            onDismiss: () => setState(() => _result = null),
          ),
      ],
    );
  }
}
