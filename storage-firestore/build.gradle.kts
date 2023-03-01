plugins {
    kotlin("jvm")
}

group = "az.santabot"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":storage"))

    implementation("com.fasterxml.jackson.core:jackson-annotations:2.14.1")
    implementation("com.google.cloud:google-cloud-firestore:3.8.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}