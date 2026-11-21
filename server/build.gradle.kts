plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.himanshu.liftly.server.ApplicationKt")
}

dependencies {
    implementation(projects.core.protocol)
    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.hikari)
    implementation(libs.postgresql)
    implementation(libs.logback.classic)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.embedded.postgres)
    testImplementation(platform(libs.embedded.postgres.binaries.bom))
    // Test-only: drive the app's real SyncEngine (Room, in-memory) against this server.
    testImplementation(projects.core.data)
    testImplementation(libs.room.runtime)
    testImplementation(libs.sqlite.bundled)
}

// The server ships only its own code + core/protocol. Fail the build if an app module ever leaks in.
val checkNoAppModules by tasks.registering {
    val projectDeps = configurations.runtimeClasspath.map { cfg ->
        cfg.incoming.resolutionResult.allComponents
            .mapNotNull { (it.id as? org.gradle.api.artifacts.component.ProjectComponentIdentifier)?.projectPath }
    }
    doLast {
        val unexpected = projectDeps.get().toSet() - setOf(":server", ":core:protocol")
        check(unexpected.isEmpty()) { "server runtime must not depend on app modules: $unexpected" }
    }
}
tasks.named("check") { dependsOn(checkNoAppModules) }

// Local dev without Docker: embedded PostgreSQL + the server on :8080 (see server/README.md).
tasks.register<JavaExec>("runDev") {
    group = "application"
    description = "Runs the server against an embedded PostgreSQL with dev auth enabled."
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.himanshu.liftly.server.DevServerKt")
}
