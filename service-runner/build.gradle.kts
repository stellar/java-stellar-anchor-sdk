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
  jar {
    archiveBaseName.set("anchor-platform-runner")
  }

  bootJar {
    archiveBaseName.set("anchor-platform-runner")
  }
}

configurations {
  all {
    exclude(group = "ch.qos.logback", module = "logback-classic")
    exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
    exclude(group = "org.slf4j", module = "slf4j-log4j12")
  }
}

application { mainClass.set("org.stellar.anchor.platform.ServiceRunner") }
