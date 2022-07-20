plugins {
  application
  id("org.springframework.boot") version "2.6.3"
  id("io.spring.dependency-management") version "1.0.11.RELEASE"
  id("org.jetbrains.kotlin.jvm") version "1.6.10"
}

dependencies {
  api(libs.lombok)

  annotationProcessor(libs.lombok)
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  implementation("org.springframework.boot:spring-boot")
  implementation("org.springframework.boot:spring-boot-autoconfigure")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-reactor-netty")
  implementation("com.amazonaws:aws-java-sdk-rds:1.12.248")
  implementation("software.amazon.msk:aws-msk-iam-auth:1.1.4")
  implementation("org.springframework.boot:spring-boot-starter-actuator:2.7.0")
  implementation("org.springframework.boot:spring-boot-starter-aop:2.7.0")
  implementation("org.flywaydb:flyway-core:8.5.13")


  implementation(libs.commons.cli)
  implementation(libs.commons.io)
  implementation(libs.google.gson)
  implementation(libs.java.stellar.sdk)

  implementation(libs.sqlite.jdbc)
  implementation(libs.okhttp3)
  implementation(libs.jackson.dataformat.yaml)
  implementation(libs.log4j2.core)
  implementation(libs.log4j2.slf4j)

  // From projects
  implementation(project(":api-schema"))
  implementation(project(":core"))
  implementation(project(":payment"))
  implementation(project(":anchor-reference-server"))

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation(libs.okhttp3.mockserver)
}

tasks.test {
  testLogging {
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    events = setOf(org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED)
  }
}

tasks { bootJar { enabled = false } }
