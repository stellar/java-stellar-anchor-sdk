plugins {
  `java-library`
  id("org.springframework.boot") version "2.6.3"
  id("io.spring.dependency-management") version "1.0.11.RELEASE"
  id("org.jetbrains.kotlin.jvm") version "1.6.10"
}

dependencies {
  api(libs.lombok)

  annotationProcessor(libs.lombok)

  implementation("org.springframework.boot:spring-boot")
  implementation("org.springframework.boot:spring-boot-autoconfigure")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-web")

  implementation(libs.google.gson)
  implementation(libs.commons.cli)
  implementation(libs.java.stellar.sdk)
  implementation(libs.okhttp3)

  // From projects
  implementation(project(":core"))
  implementation(project(":platform-apis"))
  implementation(project(":platform"))
  implementation(project(":anchor-reference-server"))
}

tasks { bootJar { enabled = false } }

configurations {
  all {
    exclude(group = "ch.qos.logback", module = "logback-classic")
    exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
    exclude(group = "org.slf4j", module = "slf4j-log4j12")
  }
}
