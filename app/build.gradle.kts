plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.skipqc"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.skipqc"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    // R-73: one shared key for debug + release so APKs install over each other.
    // Personal project: key is committed on purpose.
    signingConfigs {
        create("shared") {
            storeFile = rootProject.file("keystore/skipqc.keystore")
            storePassword = "android"
            keyAlias = "skipqc"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("shared")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("shared")
        }
    }

    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    // H2: no runtime dependencies.
    testImplementation(libs.junit)
    testImplementation(libs.orgjson)
}
