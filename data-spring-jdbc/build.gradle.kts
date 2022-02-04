plugins {
  `java-library`
  id("org.springframework.boot") version "2.6.3"
  id("io.spring.dependency-management") version "1.0.11.RELEASE"
}

dependencies {
  api(libs.lombok)

  implementation(libs.google.gson)
  implementation("org.springframework.boot:spring-boot")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")

  annotationProcessor(libs.lombok)

  // From projects
  implementation(project(":core"))
}

tasks { bootJar { enabled = false } }
