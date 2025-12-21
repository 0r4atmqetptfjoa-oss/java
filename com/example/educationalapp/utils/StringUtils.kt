package com.example.educationalapp.utils

import java.text.Normalizer

fun String.toSafeFileName(): String {
    val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
    val noDiacritics = "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(normalized, "")
    return noDiacritics.replace(" ", "_").lowercase()
}
