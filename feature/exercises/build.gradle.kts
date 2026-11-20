plugins {
    id("liftly.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.platform)
            implementation(libs.compose.material3.adaptive)
        }
    }
}
