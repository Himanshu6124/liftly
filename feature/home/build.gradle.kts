plugins {
    id("liftly.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Routine card.
            implementation(projects.feature.routines)
        }
    }
}
