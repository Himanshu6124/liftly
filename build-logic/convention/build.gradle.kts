plugins {
    `kotlin-dsl`
}

group = "com.himanshu.liftly.buildlogic"

kotlin {
    jvmToolchain(21)
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "liftly.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "liftly.kmp.compose"
            implementationClass = "KmpComposeConventionPlugin"
        }
        register("kmpFeature") {
            id = "liftly.kmp.feature"
            implementationClass = "KmpFeatureConventionPlugin"
        }
        register("room") {
            id = "liftly.room"
            implementationClass = "RoomConventionPlugin"
        }
    }
}
