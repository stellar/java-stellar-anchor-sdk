// The alias call in plugins scope produces IntelliJ false error which is suppressed here.
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  `java-library`
  alias(libs.plugins.kotlin.jvm)
}

tasks {
  processResources {
    doFirst {
      val existingFile = file("$buildDir/resources/main/metadata.properties")
      existingFile.delete()
    }
    filter { line -> line.replace("%APP_VERSION_TOKEN%", rootProject.version.toString()) }
  }
}

dependencies {
  api(libs.lombok)

  implementation(libs.google.gson)
  implementation(libs.reactor.core)

  annotationProcessor(libs.lombok)
}
