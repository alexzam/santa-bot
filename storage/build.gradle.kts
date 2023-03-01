plugins {
    kotlin("jvm")
}

group = "az.santabot"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":util"))

    implementation("org.postgresql:postgresql:42.5.4")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.14.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}