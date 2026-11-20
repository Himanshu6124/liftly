plugins {
    id("liftly.kmp.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            api(projects.core.resources)
            implementation(libs.coil.compose)
        }
    }
}
