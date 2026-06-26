import 'dart:async';

import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:food_classifier/image/image_crop_utils.dart';
import 'package:food_classifier/image/normalized_rect.dart';
import 'package:food_classifier/widgets/crop_guide_overlay.dart';
import 'package:image/image.dart' as img;
import 'package:permission_handler/permission_handler.dart';

class InAppCameraScreen extends StatefulWidget {
  const InAppCameraScreen({
    super.key,
    required this.onCaptured,
    required this.onDismiss,
  });

  final ValueChanged<CameraCaptureResult> onCaptured;
  final VoidCallback onDismiss;

  @override
  State<InAppCameraScreen> createState() => _InAppCameraScreenState();
}

class _InAppCameraScreenState extends State<InAppCameraScreen>
    with WidgetsBindingObserver {
  CameraController? _controller;
  bool _isCapturing = false;
  bool _permissionDenied = false;
  String? _initError;
  Offset? _focusPoint;
  Timer? _focusTimer;
  Size _previewAreaSize = Size.zero;
  Size _previewContentSize = Size.zero;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    unawaited(_initCamera());
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _focusTimer?.cancel();
    unawaited(_controller?.dispose());
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    final controller = _controller;
    if (controller == null || !controller.value.isInitialized) {
      return;
    }
    if (state == AppLifecycleState.inactive) {
      unawaited(controller.dispose());
      _controller = null;
    } else if (state == AppLifecycleState.resumed) {
      unawaited(_initCamera());
    }
  }

  Future<void> _initCamera() async {
    final status = await Permission.camera.request();
    if (!status.isGranted) {
      if (mounted) {
        setState(() => _permissionDenied = true);
        widget.onDismiss();
      }
      return;
    }

    try {
      final cameras = await availableCameras();
      final backCamera = cameras.firstWhere(
        (camera) => camera.lensDirection == CameraLensDirection.back,
        orElse: () => cameras.first,
      );
      final controller = CameraController(
        backCamera,
        ResolutionPreset.high,
        enableAudio: false,
        imageFormatGroup: ImageFormatGroup.jpeg,
      );
      await controller.initialize();
      await controller.setFocusMode(FocusMode.auto);
      await controller.setExposureMode(ExposureMode.auto);
      if (!mounted) {
        await controller.dispose();
        return;
      }
      setState(() {
        _controller = controller;
        _previewContentSize = _previewDisplaySize(controller);
      });
    } catch (error) {
      if (mounted) {
        setState(() => _initError = error.toString());
      }
    }
  }

  Size _previewDisplaySize(CameraController controller) {
    final previewSize = controller.value.previewSize;
    if (previewSize == null) {
      return Size.zero;
    }
    return Size(previewSize.height, previewSize.width);
  }

  Future<void> _handleTapToFocus(TapUpDetails details, Size areaSize) async {
    final controller = _controller;
    if (controller == null || !controller.value.isInitialized) {
      return;
    }

    final normalized = Offset(
      (details.localPosition.dx / areaSize.width).clamp(0.0, 1.0),
      (details.localPosition.dy / areaSize.height).clamp(0.0, 1.0),
    );

    setState(() => _focusPoint = details.localPosition);
    _focusTimer?.cancel();
    _focusTimer = Timer(const Duration(milliseconds: 900), () {
      if (mounted) {
        setState(() => _focusPoint = null);
      }
    });

    try {
      await controller.setFocusPoint(normalized);
      await controller.setExposurePoint(normalized);
      await controller.setFocusMode(FocusMode.auto);
    } catch (_) {}
  }

  Future<void> _capture() async {
    final controller = _controller;
    if (controller == null || !controller.value.isInitialized || _isCapturing) {
      return;
    }

    setState(() => _isCapturing = true);
    try {
      final file = await controller.takePicture();
      final rawBytes = await file.readAsBytes();
      final normalizedBytes = normalizeJpegOrientation(rawBytes);
      final decoded = img.decodeImage(normalizedBytes);
      if (decoded == null) {
        throw StateError('Could not decode captured photo.');
      }
      final baked = img.bakeOrientation(decoded);
      final initialCrop = mapViewGuideToImageGuide(
        guide: NormalizedRect.ingredientsGuide,
        viewWidth: _previewAreaSize.width,
        viewHeight: _previewAreaSize.height,
        imageWidth: baked.width.toDouble(),
        imageHeight: baked.height.toDouble(),
      );
      widget.onCaptured(
        CameraCaptureResult(
          imageBytes: normalizedBytes,
          initialCrop: initialCrop,
        ),
      );
    } catch (_) {
      if (mounted) {
        setState(() => _isCapturing = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_permissionDenied) {
      return const SizedBox.shrink();
    }

    final controller = _controller;
    if (_initError != null) {
      return _CameraScaffold(
        onDismiss: widget.onDismiss,
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Text(
              'Could not open camera.',
              style: TextStyle(color: Theme.of(context).colorScheme.error),
              textAlign: TextAlign.center,
            ),
          ),
        ),
      );
    }

    if (controller == null || !controller.value.isInitialized) {
      return _CameraScaffold(
        onDismiss: widget.onDismiss,
        child: const Center(child: CircularProgressIndicator()),
      );
    }

    return _CameraScaffold(
      onDismiss: widget.onDismiss,
      child: Column(
        children: [
          Expanded(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              child: LayoutBuilder(
                builder: (context, constraints) {
                  _previewAreaSize = Size(
                    constraints.maxWidth,
                    constraints.maxHeight,
                  );
                  final contentSize = _previewContentSize;
                  return GestureDetector(
                    onTapUp: (details) =>
                        _handleTapToFocus(details, _previewAreaSize),
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(12),
                      child: Stack(
                        fit: StackFit.expand,
                        children: [
                          Center(
                            child: AspectRatio(
                              aspectRatio: controller.value.aspectRatio,
                              child: CameraPreview(controller),
                            ),
                          ),
                          CropGuideOverlay(
                            guide: NormalizedRect.ingredientsGuide,
                            contentWidth: contentSize.width > 0
                                ? contentSize.width
                                : null,
                            contentHeight: contentSize.height > 0
                                ? contentSize.height
                                : null,
                          ),
                          if (_focusPoint != null)
                            Positioned(
                              left: _focusPoint!.dx - 28,
                              top: _focusPoint!.dy - 28,
                              child: IgnorePointer(
                                child: Container(
                                  width: 56,
                                  height: 56,
                                  decoration: BoxDecoration(
                                    border: Border.all(
                                      color: Colors.white,
                                      width: 2,
                                    ),
                                    borderRadius: BorderRadius.circular(4),
                                  ),
                                ),
                              ),
                            ),
                        ],
                      ),
                    ),
                  );
                },
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: _isCapturing ? null : widget.onDismiss,
                    child: const Text('Cancel'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: FilledButton(
                    onPressed: _isCapturing ? null : _capture,
                    child: Text(_isCapturing ? 'Capturing…' : 'Capture'),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _CameraScaffold extends StatelessWidget {
  const _CameraScaffold({
    required this.onDismiss,
    required this.child,
  });

  final VoidCallback onDismiss;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.black,
      child: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Align(
              alignment: Alignment.centerLeft,
              child: TextButton(
                onPressed: onDismiss,
                child: const Text('Cancel', style: TextStyle(color: Colors.white)),
              ),
            ),
            Expanded(child: child),
          ],
        ),
      ),
    );
  }
}
