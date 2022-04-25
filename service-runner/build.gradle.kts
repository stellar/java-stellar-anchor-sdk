plugins {
  application
  id("org.springframework.boot") version "2.6.3"
  id("io.spring.dependency-management") version "1.0.11.RELEASE"
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation(libs.commons.cli)

  // From projects
  implementation(project(":payment-circle"))
  implementation(project(":platform"))
  implementation(project(":anchor-reference-server"))
}

tasks {
  bootJar {
    archiveBaseName.set("anchor-platform-runner")
  }
}

application { mainClass.set("org.stellar.anchor.platform.ServiceRunner") }
