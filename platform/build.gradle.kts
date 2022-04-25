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
  implementation(libs.commons.cli)
  implementation(libs.google.gson)
  implementation(libs.java.stellar.sdk)
  implementation(libs.sqlite.jdbc)
  implementation(libs.okhttp3)
  implementation(libs.jackson.dataformat.yaml)
  implementation(libs.log4j2.core)
  implementation(libs.log4j2.slf4j)


  // TODO: Used by the test suite. To be removed when the test suite is moved to a different
  // project.
  implementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
  implementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
  implementation("org.jetbrains.kotlin:kotlin-test-junit5:1.6.10")

  // From projects
  implementation(project(":core"))
  implementation(project(":platform-apis"))
  implementation(project(":payment-circle"))
  implementation(project(":anchor-reference-server"))

  testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
  testImplementation("com.h2database:h2")
  testImplementation("org.springframework.boot:spring-boot-starter-test")

  testImplementation(libs.okhttp3.mockserver)
}

tasks.test {
  testLogging {
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    events = setOf(org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED)
  }
}

application { mainClass.set("org.stellar.anchor.platform.ServiceRunner") }

configurations {
  all {
    exclude(group = "ch.qos.logback", module = "logback-classic")
    exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
    exclude(group = "org.slf4j", module = "slf4j-log4j12")
  }
}
