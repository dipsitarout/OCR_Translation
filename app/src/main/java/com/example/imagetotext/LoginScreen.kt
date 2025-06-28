package com.example.imagetotext

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Add this to strings.xml
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            ImageToTextTheme {
                LoginScreen(
                    onLoginSuccess = {
                        // Navigate to MainActivity
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    auth = auth,
                    googleSignInClient = googleSignInClient
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    auth: FirebaseAuth,
    googleSignInClient: GoogleSignInClient
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var isGitHubLoading by remember { mutableStateOf(false) }
    var isSignUpMode by remember { mutableStateOf(false) }

    // Google Sign In Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            Log.d("LoginActivity", "firebaseAuthWithGoogle:" + account.id)

            val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { authTask ->
                    isGoogleLoading = false
                    if (authTask.isSuccessful) {
                        Log.d("LoginActivity", "signInWithCredential:success")
                        Toast.makeText(context, "Google Sign In Successful!", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    } else {
                        Log.w("LoginActivity", "signInWithCredential:failure", authTask.exception)
                        Toast.makeText(context, "Authentication Failed: ${authTask.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        } catch (e: ApiException) {
            Log.w("LoginActivity", "Google sign in failed", e)
            Toast.makeText(context, "Google Sign In Failed: ${e.message}", Toast.LENGTH_LONG).show()
            isGoogleLoading = false
        }
    }

    // Email validation function
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Password validation function
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    // Email/Password Sign In Function
    fun signInWithEmailPassword() {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidEmail(email)) {
            Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidPassword(password)) {
            Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    Log.d("LoginActivity", "signInWithEmail:success")
                    Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                    onLoginSuccess()
                } else {
                    Log.w("LoginActivity", "signInWithEmail:failure", task.exception)
                    Toast.makeText(context, "Login Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Email/Password Sign Up Function
    fun signUpWithEmailPassword() {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidEmail(email)) {
            Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidPassword(password)) {
            Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    Log.d("LoginActivity", "createUserWithEmail:success")
                    Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                    onLoginSuccess()
                } else {
                    Log.w("LoginActivity", "createUserWithEmail:failure", task.exception)
                    Toast.makeText(context, "Sign Up Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Google Sign In Function
    fun signInWithGoogle() {
        isGoogleLoading = true
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    // GitHub Sign In Function
    fun signInWithGitHub() {
        isGitHubLoading = true

        val provider = OAuthProvider.newBuilder("github.com")
        provider.addCustomParameter("allow_signup", "false")

        // Optional: Request specific scopes
        val scopes = arrayListOf<String>()
        scopes.add("user:email")
        provider.scopes = scopes

        val pendingResultTask = auth.pendingAuthResult
        if (pendingResultTask != null) {
            // There's something already here! Finish the sign-in for your user.
            pendingResultTask
                .addOnSuccessListener { authResult ->
                    isGitHubLoading = false
                    Log.d("LoginActivity", "GitHub sign in success: ${authResult.user?.displayName}")
                    Toast.makeText(context, "GitHub Sign In Successful!", Toast.LENGTH_SHORT).show()
                    onLoginSuccess()
                }
                .addOnFailureListener { exception ->
                    isGitHubLoading = false
                    Log.w("LoginActivity", "GitHub sign in failed", exception)
                    Toast.makeText(context, "GitHub Sign In Failed: ${exception.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            // There's no pending result so you need to start the sign-in flow.
            auth.startActivityForSignInWithProvider(context as ComponentActivity, provider.build())
                .addOnSuccessListener { authResult ->
                    isGitHubLoading = false
                    Log.d("LoginActivity", "GitHub sign in success: ${authResult.user?.displayName}")
                    Toast.makeText(context, "GitHub Sign In Successful!", Toast.LENGTH_SHORT).show()
                    onLoginSuccess()
                }
                .addOnFailureListener { exception ->
                    isGitHubLoading = false
                    Log.w("LoginActivity", "GitHub sign in failed", exception)
                    Toast.makeText(context, "GitHub Sign In Failed: ${exception.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6366F1),
                        Color(0xFF8B5CF6),
                        Color(0xFFF1F5F9)
                    )
                )
            )
            .verticalScroll(rememberScrollState())
    ) {
        // Top Section with Logo and Welcome Text
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 80.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Logo/Icon - Replace with your image
            Card(
                modifier = Modifier
                    .size(100.dp)
                    .shadow(20.dp, RoundedCornerShape(25.dp)),
                shape = RoundedCornerShape(25.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo), // Replace with your app icon drawable
                        contentDescription = "App Icon",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (isSignUpMode) "Create Account" else "Welcome Back!",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (isSignUpMode) "Sign up to start using Inkray" else "Sign in to continue using Inkray",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(60.dp))

        // Login Form Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .shadow(16.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isSignUpMode) "Sign Up" else "Sign In",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = Color(0xFF6366F1)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedLabelColor = Color(0xFF6366F1)
                    ),
                    enabled = !isLoading
                )

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF6366F1)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedLabelColor = Color(0xFF6366F1)
                    ),
                    enabled = !isLoading
                )

                // Forgot Password (only show in sign in mode)
                if (!isSignUpMode) {
                    TextButton(
                        onClick = {
                            if (email.isNotBlank()) {
                                auth.sendPasswordResetEmail(email)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Toast.makeText(context, "Password reset email sent!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Failed to send reset email", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            } else {
                                Toast.makeText(context, "Please enter your email first", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = "Forgot Password?",
                            color = Color(0xFF6366F1),
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Sign In/Sign Up Button
                Button(
                    onClick = {
                        if (isSignUpMode) {
                            signUpWithEmailPassword()
                        } else {
                            signInWithEmailPassword()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1)
                    ),
                    enabled = !isLoading && !isGoogleLoading && !isGitHubLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isSignUpMode) "Sign Up" else "Sign In",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFE2E8F0)
                    )
                    Text(
                        text = "  or continue with  ",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFE2E8F0)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Social Login Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Google Sign In Button
                    SocialLoginButton(
                        text = "Continue with Google",
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.google),
                                contentDescription = "Google Icon",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        backgroundColor = Color.White,
                        textColor = Color(0xFF1E293B),
                        borderColor = Color(0xFFE2E8F0),
                        isLoading = isGoogleLoading,
                        enabled = !isLoading && !isGoogleLoading && !isGitHubLoading,
                        onClick = { signInWithGoogle() }
                    )

                    // GitHub Sign In Button
                    SocialLoginButton(
                        text = "Continue with GitHub",
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.githubicon),
                                contentDescription = "GitHub Icon",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        backgroundColor = Color(0xFF1F2937),
                        textColor = Color.White,
                        borderColor = Color(0xFF1F2937),
                        isLoading = isGitHubLoading,
                        enabled = !isLoading && !isGoogleLoading && !isGitHubLoading,
                        onClick = { signInWithGitHub() }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Sign Up/Sign In Link
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSignUpMode) "Already have an account? " else "Don't have an account? ",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )
                    TextButton(
                        onClick = {
                            isSignUpMode = !isSignUpMode
                            // Clear fields when switching modes
                            email = ""
                            password = ""
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (isSignUpMode) "Sign In" else "Sign Up",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6366F1)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun SocialLoginButton(
    text: String,
    icon: @Composable () -> Unit,
    backgroundColor: Color,
    textColor: Color,
    borderColor: Color,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = borderColor
        ),
        enabled = enabled
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = textColor,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}