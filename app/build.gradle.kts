plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdk = 31

    signingConfigs {
        create("sign") {
            storeFile = file("/home/dima/keys.jks")
            storePassword = "Kopeko12kopeko"
            keyAlias = "ecoapp"
            keyPassword = "Kopeko12kopeko"
        }
    }

    defaultConfig {
        applicationId = "dimasafonis.ecoapp"
        minSdk = 21
        targetSdk = 31
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("sign")
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
}

dependencies {
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("com.google.android.material:material:1.5.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation("com.google.mlkit:text-recognition:16.0.0-beta3")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.google.android.material:material:1.5.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
}
