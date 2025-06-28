package com.example.imagetotext.model

import com.example.imagetotext.model.TranslationHistoryItem


import com.google.firebase.Timestamp

data class TranslationHistoryItem(
    val originalText: String = "",
    val timestamp: Timestamp? = null
)
