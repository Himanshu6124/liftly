import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * Base for every shared module: Android (KMP library), iOS (arm64, simulatorArm64) and Desktop JVM.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        kotlinMultiplatform {
            jvmToolchain(21)

            androidLibraryTarget {
                namespace = liftlyNamespace
                compileSdk = libs.int("android-compileSdk")
                minSdk = libs.int("android-minSdk")
                compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
                withHostTest { }
            }

            iosArm64()
            iosSimulatorArm64()
            jvm("desktop") {
                compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
            }

            compilerOptions {
                freeCompilerArgs.addAll(
                    "-Xexpect-actual-classes",
                    "-opt-in=kotlin.time.ExperimentalTime",
                )
            }

            sourceSets.getByName("commonMain").dependencies {
                implementation(libs.lib("kotlinx-coroutines-core"))
                implementation(libs.lib("kotlinx-datetime"))
                implementation(libs.lib("kotlinx-serialization-json"))
                implementation(libs.lib("kotlinx-collections-immutable"))
                implementation(libs.lib("kermit"))
                implementation(project.dependencies.platform(libs.lib("koin-bom")))
                implementation(libs.lib("koin-core"))
            }
            sourceSets.getByName("commonTest").dependencies {
                implementation(kotlin("test"))
                implementation(libs.lib("kotlinx-coroutines-test"))
                implementation(libs.lib("turbine"))
            }
        }

        tasks.withType<Test>().configureEach {
            testLogging { events("failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
        }
    }
}
