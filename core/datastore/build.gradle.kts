plugins {
    id("liftly.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            api(libs.datastore.preferences)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
    }
}
