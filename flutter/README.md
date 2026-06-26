# Food Classifier

Flutter app for scanning food ingredient labels and classifying them as vegan, vegetarian, or neither.

## Features

- In-app camera with crop guide and tap-to-focus
- Gallery and camera label scanning (ML Kit + inverted OCR pass)
- Paste raw label text
- Expandable ingredient list with remove
- Manual ingredient entry
- Classification result dialog with warnings

## Run

```bash
flutter pub get
flutter test
flutter run
```

On Android/iOS, the camera button opens the in-app camera (capture → crop → scan). Gallery picks scan directly without cropping.

## Project layout

```
lib/
  classifier.dart              # Diet classification logic
  ingredient_text.dart         # Ingredient parsing helpers
  text/                        # OCR label parsing pipeline
  screens/                     # Main UI, camera, crop
  services/                    # ML Kit scan service
  widgets/                     # Dialogs, ingredient list, overlays
test/                          # Unit and golden tests
```
