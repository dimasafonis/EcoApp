plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdk = 33

    signingConfigs {
        create("sign") {
            storeFile = file("C:\\Users\\dimas\\androidDevelopment.jks")
            storePassword = "Kopeko13kopeko"
            keyAlias = "dimasafonisKey"
            keyPassword = "987665444444"
        }
    }

    defaultConfig {
        applicationId = "dimasafonis.ecoapp"
        minSdk = 21
        targetSdk = 33
        versionCode = 3
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
    namespace = "dimasafonis.ecoapp"
}

dependencies {
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    implementation(kotlin("reflect"))
}
