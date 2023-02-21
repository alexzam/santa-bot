plugins {
    kotlin("jvm")
    application
}

group = "az.santabot"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

val ktorVersion = "2.2.3"

application {
    mainClass.set("az.santabot.app.MainKt")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":storage-pg"))
    implementation(project(":util"))

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}