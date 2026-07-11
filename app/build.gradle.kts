import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Apply the Compose Compiler plugin
    alias(libs.plugins.kotlin.compose)

    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")

}

// Read ACRA config from local.properties (keeps secrets out of version control)
val localProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) load(localPropsFile.inputStream())
}

val keystoreProperties = Properties().apply {
    val keystoreFile = rootProject.file("keystore.properties")
    if (keystoreFile.exists()) load(keystoreFile.inputStream())
}

val tmdbApiKey: String = localProperties.getProperty("tmdb.api_key", "")
val traktClientId: String = localProperties.getProperty("TRAKT_CLIENT_ID", "")
val traktClientSecret: String = localProperties.getProperty("TRAKT_CLIENT_SECRET", "")

android {
    namespace = "com.lumera.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lumera.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // GitHub repository for auto-update system
        buildConfigField("String", "GITHUB_OWNER", "\"LumeraD3v\"")
        buildConfigField("String", "GITHUB_REPO", "\"Lumera\"")

        // TMDB API (loaded from local.properties)
        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")

        // Trakt API (loaded from local.properties)
        buildConfigField("String", "TRAKT_CLIENT_ID", "\"$traktClientId\"")
        buildConfigField("String", "TRAKT_CLIENT_SECRET", "\"$traktClientSecret\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        debug {
            resValue("string", "app_name", "Lumera")
        
            isMinifyEnabled = true
            isShrinkResources = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

// Compose Compiler configuration for optimal performance
composeCompiler {
    // Enable strong skipping mode for more efficient recomposition
    // Skips recomposition when parameters are stable even if equals() isn't overridden
    enableStrongSkippingMode = true

    // Enable intrinsic remember optimization
    enableIntrinsicRemember = true

    // Stability configuration: tells the compiler which classes are effectively immutable
    // so it can skip recomposition when their instances haven't changed
    stabilityConfigurationFile = project.layout.projectDirectory.file("compose_stability_config.conf")
}

dependencies {

    // 1. Android TV UI (Compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)


    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // 2. Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:4.12.0")

    // 4. Image Loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // 5. Database
    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    implementation(libs.androidx.compose.animation.core)
    kapt("androidx.room:room-compiler:2.7.0")

    // 6. Dependency Injection
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // 7. Video Player
    implementation(project(":playbackcore"))
    implementation(files("../playbackcore/libs/lib-exoplayer-release.aar"))
    implementation(files("../playbackcore/libs/lib-decoder-av1-release.aar"))
    implementation(files("../playbackcore/libs/lib-decoder-ffmpeg-release.aar"))

    // 8. Testing & Debugging
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.material:material-icons-extended")

    // OkHttp is already available via Retrofit, but declare explicitly for TorrServer API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // --- LOCAL WEB SERVER (used by remote input hub) ---
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    // --- QR CODE GENERATION ---
    implementation("com.google.zxing:core:3.5.2")

    // --- ENCRYPTED SHARED PREFERENCES ---
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

}
