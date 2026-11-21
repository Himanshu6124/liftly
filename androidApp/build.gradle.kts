plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.himanshu.liftly.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.himanshu.liftly"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
        // Google OAuth *Web* client id of our server (Credential Manager asks Google for an ID
        // token for it). Set liftly.googleServerClientId in ~/.gradle/gradle.properties; empty
        // hides Google sign-in.
        buildConfigField(
            "String",
            "GOOGLE_SERVER_CLIENT_ID",
            "\"${providers.gradleProperty("liftly.googleServerClientId").getOrElse("")}\"",
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.composeApp)
    implementation(projects.core.platform) // notification permission hook, widget snapshot
    implementation(projects.core.domain) // SyncController for the background SyncWorker
    implementation(libs.androidx.activity.compose)
    implementation(libs.glance.appwidget) // home-screen widget
    implementation(libs.androidx.work.runtime) // periodic background sync
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.android)
}
