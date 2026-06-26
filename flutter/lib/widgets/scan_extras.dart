import 'dart:typed_data';

import 'package:flutter/material.dart';

class ScannedLabelImage extends StatelessWidget {
  const ScannedLabelImage({super.key, required this.imageBytes});

  final Uint8List imageBytes;

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(8),
      child: Image.memory(
        imageBytes,
        fit: BoxFit.contain,
        width: double.infinity,
      ),
    );
  }
}

class ExtractedIngredientsText extends StatelessWidget {
  const ExtractedIngredientsText({super.key, required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Extracted ingredients (tap to select and copy)',
          style: Theme.of(context).textTheme.labelMedium?.copyWith(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
              ),
        ),
        const SizedBox(height: 8),
        SelectableText(
          text,
          style: Theme.of(context).textTheme.bodySmall,
        ),
      ],
    );
  }
}
