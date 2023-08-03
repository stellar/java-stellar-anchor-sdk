// The alias call in plugins scope produces IntelliJ false error which is suppressed here.
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  application
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ktor)
}

dependencies {
  implementation(libs.bundles.ktor)
  implementation(libs.bundles.ktor.client)
  implementation(libs.google.gson)
  implementation(libs.hoplite.core)
  implementation(libs.hoplite.yaml)
  implementation(libs.java.stellar.sdk)
  implementation(libs.jjwt)
  implementation(libs.kotlin.logging)
  implementation(libs.ktor.server.call.logging)
  implementation(libs.slf4j.log4j12)

  implementation(project(mapOf("path" to ":api-schema")))
  implementation(project(mapOf("path" to ":core")))
  implementation(project(mapOf("path" to ":platform")))
}

tasks {
  compileKotlin {
    dependsOn("spotlessKotlinApply")
    kotlinOptions.jvmTarget = "11"
  }

  test { useJUnitPlatform() }
}

application { mainClass.set("com.example.ReferenceServerKt") }
