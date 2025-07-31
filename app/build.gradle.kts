import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
    alias(libs.plugins.navigation.safeargs)
}

android {
    namespace = "com.example.linguify"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.linguify"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        val properties = Properties().apply {
            load(rootProject.file("app/secrets.properties").inputStream())
        }

        buildConfigField(
            type = "String",
            name = "WORDS_API_KEY",
            value = "\"${properties.getProperty("WORDS_API_KEY")}\""
        )
        buildConfigField(
            type = "String",
            name = "WORDS_API_HOST",
            value = "\"${properties.getProperty("WORDS_API_HOST")}\""
        )

        buildConfigField(
            type = "String",
            name = "PEXELS_API_KEY",
            value = "\"${properties.getProperty("PEXELS_API_KEY")}\""
        )

        buildConfigField(
            type = "String",
            name = "GEMINI_API_KEY",
            value = "\"${properties.getProperty("GEMINI_API_KEY")}\""
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
        dataBinding = true
        viewBinding = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    hilt {
        enableAggregatingTask = true
    }
}

dependencies {
    implementation(libs.dotsindicator)
    implementation(libs.lottie)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.gridlayout)
    implementation(libs.gridlayout)
    implementation(libs.androidx.swiperefreshlayout)
    kapt(libs.hilt.android.compiler)
    kapt(libs.androidx.hilt.compiler)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.common)
    implementation(libs.bundles.retrofit)
    implementation(libs.glide)
    kapt(libs.glide.compiler)
    implementation(libs.bundles.room)
    kapt(libs.room.compiler)
    implementation(libs.bundles.media)
    implementation(libs.datastore.preferences)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.coroutines.test)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.google.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    kapt(libs.javaPoet)
}