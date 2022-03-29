plugins {
  `java-library`
  id("org.springframework.boot") version "2.6.3"
  id("io.spring.dependency-management") version "1.0.11.RELEASE"
  id("org.jetbrains.kotlin.jvm") version "1.6.10"
}

dependencies {
  api(libs.lombok)

  implementation(libs.google.gson)
  implementation("org.springframework.boot:spring-boot")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  testImplementation("com.h2database:h2")
  testImplementation("org.springframework.boot:spring-boot-starter-test")

  annotationProcessor(libs.lombok)

  // From projects
  implementation(project(":core"))
}

tasks { bootJar { enabled = false } }
