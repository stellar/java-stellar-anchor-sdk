plugins {
  `java-library`
  id("org.jetbrains.kotlin.jvm") version "1.6.10"
}

dependencies {
  api(libs.lombok)

  implementation(libs.log4j.core)
  implementation(libs.google.gson)
  implementation(libs.reactor.core)
  implementation(libs.commons.validator)
  implementation(libs.slf4j.log4j12)
  implementation(libs.okhttp3.mockserver)
  implementation(libs.reactor.netty)

  // Lombok should be used by all sub-projects to reduce Java verbosity
  annotationProcessor(libs.lombok)

  // From jitpack.io
  implementation(libs.java.stellar.sdk)

  // From projects
  implementation(project(":core"))
}
