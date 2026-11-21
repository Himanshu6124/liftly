plugins {
    id("liftly.kmp.library")
    id("liftly.room")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            api(projects.core.protocol)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
    }
}
