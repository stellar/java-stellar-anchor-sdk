// The alias call in plugins scope produces IntelliJ false error which is suppressed here.
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.ktor)
  alias(libs.plugins.kotlin.serialization)
}

dependencies {
  implementation(libs.bundles.exposed)
  implementation(libs.bundles.ktor)

  implementation(libs.kotlin.logging)
  implementation(libs.logback.classic)

  implementation(variantOf(libs.java.stellar.sdk) { classifier("uber") })
  implementation(project(mapOf("path" to ":api-schema")))
  implementation(project(mapOf("path" to ":core")))

  testImplementation(libs.ktor.server.test)
  testImplementation(libs.kotlin.junit)
}

tasks {
  compileKotlin {
    dependsOn("spotlessKotlinApply")
    kotlinOptions.jvmTarget = "17"
  }

  test { useJUnitPlatform() }
}
