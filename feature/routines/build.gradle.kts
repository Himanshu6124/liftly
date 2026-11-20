plugins {
    id("liftly.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Exercise picker sheet.
            implementation(projects.feature.exercises)
            implementation(libs.reorderable)
        }
    }
}
