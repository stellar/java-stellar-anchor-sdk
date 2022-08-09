plugins {
  `java-library`
  id("org.springframework.boot") version "2.6.3"
  id("io.spring.dependency-management") version "1.0.11.RELEASE"
  id("org.jetbrains.kotlin.jvm") version "1.6.10"
}

dependencies {
  implementation("org.springframework.boot:spring-boot")
  implementation("org.springframework.boot:spring-boot-autoconfigure")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-web")

  implementation(libs.commons.cli)
  implementation(libs.java.stellar.sdk)
  implementation(libs.google.gson)
  implementation(libs.okhttp3)
  implementation(libs.log4j2.core)
  implementation(libs.log4j2.slf4j)

  // project dependencies
  implementation(project(":api-schema"))
  implementation(project(":core"))
  implementation(project(":platform"))
  implementation(project(":anchor-reference-server"))
  implementation(project(":service-runner"))

  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation(libs.okhttp3.mockserver)
  testImplementation("org.springframework.boot:spring-boot-starter-test")

}

tasks { bootJar { enabled = false } }


