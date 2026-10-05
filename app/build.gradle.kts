plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.digitalminds.grow"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.digitalminds.grow"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "1.2"
        resourceConfigurations += listOf("en")
    }

    signingConfigs {
        create("grow") {
            storeFile = file("grow.jks")
            storePassword = "growapp2026"
            keyAlias = "grow"
            keyPassword = "growapp2026"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("grow")
        }
        debug {
            signingConfig = signingConfigs.getByName("grow")
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-Xno-param-assertions", "-Xno-call-assertions", "-Xno-receiver-assertions")
    }
    packaging {
        resources.excludes += setOf("/META-INF/*.kotlin_module", "/META-INF/*.version", "DebugProbesKt.bin", "kotlin/**", "**/*.kotlin_builtins", "/META-INF/com/android/build/gradle/app-metadata.properties")
    }
}
