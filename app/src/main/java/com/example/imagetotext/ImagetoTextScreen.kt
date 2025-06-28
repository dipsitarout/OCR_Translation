package com.example.imagetotext

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.example.imagetotext.network.TranslateApi
import android.util.Log

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.firestore.ktx.firestore
import com.example.imagetotext.network.languageMap
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

import com.google.firebase.ktx.Firebase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Translate

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.content.Intent
import android.widget.Toast
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PermMedia
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import android.speech.tts.TextToSpeech

import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.Timestamp
import java.util.UUID
import java.util.Locale


import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.window.Dialog


// Helper function to handle translation
private fun translateText(
    text: String,
    targetLanguage: String,
    onResult: (String) -> Unit,
    onError: (String) -> Unit
) {
    try {
        TranslateApi.translateText(
            text = text,
            targetLanguage = targetLanguage,
            onResult = { result ->
                if (result == "Translation failed" || result == "Translation error") {
                    onError(result)
                } else {
                    onResult(result)
                }
            },
            onError = { error ->
                onError("Translation failed: $error")
            }
        )
    } catch (e: Exception) {
        onError("Translation failed: ${e.message}")
    }
}

fun saveToFirestore(
    context: Context,
    extractedText: String,
    imageUrl: String? = null
) {
    val db = Firebase.firestore
    val data = hashMapOf(
        "text" to extractedText,
        "timestamp" to System.currentTimeMillis(),
        "imageUrl" to imageUrl
    )

    db.collection("ocr_history")
        .add(data)
        .addOnSuccessListener {
            Toast.makeText(context, "Saved to Firestore ✅", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Error saving to Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

fun uploadImageAndSaveResult(context: Context, uri: Uri, resultText: String) {
    val storageRef = FirebaseStorage.getInstance().reference
    val imageRef = storageRef.child("images/${UUID.randomUUID()}.jpg")

    imageRef.putFile(uri).continueWithTask { task ->
        if (!task.isSuccessful) throw task.exception!!
        imageRef.downloadUrl
    }.addOnSuccessListener { downloadUrl ->
        val firestore = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnSuccessListener

        val ocrResult = hashMapOf(
            "userId" to uid,
            "imageUrl" to downloadUrl.toString(),
            "extractedText" to resultText,
            "timestamp" to Timestamp.now()
        )

        firestore.collection("ocrResults")
            .add(ocrResult)
            .addOnSuccessListener {
                Toast.makeText(context, "Saved to history!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to save: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }.addOnFailureListener {
//        Toast.makeText(context, "Image upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
    }
}

fun processImage(
    uri: Uri,
    context: Context,
    callback: (Bitmap?, String, Boolean) -> Unit
) {
    callback(null, "", true) // Start processing state

    try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        if (inputStream == null) {
            callback(null, "Could not open image stream", false)
            return
        }

        val bmp = BitmapFactory.decodeStream(inputStream)
        if (bmp == null) {
            callback(null, "Could not decode image", false)
            return
        }

        val image = InputImage.fromBitmap(bmp, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val text = if (visionText.text.isNotEmpty()) visionText.text
                else "No text found in the image"

                // Optional: Upload image and save results
                uploadImageAndSaveResult(context, uri, text)
                saveToFirestore(context, text)

                callback(bmp, text, false)
            }
            .addOnFailureListener {
                callback(bmp, "Failed to recognize text. Please try again.", false)
            }
    } catch (e: Exception) {
        callback(null, "Error processing image: ${e.localizedMessage}",false)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToTextScreen(
    onLogout: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var resultText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var showImagePickerDialog by remember { mutableStateOf(false) }

    // --- Translation States ---
    var selectedLang by remember { mutableStateOf("hi") }
    var translatedText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var isTranslating by remember { mutableStateOf(false) }
    var showFullImage by remember { mutableStateOf(false) }

    // Create a temporary file for camera capture
    val imageFile = remember {
        File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
    }

    val cameraUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            imageFile
        )
    }

    val cropImageLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val croppedUri = result.uriContent
            if (croppedUri != null) {
                processImage(croppedUri, context) { bmp, text, processing ->
                    bitmap = bmp
                    resultText = text
                    isProcessing = processing
                    translatedText = "" // Reset translation
                }
            } else {
                resultText = "Image cropping failed: URI is null"
                isProcessing = false
            }
        } else {
            resultText = "Image cropping failed: ${result.error?.message ?: "Unknown error"}"
            isProcessing = false
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val cropOptions = CropImageContractOptions(it, CropImageOptions()).apply {
                setGuidelines(CropImageView.Guidelines.ON)
            }
            cropImageLauncher.launch(cropOptions)
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val cropOptions = CropImageContractOptions(cameraUri, CropImageOptions()).apply {
                setGuidelines(CropImageView.Guidelines.ON)
            }
            cropImageLauncher.launch(cropOptions)
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(cameraUri)
        }
    }

    val tts = remember {
        TextToSpeech(context) {}
    }

    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF1F5F9),
                        Color(0xFFE2E8F0)
                    )
                )
            )
    ) {
        // Top App Bar with Logout Button
        TopAppBar(
            title = {
                Text(
                    text = "Image to Text",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            actions = {
                // Profile Icon Button
                IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Logout Icon Button
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Logout",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF6366F1)
            ),
            modifier = Modifier.shadow(8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Image Display Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                if (bitmap != null) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Selected Image",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        bitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Selected image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { showFullImage = true },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                } else {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PermMedia,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color(0xFFCBD5E1)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No image selected",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Choose an image to extract text from it",
                            fontSize = 14.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // Fullscreen Dialog when image is tapped
            if (showFullImage && bitmap != null) {
                Dialog(onDismissRequest = { showFullImage = false }) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    ) {
                        bitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Full image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.Center)
                                    .padding(16.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        IconButton(
                            onClick = { showFullImage = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Row for Select Image + View History Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showImagePickerDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1)
                    ),
                    enabled = !isProcessing
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (bitmap != null) "Choose Another" else "Select Image",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                OutlinedButton(
                    onClick = onHistoryClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF6366F1)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "View History",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Result Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PermMedia,
                            contentDescription = "Extracted Text",
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Text Results",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                        if (isProcessing) {
                            Spacer(modifier = Modifier.width(12.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF6366F1)
                            )
                        }
                    }

                    // Translation Dropdown - positioned below the header
                    if (resultText.isNotEmpty() && !isProcessing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Translate",
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            Box {
                                OutlinedButton(
                                    onClick = { expanded = true },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF6366F1)
                                    )
                                ) {
                                    Text("Translate to: ${languageMap[selectedLang] ?: selectedLang}")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    languageMap.forEach { (code, name) ->
                                        DropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = {
                                                selectedLang = code
                                                expanded = false

                                                if (resultText.isNotEmpty()) {
                                                    isTranslating = true
                                                    translateText(
                                                        text = resultText,
                                                        targetLanguage = code,
                                                        onResult = { translated ->
                                                            MainScope().launch {
                                                                translatedText = translated
                                                                isTranslating = false
                                                            }
                                                        },
                                                        onError = { error ->
                                                            MainScope().launch {
                                                                translatedText =
                                                                    "Translation failed. Please try again."
                                                                isTranslating = false
                                                                Toast.makeText(
                                                                    context,
                                                                    error,
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    if (resultText.isNotEmpty()) {
                                        isTranslating = true
                                        translateText(
                                            text = resultText,
                                            targetLanguage = selectedLang,
                                            onResult = { translated ->
                                                MainScope().launch {
                                                    translatedText = translated
                                                    isTranslating = false
                                                }
                                            },
                                            onError = { error ->
                                                MainScope().launch {
                                                    translatedText =
                                                        "Translation failed. Please try again."
                                                    isTranslating = false
                                                    Toast.makeText(
                                                        context,
                                                        error,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        )
                                    }
                                },
                                enabled = !isTranslating,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF8B5CF6)
                                )
                            ) {
                                if (isTranslating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Text("Translate")
                                }
                            }
                        }
                    }

                    if (isProcessing) {
                        // Processing State
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF0F7FF)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp,
                                    color = Color(0xFF6366F1)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "🔍 Processing image...",
                                    fontSize = 16.sp,
                                    color = Color(0xFF6366F1),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Please wait while we extract text",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    } else if (resultText.isNotEmpty()) {
                        // English Text Section
                        Column {
                            // English Text Header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF10B981)
                                    ),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = "EN",
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 4.dp
                                        )
                                    )
                                }
                                Text(
                                    text = "Extracted Text (English)",
                                    fontSize = 16.sp,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "${resultText.length} chars",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier
                                        .background(
                                            Color(0xFFF1F5F9),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // English Text Display
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFAFAFA)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = resultText,
                                        fontSize = 15.sp,
                                        lineHeight = 24.sp,
                                        color = Color(0xFF1F2937),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // English Text Action Buttons
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(resultText))
                                                Toast.makeText(
                                                    context,
                                                    "English text copied!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            modifier = Modifier.weight(1f).height(40.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF6366F1)
                                            )
                                        ) {
                                            Text(
                                                text = "📋 Copy",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val shareIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, resultText)
                                                    putExtra(
                                                        Intent.EXTRA_SUBJECT,
                                                        "Extracted Text (English)"
                                                    )
                                                }
                                                context.startActivity(
                                                    Intent.createChooser(
                                                        shareIntent,
                                                        "Share English text"
                                                    )
                                                )
                                            },
                                            modifier = Modifier.weight(1f).height(40.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color(0xFF6366F1)
                                            )
                                        ) {
                                            Text(
                                                text = "📤 Share",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Translated Text Section
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF8B5CF6)
                                    ),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = selectedLang.uppercase(),
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 4.dp
                                        )
                                    )
                                }
                                Text(
                                    text = "Translated Text",
                                    fontSize = 16.sp,
                                    color = Color(0xFF8B5CF6),
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (isTranslating) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF8B5CF6)
                                    )
                                }
                            }

                            // Translated Text Display
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (translatedText.isNotEmpty()) Color(
                                        0xFFF8F4FF
                                    ) else Color(0xFFF8FAFC)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    if (translatedText.isNotEmpty()) {
                                        Text(
                                            text = translatedText,
                                            fontSize = 15.sp,
                                            lineHeight = 24.sp,
                                            color = Color(0xFF1F2937),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        // Speak Button
                                        Button(
                                            onClick = {
                                                if (translatedText.isNotEmpty()) {
                                                    try {
                                                        tts.language = Locale(selectedLang)
                                                        tts.speak(
                                                            translatedText,
                                                            TextToSpeech.QUEUE_FLUSH,
                                                            null,
                                                            null
                                                        )
                                                    } catch (e: Exception) {
                                                        Toast.makeText(
                                                            context,
                                                            "TTS Error: ${e.message}",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .height(40.dp)
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(
                                                    0xFF10B981
                                                )
                                            )
                                        ) {
                                            Text(
                                                text = "🔊 Speak",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        // Translated Text Action Buttons
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    clipboardManager.setText(
                                                        AnnotatedString(
                                                            translatedText
                                                        )
                                                    )
                                                    Toast.makeText(
                                                        context,
                                                        "Translated text copied!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                },
                                                modifier = Modifier.weight(1f).height(40.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF8B5CF6)
                                                )
                                            ) {
                                                Text(
                                                    text = "📋 Copy",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    val shareIntent = Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_TEXT, translatedText)
                                                        putExtra(
                                                            Intent.EXTRA_SUBJECT,
                                                            "Translated Text"
                                                        )
                                                    }
                                                    context.startActivity(
                                                        Intent.createChooser(
                                                            shareIntent,
                                                            "Share translated text"
                                                        )
                                                    )
                                                },
                                                modifier = Modifier.weight(1f).height(40.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = Color(0xFF8B5CF6)
                                                )
                                            ) {
                                                Text(
                                                    text = "📤 Share",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    } else {
                                        // Empty state for translation
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxWidth()
                                                .padding(vertical = 20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Translate,
                                                contentDescription = null,
                                                modifier = Modifier.size(40.dp),
                                                tint = Color(0xFFCBD5E1)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Select a language to translate",
                                                fontSize = 14.sp,
                                                color = Color(0xFF64748B),
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = "Translation will appear here",
                                                fontSize = 12.sp,
                                                color = Color(0xFF94A3B8),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Empty State
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF8FAFC)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "📄",
                                    fontSize = 32.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = "No text extracted yet",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Select an image to extract text from it",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

// Image Picker Dialog
    if (showImagePickerDialog) {
        AlertDialog(
            onDismissRequest = { showImagePickerDialog = false },
            title = {
                Text(
                    text = "Select Image Source",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Choose how you want to select your image:",
                        fontSize = 16.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Camera Option
                    Card(
                        onClick = {
                            showImagePickerDialog = false
                            val permission = Manifest.permission.CAMERA
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    permission
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                cameraLauncher.launch(cameraUri)
                            } else {
                                cameraPermissionLauncher.launch(permission)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF8FAFC)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Camera",
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Take Photo",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155)
                            )
                        }
                    }

                    // Gallery Option
                    Card(
                        onClick = {
                            showImagePickerDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF8FAFC)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Gallery",
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Choose from Gallery",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showImagePickerDialog = false }
                ) {
                    Text(
                        text = "Cancel",
                        color = Color(0xFF6366F1),
                        fontWeight = FontWeight.Medium
                    )
                }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}
    }







