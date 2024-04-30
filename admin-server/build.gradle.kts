// The alias call in plugins scope produces IntelliJ false error which is suppressed here.
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  //  kotlin("jvm") version "1.9.23"
  //  id("io.ktor.plugin") version "2.3.10"
  //  id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.ktor)
  alias(libs.plugins.kotlin.serialization)
}

// val ktor_version = "2.3.10"
// val kotlin_version = "1.9.23"
// val logback_version = "1.5.6"

dependencies {
  //  implementation("io.ktor:ktor-server-core-jvm")
  //  implementation("io.ktor:ktor-server-host-common-jvm")
  //  implementation("io.ktor:ktor-server-cors-jvm")
  //  implementation("io.ktor:ktor-server-openapi")
  //  implementation("io.ktor:ktor-server-call-logging-jvm")
  //  implementation("io.ktor:ktor-server-content-negotiation-jvm")
  //  implementation("io.ktor:ktor-serialization-gson-jvm")
  //  implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
  //  implementation("org.jetbrains.exposed:exposed-core:0.41.1")
  //  implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")
  //  implementation("com.h2database:h2:2.1.214")
  //  implementation("io.ktor:ktor-server-netty-jvm")
  //  implementation("ch.qos.logback:logback-classic:$logback_version")
  //
  //  testImplementation("io.ktor:ktor-server-tests-jvm")
  //  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
  //

  implementation(libs.bundles.ktor)
  implementation(libs.kotlin.logging)
  implementation("ch.qos.logback:logback-classic:1.5.6")

  implementation("org.jetbrains.exposed:exposed-core:0.41.1")
  implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")
  implementation("com.h2database:h2:2.1.214")
  implementation("io.ktor:ktor-server-netty-jvm")

  implementation(variantOf(libs.java.stellar.sdk) { classifier("uber") })
  implementation(project(mapOf("path" to ":api-schema")))

  testImplementation("io.ktor:ktor-server-tests-jvm:2.3.10")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.23")
}

tasks {
  compileKotlin {
    dependsOn("spotlessKotlinApply")
    kotlinOptions.jvmTarget = "11"
  }

  test { useJUnitPlatform() }
}
