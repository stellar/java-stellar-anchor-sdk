// The alias call in plugins scope produces IntelliJ false error which is suppressed here.
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  `java-library`
  alias(libs.plugins.kotlin.jvm)
}

dependencies {
  api(libs.lombok)

  implementation(libs.apache.commons.lang3)
  implementation(libs.httpcore)
  implementation(libs.jackson.annotations)
  implementation(libs.jakarta.annotation.api)
  implementation(libs.jakarta.validation.api)
  implementation(libs.google.gson)

  annotationProcessor(libs.lombok)
}


