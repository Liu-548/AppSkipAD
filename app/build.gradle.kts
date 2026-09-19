import java.util.Properties

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
        versionCode = 3
        versionName = "0.1.2"
    }

    // R-73: debug + release share one key so APKs install over each other.
    // The key is NOT in git: file in keystore/ (gitignored), password in
    // local.properties (skipqc.keystorePassword) or env SKIPQC_KEYSTORE_PASSWORD (CI).
    val keystoreFile = rootProject.file("keystore/skipqc.keystore")
    val keystorePassword = System.getenv("SKIPQC_KEYSTORE_PASSWORD")
        ?: Properties().apply {
            rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
        }.getProperty("skipqc.keystorePassword")

    val sharedSigning = if (keystoreFile.exists() && keystorePassword != null) {
        signingConfigs.create("shared") {
            storeFile = keystoreFile
            storePassword = keystorePassword
            keyAlias = "skipqc"
            keyPassword = keystorePassword
        }
    } else null

    buildTypes {
        debug {
            sharedSigning?.let { signingConfig = it }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            sharedSigning?.let { signingConfig = it }
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
