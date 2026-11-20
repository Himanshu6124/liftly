plugins {
    id("liftly.kmp.library")
    id("liftly.room")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
    }
}
