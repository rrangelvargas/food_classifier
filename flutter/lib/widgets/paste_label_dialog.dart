import 'package:flutter/material.dart';

Future<String?> showPasteLabelDialog(BuildContext context) {
  return showDialog<String>(
    context: context,
    builder: (context) => const _PasteLabelDialog(),
  );
}

class _PasteLabelDialog extends StatefulWidget {
  const _PasteLabelDialog();

  @override
  State<_PasteLabelDialog> createState() => _PasteLabelDialogState();
}

class _PasteLabelDialogState extends State<_PasteLabelDialog> {
  final _controller = TextEditingController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Paste label text'),
      content: SizedBox(
        width: 420,
        child: TextField(
          controller: _controller,
          decoration: const InputDecoration(
            hintText: 'Paste raw OCR or ingredients text from the label…',
            border: OutlineInputBorder(),
          ),
          minLines: 6,
          maxLines: 12,
          autofocus: true,
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: () {
            final text = _controller.text.trim();
            if (text.isNotEmpty) {
              Navigator.of(context).pop(text);
            }
          },
          child: const Text('Parse'),
        ),
      ],
    );
  }
}
