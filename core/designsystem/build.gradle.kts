plugins {
    id("liftly.kmp.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
        }
    }
}
