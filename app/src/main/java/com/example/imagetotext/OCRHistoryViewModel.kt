package com.example.imagetotext.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.imagetotext.model.OcrHistoryItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class OCRHistoryViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // Internal mutable list
    private val _history = mutableStateListOf<OcrHistoryItem>()

    // Public immutable access
    val history: List<OcrHistoryItem> get() = _history

    init {
        fetchHistory()
    }

    fun fetchHistory() {
        db.collection("ocr_history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                _history.clear()
                for (doc in snapshot.documents) {
                    val item = doc.toObject(OcrHistoryItem::class.java)
                    if (item != null) {
                        _history.add(item.copy(docid = doc.id))  // Ensure docId is set
                    }
                }
            }
            .addOnFailureListener {
                // Optional: Log or handle error
            }
    }
}
