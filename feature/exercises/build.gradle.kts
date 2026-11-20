plugins {
    id("liftly.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.platform)
        }
    }
}
