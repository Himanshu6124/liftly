import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.lib(alias: String) = findLibrary(alias).get()

internal fun VersionCatalog.int(alias: String) = findVersion(alias).get().requiredVersion.toInt()

internal fun Project.kotlinMultiplatform(block: KotlinMultiplatformExtension.() -> Unit) =
    extensions.configure<KotlinMultiplatformExtension>(block)

internal fun KotlinMultiplatformExtension.androidLibraryTarget(block: KotlinMultiplatformAndroidLibraryTarget.() -> Unit) =
    (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android", block)

/** `:core:data` -> `com.himanshu.liftly.core.data` */
internal val Project.liftlyNamespace: String
    get() = "com.himanshu.liftly" + path.replace(":composeApp", "").replace(':', '.').replace('-', '_')
