import androidx.room.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Room KMP with KSP for every target and exported schemas. */
class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("androidx.room")

        extensions.configure<RoomExtension> {
            schemaDirectory("$projectDir/schemas")
        }

        kotlinMultiplatform {
            sourceSets.getByName("commonMain").dependencies {
                implementation(libs.lib("room-runtime"))
                implementation(libs.lib("sqlite-bundled"))
            }
        }

        dependencies {
            val compiler = libs.lib("room-compiler")
            listOf("kspAndroid", "kspIosArm64", "kspIosSimulatorArm64", "kspDesktop")
                .forEach { add(it, compiler) }
        }
    }
}
