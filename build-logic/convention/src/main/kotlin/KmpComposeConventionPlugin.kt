import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.compose.resources.ResourcesExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposeExtension

/** Shared module that contains Compose UI. */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("liftly.kmp.library")
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        kotlinMultiplatform {
            androidLibraryTarget {
                androidResources { enable = true }
            }
            sourceSets.getByName("commonMain").dependencies {
                implementation(libs.lib("compose-runtime"))
                implementation(libs.lib("compose-foundation"))
                implementation(libs.lib("compose-ui"))
                implementation(libs.lib("compose-material3"))
                implementation(libs.lib("compose-material-icons-core"))
                implementation(libs.lib("compose-components-resources"))
                implementation(libs.lib("compose-ui-tooling-preview"))
                implementation(libs.lib("androidx-lifecycle-runtime-compose"))
                implementation(libs.lib("koin-compose"))
            }
        }

        extensions.configure<ComposeExtension> {
            (this as ExtensionAware).extensions.configure<ResourcesExtension> {
                packageOfResClass = "$liftlyNamespace.resources"
                publicResClass = true
            }
        }
    }
}
