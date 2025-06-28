plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id ("com.google.gms.google-services") // At the top or bottom

}

android {


    namespace = "com.example.imagetotext"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.imagetotext"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10" // ✅ Update as needed
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation ("androidx.activity:activity-compose:1.7.2")
    implementation ("androidx.compose.ui:ui:1.5.1")

    implementation("androidx.compose.material3:material3:1.2.1") // or latest

    implementation ("androidx.compose.ui:ui-tooling-preview:1.5.1")
    implementation (platform("androidx.compose:compose-bom:2023.10.01"))
    implementation ("androidx.compose.ui:ui")
    implementation ("androidx.compose.ui:ui-graphics")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation ("androidx.compose.runtime:runtime-livedata:1.5.1")
    implementation("com.google.firebase:firebase-analytics")
    implementation(platform("com.google.firebase:firebase-bom:33.15.0"))
    implementation ("com.google.firebase:firebase-auth-ktx")
    implementation ("com.google.android.gms:play-services-auth:20.7.0")

    // ML Kit Text Recognition
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // Coroutines
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Google Material (optional but nice to have)
    implementation("com.google.android.material:material:1.9.0")

    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.firebaseui:firebase-ui-auth:8.0.2")
    implementation ("com.google.firebase:firebase-analytics-ktx")
    // Firebase Storage and Firestore
    implementation ("com.google.firebase:firebase-storage-ktx")
    implementation ("com.google.firebase:firebase-firestore-ktx")
    implementation ("com.google.firebase:firebase-auth-ktx")
    // Firebase BoM
    implementation (platform("com.google.firebase:firebase-bom:32.7.3"))

    // Testing
    testImplementation ("junit:junit:4.13.2")
    androidTestImplementation ("androidx.test.ext:junit:1.1.5")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation (platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation ("androidx.compose.ui:ui-test-junit4")
    debugImplementation ("androidx.compose.ui:ui-tooling")
    debugImplementation ("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation ("androidx.datastore:datastore-preferences:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation ("com.github.CanHub:Android-Image-Cropper:4.3.2")



//    implementation ("androidx.compose.material:material-icons-extended:$compose_version")






}