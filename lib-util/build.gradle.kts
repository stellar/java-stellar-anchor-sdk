// The alias call in plugins scope produces IntelliJ false error which is suppressed here.
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  `java-library`
  alias(libs.plugins.kotlin.jvm)
}

dependencies {
  implementation(libs.bundles.junit)
  implementation(libs.commons.text)
  implementation(libs.google.gson)
  implementation(libs.httpcore)
  implementation(libs.jjwt)
  implementation(variantOf(libs.java.stellar.sdk) { classifier("uber") })
  implementation(project(":api-schema"))
}
