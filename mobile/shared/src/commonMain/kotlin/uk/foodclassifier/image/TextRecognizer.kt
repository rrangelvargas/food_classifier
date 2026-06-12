package uk.foodclassifier.image

expect suspend fun recognizeTextFromImage(imageBytes: ByteArray): String
