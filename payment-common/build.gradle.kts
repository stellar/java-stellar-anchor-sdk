plugins {
  `java-library`
  id("org.jetbrains.kotlin.jvm") version "1.6.10"
}

dependencies {
  api(libs.lombok)

  implementation(libs.google.gson)
  implementation(libs.reactor.core)
  implementation(project(":api-schema"))

  annotationProcessor(libs.lombok)
}
