plugins {
    kotlin("jvm")
}

group = "az.santabot"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core"))
    implementation(project(":storage-pg"))
    implementation(project(":util"))

    implementation("com.google.cloud.functions:functions-framework-api:1.0.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.1")
}

val jarTask = tasks.getByName<Jar>("jar") {
    manifest {
        attributes("Class-Path" to
                configurations.runtimeClasspath.map { conf ->
                    conf.resolve().joinToString(" ") { "libs/${it.name}" }
                })
    }
}

tasks.create<Copy>("prepareDeployment") {
    into("${buildDir}/tmp/deployment")
    into(".") {
        from(jarTask)
    }
    into("libs") {
        from(configurations.runtimeClasspath)
    }
}

tasks.create<Zip>("makeDeployment") {
    group = "build"
    dependsOn("prepareDeployment")
    this.from("${buildDir}/tmp/deployment")
    archiveFileName.set("function.zip")
}