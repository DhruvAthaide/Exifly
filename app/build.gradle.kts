plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.dhruvathaide.exifly"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dhruvathaide.exifly"
        minSdk = 25
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    // Material 3
    implementation("com.google.android.material:material:1.11.0")

    // WindowCompat
    implementation("androidx.core:core-ktx:1.17.0")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // Apache Commons Imaging (EXIF removal)
    implementation("org.apache.commons:commons-imaging:1.0-alpha3")

    // Lottie
    implementation("com.airbnb.android:lottie:6.3.0")

    // Lifecycle (optional but recommended)
    implementation("androidx.lifecycle:lifecycle-runtime:2.10.0")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}