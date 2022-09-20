@Suppress(
  // The alias call in plugins scope produces IntelliJ false error which is suppressed here.
  "DSL_SCOPE_VIOLATION"
)

plugins {
  application
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.spring.dependency.management)
}

dependencies {
  implementation(libs.snakeyaml)
  implementation("org.springframework.boot:spring-boot-starter-web")

  implementation(libs.commons.cli)
  implementation(libs.okhttp3)

  // From projects
  implementation(project(":platform"))
  implementation(project(":anchor-reference-server"))
}

tasks {
  bootJar {
    archiveBaseName.set("anchor-platform-runner")
  }
}

application { mainClass.set("org.stellar.anchor.platform.ServiceRunner") }
