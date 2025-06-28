// ui/screens/OcrHistoryScreen.kt
package com.example.imagetotext.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import com.example.imagetotext.model.OcrHistoryItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrHistoryScreen(
    onLogout: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var historyList by remember { mutableStateOf(listOf<OcrHistoryItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(true) {
        try {
            val snapshot = db.collection("ocr_history")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            historyList = snapshot.documents.map { doc ->
                OcrHistoryItem(
                    text = doc.getString("text") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    docid = doc.id
                )
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to load history", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    val filteredHistory = historyList.filter { item ->
        val matchesSearch = searchQuery.isBlank() || item.text.contains(searchQuery, ignoreCase = true)
        val matchesDate = selectedDate?.let { date ->
            val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val itemDate = sdf.format(Date(item.timestamp))
            val selectedDateOnly = sdf.format(date)
            itemDate == selectedDateOnly
        } ?: true
        matchesSearch && matchesDate
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(onClick = {
                    auth.signOut()
                    showLogoutDialog = false
                    onLogout()
                    Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Yes", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("OCR History", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text("${historyList.size} items saved", fontSize = 14.sp)
                        }
                    }

                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", modifier = Modifier.size(24.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search") },
                    modifier = Modifier.weight(1f)
                )

                Button(onClick = {
                    val today = Calendar.getInstance()
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            Calendar.getInstance().apply {
                                set(y, m, d, 0, 0)
                                selectedDate = time
                            }
                        },
                        today.get(Calendar.YEAR),
                        today.get(Calendar.MONTH),
                        today.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }) {
                    Text("Pick Date")
                }

                if (selectedDate != null) {
                    IconButton(onClick = { selectedDate = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear Date")
                    }
                }
            }

            selectedDate?.let {
                val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it)
                AssistChip(
                    onClick = {},
                    label = { Text("Date: $formattedDate") },
                    trailingIcon = {
                        IconButton(onClick = { selectedDate = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Date")
                        }
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(80.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No OCR History Found", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Try another search or date", fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
                    items(filteredHistory) { item ->

                        val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        val annotatedString = buildAnnotatedString {
                            if (searchQuery.isNotEmpty()) {
                                val regex = Regex("(?i)${Regex.escape(searchQuery)}")
                                var currentIndex = 0
                                for (match in regex.findAll(item.text)) {
                                    val start = match.range.first
                                    val end = match.range.last + 1
                                    append(item.text.substring(currentIndex, start))
                                    withStyle(SpanStyle(background = highlightColor)) {
                                        append(item.text.substring(start, end))
                                    }
                                    currentIndex = end
                                }
                                if (currentIndex < item.text.length) {
                                    append(item.text.substring(currentIndex))
                                }
                            } else {
                                append(item.text)
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = annotatedString,
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp,
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()).format(Date(item.timestamp)),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(item.text))
                                            Toast.makeText(context, "📋 Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Copy")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, item.text)
                                            }
                                            startActivity(context, Intent.createChooser(shareIntent, "Share via"), null)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Share")
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}
