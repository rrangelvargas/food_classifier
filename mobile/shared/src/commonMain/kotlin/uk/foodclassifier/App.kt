package uk.foodclassifier

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uk.foodclassifier.image.ImageCropScreen
import uk.foodclassifier.image.InAppCameraScreen
import uk.foodclassifier.text.IngredientLabelParser
import uk.foodclassifier.image.NormalizedRect
import uk.foodclassifier.image.ScannedLabelImage
import uk.foodclassifier.image.prefersInAppCamera
import uk.foodclassifier.image.rememberImagePicker
import uk.foodclassifier.image.recognizeTextFromImage

private val VeganGreen = Color(0xFF2E7D32)
private val VegetarianAmber = Color(0xFFF9A825)
private val NeitherRed = Color(0xFFC62828)
private val UncertainPurple = Color(0xFF6A1B9A)
private val IngredientRowGray = Color(0xFFF0F0F0)

@Composable
@Preview
fun App() {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = VeganGreen,
            secondary = VegetarianAmber,
        ),
    ) {
        FoodClassifierScreen()
    }
}

@Composable
fun FoodClassifierScreen() {
    var ingredients by remember {
        mutableStateOf(listOf("water", "oats", "sunflower oil", "salt"))
    }
    var newIngredientText by remember { mutableStateOf("") }
    var ingredientsExpanded by remember { mutableStateOf(false) }
    var scannedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var result by remember { mutableStateOf<ClassificationResult?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var extractedIngredientsText by remember { mutableStateOf<String?>(null) }
    var pendingCropBytes by remember { mutableStateOf<ByteArray?>(null) }
    var cropInitialRect by remember { mutableStateOf(NormalizedRect.ingredientsGuide) }
    var showCamera by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun classifyCurrentIngredients() {
        result = classifyParsedIngredients(ingredients)
    }

    fun addIngredientFromInput() {
        val parsed = parseIngredientsToChips(newIngredientText)
        if (parsed.isEmpty()) {
            return
        }
        ingredients = ingredients + parsed
        newIngredientText = ""
        ingredientsExpanded = true
    }

    fun removeIngredient(index: Int) {
        ingredients = ingredients.filterIndexed { chipIndex, _ -> chipIndex != index }
    }

    fun clearAll() {
        ingredients = emptyList()
        scannedImageBytes = null
        extractedIngredientsText = null
        result = null
        scanError = null
    }

    fun scanImage(imageBytes: ByteArray?) {
        if (imageBytes == null) {
            return
        }

        scannedImageBytes = imageBytes

        scope.launch {
            isScanning = true
            scanError = null
            try {
                val recognizedText = recognizeTextFromImage(imageBytes)
                val parsed = IngredientLabelParser.parse(recognizedText)
                if (parsed.chips.isEmpty()) {
                    scanError = "No ingredients text found. Try a clearer photo of the label."
                    return@launch
                }
                extractedIngredientsText = parsed.displayText
                ingredients = parsed.chips
                result = null
            } catch (error: Exception) {
                scanError = error.message ?: "Could not read text from the image."
            } finally {
                isScanning = false
            }
        }
    }

    val imagePicker = rememberImagePicker(
        onGalleryPicked = { bytes ->
            if (bytes != null) {
                scanImage(bytes)
            }
        },
        onCameraPicked = { bytes ->
            if (bytes != null) {
                cropInitialRect = NormalizedRect.ingredientsGuide
                pendingCropBytes = bytes
            }
        },
    )

    when {
        showCamera && prefersInAppCamera() -> {
            InAppCameraScreen(
                visible = true,
                onCaptured = { result ->
                    showCamera = false
                    cropInitialRect = result.initialCrop
                    pendingCropBytes = result.imageBytes
                },
                onDismiss = { showCamera = false },
            )
        }
        pendingCropBytes != null -> {
            val imageBytes = pendingCropBytes!!
            ImageCropScreen(
                imageBytes = imageBytes,
                initialCrop = cropInitialRect,
                onConfirm = { cropped ->
                    pendingCropBytes = null
                    scanImage(cropped)
                },
                onRetake = {
                    pendingCropBytes = null
                    if (prefersInAppCamera()) {
                        showCamera = true
                    } else {
                        imagePicker.takePhoto()
                    }
                },
                onCancel = { pendingCropBytes = null },
            )
        }
        else -> {
            FoodClassifierMainContent(
                ingredients = ingredients,
                newIngredientText = newIngredientText,
                onNewIngredientTextChange = { newIngredientText = it },
                ingredientsExpanded = ingredientsExpanded,
                onIngredientsExpandedChange = { ingredientsExpanded = it },
                scannedImageBytes = scannedImageBytes,
                isScanning = isScanning,
                scanError = scanError,
                extractedIngredientsText = extractedIngredientsText,
                onPickFromGallery = imagePicker.pickFromGallery,
                onTakePhoto = {
                    if (prefersInAppCamera()) {
                        showCamera = true
                    } else {
                        imagePicker.takePhoto()
                    }
                },
                onClearAll = ::clearAll,
                onRemoveIngredient = ::removeIngredient,
                onAddIngredient = ::addIngredientFromInput,
                onClassify = ::classifyCurrentIngredients,
            )
        }
    }

    result?.let { classificationResult ->
        ClassificationResultDialog(
            result = classificationResult,
            onDismiss = { result = null },
        )
    }
}

@Composable
private fun FoodClassifierMainContent(
    ingredients: List<String>,
    newIngredientText: String,
    onNewIngredientTextChange: (String) -> Unit,
    ingredientsExpanded: Boolean,
    onIngredientsExpandedChange: (Boolean) -> Unit,
    scannedImageBytes: ByteArray?,
    isScanning: Boolean,
    scanError: String?,
    extractedIngredientsText: String?,
    onPickFromGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onClearAll: () -> Unit,
    onRemoveIngredient: (Int) -> Unit,
    onAddIngredient: () -> Unit,
    onClassify: () -> Unit,
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding()
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Food Classifier",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Add ingredients one at a time, or scan a food label to fill the list automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedIconButton(
                onClick = onPickFromGallery,
                enabled = !isScanning,
            ) {
                Icon(
                    imageVector = Icons.Filled.Photo,
                    contentDescription = "Choose photo",
                )
            }
            OutlinedIconButton(
                onClick = onTakePhoto,
                enabled = !isScanning,
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = "Take picture",
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (ingredients.isNotEmpty() || scannedImageBytes != null) {
                TextButton(
                    onClick = onClearAll,
                    enabled = !isScanning,
                ) {
                    Text("Clear")
                }
            }
        }

        if (isScanning) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp))
                Text("Scanning label…")
            }
        }

        scanError?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        scannedImageBytes?.let { imageBytes ->
            ScannedLabelImage(imageBytes = imageBytes)
        }

        IngredientList(
            ingredients = ingredients,
            expanded = ingredientsExpanded,
            onExpandedChange = onIngredientsExpandedChange,
            onRemove = onRemoveIngredient,
            enabled = !isScanning,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = newIngredientText,
                onValueChange = onNewIngredientTextChange,
                modifier = Modifier.weight(1f),
                label = { Text("Add ingredient") },
                placeholder = { Text("e.g. milk") },
                singleLine = true,
                enabled = !isScanning,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAddIngredient() }),
            )
            Button(
                onClick = onAddIngredient,
                enabled = !isScanning && newIngredientText.isNotBlank(),
            ) {
                Text("Add")
            }
        }

        Button(
            onClick = onClassify,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isScanning && ingredients.isNotEmpty(),
        ) {
            Text("Classify")
        }

        val copyableIngredientText = extractedIngredientsText
            ?: ingredients.takeIf { it.isNotEmpty() }?.let { formatIngredientsFromChips(it) }

        copyableIngredientText?.let { text ->
            ExtractedIngredientsText(text = text)
        }
    }
}

@Composable
private fun IngredientList(
    ingredients: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onRemove: (Int) -> Unit,
    enabled: Boolean,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { onExpandedChange(!expanded) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "Ingredients",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = ingredientListSummary(ingredients.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = if (expanded) "▴" else "▾",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HorizontalDivider()

                    if (ingredients.isEmpty()) {
                        Text(
                            text = "No ingredients yet. Add one below or scan a label.",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            ingredients.forEachIndexed { index, ingredient ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (index % 2 == 0) {
                                                Color.White
                                            } else {
                                                IngredientRowGray
                                            },
                                        )
                                        .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = ingredient,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    IconButton(
                                        onClick = { onRemove(index) },
                                        enabled = enabled,
                                        modifier = Modifier.size(48.dp),
                                    ) {
                                        Text(
                                            text = "×",
                                            style = MaterialTheme.typography.headlineSmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtractedIngredientsText(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Extracted ingredients (tap to select and copy)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SelectionContainer {
            Text(
                text = text,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp),
                    )
                    .padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun classifyParsedIngredients(ingredients: List<String>): ClassificationResult {
    return classifyIngredients(ingredients)
}

private fun ingredientListSummary(count: Int): String = when (count) {
    0 -> "No ingredients listed"
    1 -> "1 ingredient listed"
    else -> "$count ingredients listed"
}

@Composable
private fun ClassificationResultDialog(
    result: ClassificationResult,
    onDismiss: () -> Unit,
) {
    val color = classificationColor(result.classification)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = result.classification.label,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (result.classification == DietClassification.Uncertain) {
                        Text(
                            text = describeUncertainResult(result.definitiveClassification),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    describeClassificationMatches(
                        result.matchedNonVegetarian,
                        result.matchedNonVegan,
                    ).forEach { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    if (result.warnings.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Warnings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            result.warnings.forEach { warning ->
                                Text(
                                    text = "${warning.term}: ${warning.message}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = items.joinToString(", "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun classificationColor(classification: DietClassification): Color {
    return when (classification) {
        DietClassification.Vegan -> VeganGreen
        DietClassification.Vegetarian -> VegetarianAmber
        DietClassification.Neither -> NeitherRed
        DietClassification.Uncertain -> UncertainPurple
    }
}
