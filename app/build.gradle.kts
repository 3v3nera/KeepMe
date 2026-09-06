plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.telegramdrive.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.telegramdrive.app"
        minSdk = 25
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output?.outputFileName = "KeepMe-v${variant.versionName}-b${variant.versionCode}-${variant.buildType.name}.apk"
        }
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation("androidx.compose.material3:material3:1.3.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-video:2.6.0")

    // --- Kotlin Coroutines ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // Room Database
    val roomVersion = "2.7.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // WorkManager (camera backup, trash purge)
    implementation("androidx.work:work-runtime-ktx:2.10.1")

    // DataStore (settings)
    implementation("androidx.datastore:datastore-preferences:1.1.4")

    // Media3 ExoPlayer (video streaming)
    val media3Version = "1.5.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")

    // Charts (storage dashboard)
    implementation("com.patrykandpatrick.vico:compose-m3:2.1.2")

    // Shimmer loading
    implementation("com.valentinilk.shimmer:compose-shimmer:1.3.1")

    // Splash screen
    implementation("androidx.core:core-splashscreen:1.2.0-alpha02")

    // Google Fonts & Extended Icons
    implementation("androidx.compose.ui:ui-text-google-fonts:1.7.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // Mesh Gradient Backgrounds
    implementation("io.github.om252345:composemeshgradient:0.3.0")


    // Accompanist Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")

    // Prismal Liquid Glass
    implementation(project(":prismal"))

    // Navigation and Hilt (as requested by EasyTDLib instructions)
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("com.google.dagger:hilt-android:2.58")
    ksp("com.google.dagger:hilt-compiler:2.58")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Security Crypto for encrypted DataStore/Preferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // DocumentFile for Storage Access Framework (SAF) folder traversal
    implementation("androidx.documentfile:documentfile:1.0.1")
    
    // Country Code Picker
    implementation("com.github.togisoft:jetpack_compose_country_code_picker:1.1.4")
}

configurations.all {
    resolutionStrategy {
        force("androidx.core:core-ktx:1.17.0")
        force("androidx.core:core:1.17.0")
    }
}