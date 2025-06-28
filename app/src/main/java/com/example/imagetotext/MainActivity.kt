package com.example.imagetotext

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.imagetotext.ui.screens.OcrHistoryScreen
import com.example.imagetotext.ProfileScreen
import com.google.firebase.FirebaseApp
import androidx.compose.runtime.collectAsState
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        setContent {
            val themePrefManager = remember { ThemePreferenceManager(this@MainActivity) }
            val isDarkMode by themePrefManager.isDarkMode.collectAsState(initial = false)
            ImageToTextTheme (darkTheme = isDarkMode){
                var isLoggedIn by remember { mutableStateOf(false) }
                val context = LocalContext.current

                val auth = remember { FirebaseAuth.getInstance() }

                val gso = remember {
                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(context.getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()
                }

                val googleSignInClient = remember {
                    GoogleSignIn.getClient(context, gso)
                }

                LaunchedEffect(Unit) {
                    if (auth.currentUser != null) {
                        isLoggedIn = true
                    }
                }

                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = if (isLoggedIn) "imageToText" else "login"
                ) {
                    composable("login") {
                        LoginScreen(
                            auth = auth,
                            googleSignInClient = googleSignInClient,
                            onLoginSuccess = {
                                isLoggedIn = true
                                navController.navigate("imageToText") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("imageToText") {
                        ImageToTextScreen(
                            onLogout = {
                                auth.signOut()
                                googleSignInClient.signOut().addOnCompleteListener {
                                    isLoggedIn = false
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            },
                            onHistoryClick = {
                                navController.navigate("ocrHistory")
                            },
                            onProfileClick = {
                                navController.navigate("profile")
                            }
                        )
                    }

                    composable("ocrHistory") {
                        OcrHistoryScreen(
                            onLogout = {
                                auth.signOut()
                                googleSignInClient.signOut().addOnCompleteListener {
                                    isLoggedIn = false
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable("profile") {
                        ProfileScreen(
                            onLogout = {
                                auth.signOut()
                                googleSignInClient.signOut().addOnCompleteListener {
                                    isLoggedIn = false
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            },
                            onBackClick = {
                                navController.popBackStack()
                            },
                            onHistoryClick = {
                                navController.navigate("ocrHistory")
                            }
                        )
                    }
                }
            }
        }

    }
}