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
  implementation(libs.ktor.server.call.logging)
  implementation(libs.google.gson)
  implementation(libs.hoplite.core)
  implementation(libs.hoplite.yaml)
  implementation(libs.jjwt)

  implementation(libs.slf4j.log4j12)
  implementation(libs.kotlin.logging)

  implementation(project(mapOf("path" to ":api-schema")))
  implementation(project(mapOf("path" to ":core")))
}

tasks {
  compileKotlin {
    dependsOn("spotlessKotlinApply")
    kotlinOptions.jvmTarget = "11"
  }

  test { useJUnitPlatform() }
}

application { mainClass.set("com.example.ReferenceServerKt") }
