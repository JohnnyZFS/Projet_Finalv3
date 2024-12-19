plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.projet_final"
    compileSdk = 35

    sourceSets {
        getByName("androidTest") {
            manifest.srcFile("src/androidTest/AndroidManifest.xml")
        }
    }

    defaultConfig {
        applicationId = "com.example.projet_final"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }


    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // AndroidX Core & Activity
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Material Design
    implementation("com.google.android.material:material:1.12.0")

    // ConstraintLayout & Navigation
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")

    // Networking with OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // WorkManager for background tasks
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Coroutine support for Android
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Testing Dependencies
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")  // Essaye avec une version antérieure

    testImplementation("org.robolectric:robolectric:4.10.3")
    testImplementation("junit:junit:4.13.2")

    // AndroidX Test Dependencies
    androidTestImplementation ("androidx.test.uiautomator:uiautomator:2.3.0")

    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")

    // Coroutine support for Android tests
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
