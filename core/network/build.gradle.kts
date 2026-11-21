plugins {
    id("liftly.kmp.library")
}

// HTTP client for our server: session (sign-in, token refresh) now, sync transport next.
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.protocol)
            api(projects.core.domain)
            implementation(projects.core.data) // RemoteSyncDataSource, SyncEngine
            api(projects.core.platform) // AuthSession takes SecureStore and NativeSignIn
            implementation(project.dependencies.platform(libs.ktor.bom))
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        androidMain.dependencies { implementation(libs.ktor.client.okhttp) }
        iosMain.dependencies { implementation(libs.ktor.client.darwin) }
        getByName("desktopMain").dependencies { implementation(libs.ktor.client.java) }
        commonTest.dependencies { implementation(libs.ktor.client.mock) }
    }
}
