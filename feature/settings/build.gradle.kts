plugins {
    id("liftly.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.platform) // native "Sign in with Apple" button on iOS
        }
    }
}
