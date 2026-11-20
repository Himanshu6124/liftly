plugins {
    id("liftly.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.platform)
            // Exercise picker + form preview sheets.
            implementation(projects.feature.exercises)
            implementation(libs.reorderable)
        }
    }
}
