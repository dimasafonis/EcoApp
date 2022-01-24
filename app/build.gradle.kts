plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    signingConfigs {
        create("sign") {
            storeFile = file("/home/dima/EcoApp/keystore.jks")
            storePassword = "987665444444"
            keyPassword = "987665444444"
            keyAlias = "key0"
        }
    }
    compileSdk = 31

    defaultConfig {
        applicationId = "dimasafonis.ecoapp"
        minSdk = 21
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("sign")
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("sign")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

}

dependencies {
    val camerax = "1.0.2"

    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-view:1.0.0-alpha32")
    implementation("com.google.android.material:material:1.5.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation("com.google.mlkit:text-recognition:16.0.0-beta2")
    implementation("org.yaml:snakeyaml:1.30")
    implementation("com.google.code.gson:gson:2.8.9")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
}