import org.gradle.api.Plugin
import org.gradle.api.Project

/** Feature module: Compose UI + ViewModel + navigation, depends on domain and design system. */
class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("liftly.kmp.compose")

        kotlinMultiplatform {
            sourceSets.getByName("commonMain").dependencies {
                implementation(project(":core:domain"))
                implementation(project(":core:designsystem"))
                implementation(libs.lib("androidx-lifecycle-viewmodel"))
                implementation(libs.lib("androidx-lifecycle-viewmodel-compose"))
                implementation(libs.lib("androidx-navigation-compose"))
                implementation(libs.lib("koin-compose-viewmodel"))
                implementation(libs.lib("koin-compose-viewmodel-navigation"))
            }
        }
    }
}
