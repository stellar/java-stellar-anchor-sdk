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
  implementation(libs.java.stellar.sdk)
  implementation(libs.jjwt)
  implementation(libs.javax.jaxb.api)

  implementation(libs.slf4j2.simple)
  implementation(libs.kotlin.logging)
}

application { mainClass.set("com.example.ReferenceServerKt") }
