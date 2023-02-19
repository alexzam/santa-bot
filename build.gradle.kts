plugins {
    kotlin("jvm") version "1.8.0"
    application
}

group = "az.santabot"
version = "0.1-SNAPSHOT"

val ktorVersion = "2.2.3"
val fuelVersion = "2.3.1"

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.clojars.org")
        name = "Clojars"
    }
}

application {
    mainClass.set("az.santabot.MainKt")
}

dependencies {
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    implementation("com.github.kittinunf.fuel:fuel:$fuelVersion")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:$fuelVersion")
    implementation("com.github.kittinunf.fuel:fuel-jackson:$fuelVersion")
    implementation("org.postgresql:postgresql:9.1")
    implementation("clj-postgresql:clj-postgresql:0.7.0")
//    implementation("postgresql:postgresql:9.3-1102.jdbc41")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

kotlin {
    jvmToolchain(8)
}