plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
}

dependencies {
    // ***************************
    // Production dependencies
    // ***************************
    // From mavenCentral
    implementation(libs.reactor.netty)
    implementation(libs.google.gson)
    implementation(libs.reactor.core)

    // Lombok should be used by all sub-projects to reduce Java verbosity
    annotationProcessor(libs.lombok)

    // From jitpack.io
    implementation(libs.java.stellar.sdk)

    // From projects
    implementation(project(":core"))
}

tasks.test {
    useJUnitPlatform()
}