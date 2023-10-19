// The alias call in plugins scope produces IntelliJ false error which is suppressed here.
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  `java-library`
  alias(libs.plugins.kotlin.jvm)
}

dependencies {
  implementation(libs.bundles.junit)
  implementation(libs.coroutines.core)

  implementation(project(":api-schema"))
}
