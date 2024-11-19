plugins {
  application
  id("org.springframework.boot") version "3.2.4"
  id("io.spring.dependency-management") version "1.1.0"
  id("org.jetbrains.kotlin.jvm") version "1.9.10"
}

dependencies {
  api(libs.lombok)

  implementation("org.springframework.boot:spring-boot")
  implementation("org.springframework.boot:spring-boot-autoconfigure")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation(libs.sqlite.jdbc)
  implementation(libs.google.gson)
  implementation(libs.java.stellar.sdk)

  annotationProcessor(libs.lombok)
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  // https://mvnrepository.com/artifact/jakarta.servlet/jakarta.servlet-api
  implementation("jakarta.servlet:jakarta.servlet-api:6.1.0")



  // From projects
  implementation(project(":core"))
  implementation(project(":payment-circle"))
  implementation(project(":config-spring-property"))
  implementation(project(":data-spring-jdbc"))
}

application { mainClass.set("org.stellar.anchor.server.AnchorPlatformApplicationMVC") }
