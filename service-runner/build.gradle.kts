@Suppress(
  // The alias call in plugins scope produces IntelliJ false error which is suppressed here.
  "DSL_SCOPE_VIOLATION"
)
plugins {
  application
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.spring.dependency.management)
  alias(libs.plugins.kotlin.jvm)
}

dependencies {
  implementation(
    libs.snakeyaml
  ) // used to force the version of snakeyaml (used by springboot) to a safer one.
  implementation("org.springframework.boot:spring-boot-starter-web")

  implementation(libs.commons.cli)
  implementation(libs.docker.compose.rule)
  implementation(libs.dotenv)
  implementation(libs.google.gson)
  implementation(libs.okhttp3)
  implementation(libs.coroutines.core)

  // From projects
  implementation(project(":api-schema"))
  implementation(project(":core"))
  implementation(project(":platform"))
  implementation(project(":anchor-reference-server"))
  implementation(project(":kotlin-reference-server"))
}

tasks {
  bootJar {
    archiveBaseName.set("anchor-platform-runner")
  }
}

application { mainClass.set("org.stellar.anchor.platform.ServiceRunner") }
