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

  implementation(libs.commons.cli)
  implementation(libs.google.gson)
  implementation(libs.java.stellar.sdk)
  implementation(libs.okhttp3)

  implementation(libs.sqlite.jdbc)
  implementation(libs.okhttp3)
  implementation(libs.jackson.dataformat.yaml)
  implementation(libs.log4j2.core)
  implementation(libs.log4j2.slf4j)

  // From projects
  implementation(project(":core"))
  implementation(project(":platform-apis"))
  implementation(project(":platform"))
  implementation(project(":anchor-reference-server"))
  implementation(project(":service-runner"))

  testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
  testImplementation("com.h2database:h2")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation(libs.okhttp3.mockserver)
}

tasks { bootJar { enabled = false } }

configurations {
  all {
    exclude(group = "ch.qos.logback", module = "logback-classic")
    exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
    exclude(group = "org.slf4j", module = "slf4j-log4j12")
  }
}
