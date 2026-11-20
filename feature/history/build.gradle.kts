plugins {
    id("liftly.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Reuses the Batch Log Sheet to edit past workouts.
            implementation(projects.feature.workout)
        }
    }
}
