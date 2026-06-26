import 'package:flutter/material.dart';
import 'package:food_classifier/classification_messages.dart';
import 'package:food_classifier/models/classification_result.dart';
import 'package:food_classifier/models/diet_classification.dart';
import 'package:food_classifier/theme/app_theme.dart';

class ClassificationResultDialog extends StatelessWidget {
  const ClassificationResultDialog({
    super.key,
    required this.result,
    required this.onDismiss,
  });

  final ClassificationResult result;
  final VoidCallback onDismiss;

  @override
  Widget build(BuildContext context) {
    final color = AppColors.forClassification(result.classification);

    return Dialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 480),
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                result.classification.label,
                style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                      fontWeight: FontWeight.bold,
                      color: color,
                    ),
              ),
              const SizedBox(height: 12),
              Flexible(
                child: SingleChildScrollView(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      if (result.classification == DietClassification.uncertain)
                        Padding(
                          padding: const EdgeInsets.only(bottom: 12),
                          child: Text(
                            describeUncertainResult(
                              result.definitiveClassification,
                            ),
                            style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                                  color: Theme.of(context)
                                      .colorScheme
                                      .onSurfaceVariant,
                                ),
                          ),
                        ),
                      ...describeClassificationMatches(
                        result.matchedNonVegetarian,
                        result.matchedNonVegan,
                      ).map(
                        (message) => Padding(
                          padding: const EdgeInsets.only(bottom: 8),
                          child: Text(message),
                        ),
                      ),
                      if (result.warnings.isNotEmpty) ...[
                        const SizedBox(height: 8),
                        Text(
                          'Warnings',
                          style: Theme.of(context).textTheme.titleMedium?.copyWith(
                                fontWeight: FontWeight.w600,
                              ),
                        ),
                        const SizedBox(height: 8),
                        ...result.warnings.map(
                          (warning) => Padding(
                            padding: const EdgeInsets.only(bottom: 8),
                            child: Text(
                              '${warning.term}: ${warning.message}',
                              style: TextStyle(
                                color: Theme.of(context)
                                    .colorScheme
                                    .onSurfaceVariant,
                              ),
                            ),
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 12),
              Align(
                alignment: Alignment.centerRight,
                child: TextButton(
                  onPressed: onDismiss,
                  child: const Text('Close'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
