import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("liftly.kmp.compose")
}

kotlin {
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            // Swift implements OngoingWorkoutIndicator (Live Activity) and registers it.
            export(projects.core.platform)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.designsystem)
            implementation(projects.core.domain)
            implementation(projects.core.data)
            implementation(projects.core.datastore)
            api(projects.core.platform) // exported to Swift
            implementation(projects.core.resources)
            implementation(projects.feature.home)
            implementation(projects.feature.workout)
            implementation(projects.feature.exercises)
            implementation(projects.feature.routines)
            implementation(projects.feature.history)
            implementation(projects.feature.settings)
            implementation(libs.compose.material3.adaptive.navigation.suite)
            implementation(libs.compose.material3.adaptive)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.koin.compose.viewmodel)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
        getByName("desktopMain").dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
        getByName("desktopTest").dependencies {
            implementation(kotlin("test"))
            implementation(libs.compose.ui.test)
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.himanshu.liftly.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Liftly"
            packageVersion = "1.0.0"
        }
    }
}
