plugins {
    id("liftly.kmp.library")
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
    }
}
