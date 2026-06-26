import 'package:flutter/material.dart';
import 'package:food_classifier/theme/app_theme.dart';

class IngredientListCard extends StatelessWidget {
  const IngredientListCard({
    super.key,
    required this.ingredients,
    required this.expanded,
    required this.onExpandedChange,
    required this.onRemove,
    required this.enabled,
  });

  final List<String> ingredients;
  final bool expanded;
  final ValueChanged<bool> onExpandedChange;
  final ValueChanged<int> onRemove;
  final bool enabled;

  @override
  Widget build(BuildContext context) {
    return Card.outlined(
      clipBehavior: Clip.antiAlias,
      child: Column(
        children: [
          InkWell(
            onTap: enabled ? () => onExpandedChange(!expanded) : null,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              child: Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Ingredients',
                          style: Theme.of(context).textTheme.titleMedium?.copyWith(
                                fontWeight: FontWeight.w600,
                              ),
                        ),
                        Text(
                          ingredientListSummary(ingredients.length),
                          style: Theme.of(context).textTheme.bodySmall?.copyWith(
                                color: Theme.of(context)
                                    .colorScheme
                                    .onSurfaceVariant,
                              ),
                        ),
                      ],
                    ),
                  ),
                  Text(
                    expanded ? '▴' : '▾',
                    style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                          fontWeight: FontWeight.bold,
                          color: Theme.of(context).colorScheme.onSurfaceVariant,
                        ),
                  ),
                ],
              ),
            ),
          ),
          AnimatedCrossFade(
            firstChild: const SizedBox.shrink(),
            secondChild: Column(
              children: [
                const Divider(height: 1),
                if (ingredients.isEmpty)
                  Padding(
                    padding: const EdgeInsets.all(16),
                    child: Text(
                      'No ingredients yet. Add one below or scan a label.',
                      style: TextStyle(
                        color: Theme.of(context).colorScheme.onSurfaceVariant,
                      ),
                    ),
                  )
                else
                  Column(
                    children: [
                      for (var index = 0; index < ingredients.length; index++)
                        ColoredBox(
                          color: index.isEven
                              ? Colors.white
                              : AppColors.ingredientRowGray,
                          child: Row(
                            children: [
                              Expanded(
                                child: Padding(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 16,
                                    vertical: 10,
                                  ),
                                  child: Text(ingredients[index]),
                                ),
                              ),
                              IconButton(
                                onPressed: enabled
                                    ? () => onRemove(index)
                                    : null,
                                icon: const Text(
                                  '×',
                                  style: TextStyle(fontSize: 22),
                                ),
                              ),
                            ],
                          ),
                        ),
                    ],
                  ),
                const SizedBox(height: 8),
              ],
            ),
            crossFadeState: expanded
                ? CrossFadeState.showSecond
                : CrossFadeState.showFirst,
            duration: const Duration(milliseconds: 200),
          ),
        ],
      ),
    );
  }
}
