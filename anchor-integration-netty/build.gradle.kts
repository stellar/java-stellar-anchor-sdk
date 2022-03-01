plugins {
  `java-library`
}

dependencies {
  api(libs.lombok)

  implementation(libs.google.gson)
  implementation(libs.reactor.netty)

  annotationProcessor(libs.lombok)

  // From projects
  implementation(project(":core"))
  implementation(project(":platform-apis"))
}
