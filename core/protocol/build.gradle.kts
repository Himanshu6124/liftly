plugins {
    id("liftly.kmp.library")
}

// Wire format shared by the app and the backend (server/ in a later milestone). Keep it free of
// Room, Compose and Koin so a plain JVM server can depend on it.
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.serialization.json)
        }
    }
}
