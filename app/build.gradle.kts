plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.kotlinapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.kotlinapp"
        minSdk = 24
        targetSdk = 35
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
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true // можно включить и viewBinding для совместимости
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.firebase:firebase-auth:21.0.0")
    implementation("com.google.firebase:firebase-firestore:24.0.0")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.6.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.6.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.squareup.picasso:picasso:2.8")

    // Firebase
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation ("de.hdodenhof:circleimageview:3.1.0")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

}