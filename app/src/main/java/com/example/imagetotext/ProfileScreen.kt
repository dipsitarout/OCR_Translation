package com.example.imagetotext

// Add these imports to your ProfileScreen file

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import com.google.firebase.auth.userProfileChangeRequest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.firestore.ListenerRegistration
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import java.text.SimpleDateFormat
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.util.*

// For status bar padding (add this if not already present)
import androidx.compose.foundation.layout.statusBarsPadding
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    onHistoryClick: () -> Unit = {}
) {

    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var isClearingHistory by remember { mutableStateOf(false) }

    // State to trigger profile reload
    var profileReloadTrigger by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themePrefManager = remember { ThemePreferenceManager(context) }
    val isDarkMode by themePrefManager.isDarkMode.collectAsState(initial = false)
    // Existing variables ke saath add karo
    var localDarkMode by remember { mutableStateOf(false) }

// LaunchedEffect add karo to sync both states
    LaunchedEffect(isDarkMode) {
        localDarkMode = isDarkMode
    }

    // Load user profile data with real-time updates
    LaunchedEffect(profileReloadTrigger) {
        loadUserProfileWithRealTimeUpdates { profile ->
            userProfile = profile
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = if (localDarkMode) listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F0F23)
                    ) else listOf(
                        Color(0xFF667EEA),
                        Color(0xFF764BA2),
                        Color(0xFF1E3A8A)
                    ),
                    radius = 1500f
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Custom Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            CircleShape
                        )
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Profile",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )

                IconButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            CircleShape
                        )
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Logout",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (isLoading) {
                // Loading State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading profile...",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // Profile Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // Profile Avatar and Basic Info Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(20.dp, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Avatar with gradient border
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF667EEA),
                                                Color(0xFF764BA2)
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.White, CircleShape)
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = userProfile?.name?.firstOrNull()?.uppercase() ?: "U",
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF667EEA)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // User Name - Centered
                            Text(
                                text = userProfile?.name ?: "User Name",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Email - Centered
                            Text(
                                text = userProfile?.email ?: "user@example.com",
                                fontSize = 16.sp,
                                color = Color(0xFF6B7280),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Member since badge
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF0F7FF)
                                )
                            ) {
                                Text(
                                    text = "Member since ${userProfile?.memberSince ?: "2024"}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF667EEA),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Stats Cards Row - Now showing dynamic data
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Usage Count Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .shadow(12.dp, RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(
                                            Color(0xFF10B981).copy(alpha = 0.1f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "📊",
                                        fontSize = 24.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "${userProfile?.usageCount ?: 0}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )

                                Text(
                                    text = "Times Used",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B7280),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Images Processed Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .shadow(12.dp, RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(
                                            Color(0xFF667EEA).copy(alpha = 0.1f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "🖼️",
                                        fontSize = 24.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "${userProfile?.imagesProcessed ?: 0}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF667EEA)
                                )

                                Text(
                                    text = "Images",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B7280),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Activity & Settings Section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "Activity & Settings",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937),
                                modifier = Modifier.padding(bottom = 20.dp)
                            )

                            // Last Activity
                            ProfileInfoRow(
                                icon = "🕒",
                                title = "Last Activity",
                                value = userProfile?.lastActivity ?: "Today",
                                iconColor = Color(0xFFEF4444)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Account Type
                            ProfileInfoRow(
                                icon = "👤",
                                title = "Account Type",
                                value = userProfile?.accountType ?: "Free",
                                iconColor = Color(0xFF8B5CF6)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Language
                            ProfileInfoRow(
                                icon = "🌍",
                                title = "Language",
                                value = "English",
                                iconColor = Color(0xFF06B6D4)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            Color(0xFFF59E0B).copy(alpha = 0.1f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (localDarkMode) "🌙" else "☀️",
                                        fontSize = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Theme",
                                        fontSize = 14.sp,
                                        color = Color(0xFF6B7280),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (localDarkMode) "Dark Mode" else "Light Mode",
                                        fontSize = 16.sp,
                                        color = Color(0xFF1F2937),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Switch(
                                    checked = localDarkMode,
                                    onCheckedChange = { newValue ->
                                        localDarkMode = newValue  // Immediately update local state
                                        scope.launch {
                                            themePrefManager.setDarkMode(newValue)
                                        }
                                    },

                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF667EEA),
                                        checkedTrackColor = Color(0xFF667EEA).copy(alpha = 0.5f)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))


                            // App Version
                            ProfileInfoRow(
                                icon = "📱",
                                title = "App Version",
                                value = "v1.0.0",
                                iconColor = Color(0xFF10B981)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Edit Profile Button
                        Button(
                            onClick = { showEditDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .shadow(8.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✏️",
                                    fontSize = 20.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = "Edit Profile",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF667EEA)
                                )
                            }
                        }

                        // Clear History Button
                        OutlinedButton(
                            onClick = { showClearHistoryDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isClearingHistory) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Text(
                                        text = "🗑️",
                                        fontSize = 20.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                                Text(
                                    text = if (isClearingHistory) "Clearing..." else "Clear History",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    // Edit Profile Dialog - Fixed
    if (showEditDialog) {
        var newName by remember { mutableStateOf(userProfile?.name ?: "") }
        var isUpdating by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isUpdating) showEditDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✏️",
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Edit Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Update your profile information",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUpdating,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newName.trim() != userProfile?.name) {
                            isUpdating = true
                            updateUserProfile(newName.trim()) { success ->
                                isUpdating = false
                                if (success) {
                                    showEditDialog = false
                                    // Trigger profile reload
                                    profileReloadTrigger++
                                    Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else if (newName.trim() == userProfile?.name) {
                            showEditDialog = false
                            Toast.makeText(context, "No changes to update", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isUpdating && newName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF667EEA)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Update",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditDialog = false },
                    enabled = !isUpdating
                ) {
                    Text(
                        text = "Cancel",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6B7280)
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Clear History Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { if (!isClearingHistory) showClearHistoryDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🗑️",
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Clear History",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to clear all your extraction history? This action cannot be undone and will reset your usage statistics.",
                    fontSize = 16.sp,
                    color = Color(0xFF6B7280),
                    lineHeight = 24.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isClearingHistory = true
                        clearUserHistory { success ->
                            isClearingHistory = false
                            showClearHistoryDialog = false
                            if (success) {
                                // Trigger profile reload
                                profileReloadTrigger++
                                Toast.makeText(context, "History cleared successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to clear history", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isClearingHistory,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isClearingHistory) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Yes, Clear All",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearHistoryDialog = false },
                    enabled = !isClearingHistory
                ) {
                    Text(
                        text = "Cancel",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6B7280)
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🚪",
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Logout",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to logout? You'll need to sign in again to use the app.",
                    fontSize = 16.sp,
                    color = Color(0xFF6B7280),
                    lineHeight = 24.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Yes, Logout",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text(
                        text = "Cancel",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6B7280)
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun ProfileInfoRow(
    icon: String,
    title: String,
    value: String,
    iconColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    iconColor.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color(0xFF6B7280),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 16.sp,
                color = Color(0xFF1F2937),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// Data class for user profile
data class UserProfile(
    val name: String,
    val email: String,
    val usageCount: Int,
    val imagesProcessed: Int,
    val memberSince: String,
    val lastActivity: String,
    val accountType: String
)

// Enhanced function to load user profile with real-time updates
private fun loadUserProfileWithRealTimeUpdates(callback: (UserProfile?) -> Unit): ListenerRegistration? {
    val currentUser = FirebaseAuth.getInstance().currentUser

    if (currentUser == null) {
        callback(null)
        return null
    }

    val db = FirebaseFirestore.getInstance()

    // Set up real-time listener for extractions to get dynamic stats
    return db.collection("extractions")
        .whereEqualTo("userId", currentUser.uid)
        .addSnapshotListener { extractionsSnapshot, error ->
            if (error != null) {
                // Fallback to basic profile on error
                loadBasicUserProfile(currentUser, callback)
                return@addSnapshotListener
            }

            if (extractionsSnapshot != null) {
                // Calculate dynamic stats
                val usageCount = extractionsSnapshot.size()
                val imagesProcessed = extractionsSnapshot.documents
                    .mapNotNull { it.getString("imageUrl") }
                    .distinct()
                    .size

                // Get last activity
                val lastExtraction = extractionsSnapshot.documents
                    .maxByOrNull { it.getTimestamp("timestamp")?.toDate()?.time ?: 0L }

                val lastActivity = lastExtraction?.getTimestamp("timestamp")?.let { timestamp ->
                    val now = System.currentTimeMillis()
                    val diff = now - timestamp.toDate().time
                    when {
                        diff < 24 * 60 * 60 * 1000 -> "Today"
                        diff < 2 * 24 * 60 * 60 * 1000 -> "Yesterday"
                        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)} days ago"
                        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(timestamp.toDate())
                    }
                } ?: if (usageCount > 0) "Recently" else "Never"

                // Get user document for additional info
                db.collection("users").document(currentUser.uid)
                    .get()
                    .addOnSuccessListener { userDoc ->
                        // Get member since date
                        val memberSince = userDoc.getTimestamp("createdAt")?.let { timestamp ->
                            SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(timestamp.toDate())
                        } ?: SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date())

                        val profile = UserProfile(
                            name = userDoc.getString("name") ?: currentUser.displayName ?: "User",
                            email = currentUser.email ?: "user@example.com",
                            usageCount = usageCount,
                            imagesProcessed = imagesProcessed,
                            memberSince = memberSince,
                            lastActivity = lastActivity,
                            accountType = userDoc.getString("accountType") ?: "Free"
                        )

                        callback(profile)
                    }
                    .addOnFailureListener {
                        // Fallback with stats but basic user info
                        val profile = UserProfile(
                            name = currentUser.displayName ?: "User",
                            email = currentUser.email ?: "user@example.com",
                            usageCount = usageCount,
                            imagesProcessed = imagesProcessed,
                            memberSince = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date()),
                            lastActivity = lastActivity,
                            accountType = "Free"
                        )
                        callback(profile)
                    }
            } else {
                loadBasicUserProfile(currentUser, callback)
            }
        }
}

// Fallback function for basic profile loading
private fun loadBasicUserProfile(currentUser: com.google.firebase.auth.FirebaseUser, callback: (UserProfile?) -> Unit) {
    val db = FirebaseFirestore.getInstance()

    db.collection("users").document(currentUser.uid)
        .get()
        .addOnSuccessListener { userDoc ->
            val profile = UserProfile(
                name = userDoc.getString("name") ?: currentUser.displayName ?: "User",
                email = currentUser.email ?: "user@example.com",
                usageCount = 0,
                imagesProcessed = 0,
                memberSince = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date()),
                lastActivity = "Never",
                accountType = userDoc.getString("accountType") ?: "Free"
            )
            callback(profile)
        }
        .addOnFailureListener {
            // Fallback with basic user info only if Firestore fails
            val profile = UserProfile(
                name = currentUser.displayName ?: "User",
                email = currentUser.email ?: "user@example.com",
                usageCount = 0,
                imagesProcessed = 0,
                memberSince = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date()),
                lastActivity = "Never",
                accountType = "Free"
            )
            callback(profile)
        }
}

// Function to update user profile
private fun updateUserProfile(newName: String, callback: (Boolean) -> Unit) {
    val currentUser = FirebaseAuth.getInstance().currentUser

    if (currentUser == null) {
        callback(false)
        return
    }

    val db = FirebaseFirestore.getInstance()
    val userRef = db.collection("users").document(currentUser.uid)

    // Create user data map
    val userData = hashMapOf(
        "name" to newName,
        "email" to (currentUser.email ?: ""),
        "uid" to currentUser.uid,
        "updatedAt" to com.google.firebase.Timestamp.now()
    )

    // Use set with merge option - this will create the document if it doesn't exist
    // or update only the specified fields if it does exist
    userRef.set(userData, com.google.firebase.firestore.SetOptions.merge())
        .addOnSuccessListener {
            // Also update the Firebase Auth profile
            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                displayName = newName
            }

            currentUser.updateProfile(profileUpdates)
                .addOnSuccessListener {
                    callback(true)
                }
                .addOnFailureListener {
                    // Even if Auth update fails, Firestore update succeeded
                    callback(true)
                }
        }
        .addOnFailureListener { exception ->
            println("Firestore update failed: ${exception.message}")
            callback(false)
        }
}

private fun clearUserHistory(callback: (Boolean) -> Unit) {
    val currentUser = FirebaseAuth.getInstance().currentUser

    if (currentUser == null) {
        println("Clear History Error: No current user")
        callback(false)
        return
    }

    val db = FirebaseFirestore.getInstance()
    val userId = currentUser.uid

    println("Attempting to clear history for user: $userId")

    // Change "extractions" to "ocr_history"
    db.collection("ocr_history")  // <- This was the issue!
        .whereEqualTo("userId", userId)
        .get()
        .addOnSuccessListener { snapshot ->
            println("Found ${snapshot.size()} extractions to delete")

            if (snapshot.isEmpty) {
                println("No extractions found - history already clear")
                callback(true)
                return@addOnSuccessListener
            }

            // Delete in smaller batches to avoid timeout
            val documents = snapshot.documents
            val batchSize = 500 // Firestore batch limit
            val batches = documents.chunked(batchSize)

            deleteBatches(db, batches, 0, callback)
        }
        .addOnFailureListener { exception ->
            println("Clear History Error: Failed to get extractions - ${exception.message}")
            callback(false)
        }
}
// Helper function to delete documents in batches
private fun deleteBatches(
    db: FirebaseFirestore,
    batches: List<List<DocumentSnapshot>>, // Fixed import issue
    currentBatchIndex: Int,
    callback: (Boolean) -> Unit
) {
    if (currentBatchIndex >= batches.size) {
        println("All batches deleted successfully")
        callback(true)
        return
    }

    val batch = db.batch()
    val currentBatch = batches[currentBatchIndex]

    println("Deleting batch ${currentBatchIndex + 1}/${batches.size} (${currentBatch.size} documents)")

    currentBatch.forEach { document ->
        batch.delete(document.reference)
    }

    batch.commit()
        .addOnSuccessListener {
            println("Batch ${currentBatchIndex + 1} deleted successfully")
            // Process next batch
            deleteBatches(db, batches, currentBatchIndex + 1, callback)
        }
        .addOnFailureListener { exception ->
            println("Clear History Error: Batch ${currentBatchIndex + 1} failed - ${exception.message}")
            callback(false)
        }
}