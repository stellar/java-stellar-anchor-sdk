// The alias call in plugins scope produces IntelliJ false error which is suppressed here.
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  application
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.ktor)
}

dependencies {
  implementation(libs.bundles.ktor)

  implementation(libs.slf4j2.simple)
  implementation(libs.kotlin.logging)
}

application { mainClass.set("com.example.ReferenceServerKt") }
