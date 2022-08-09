plugins {
  application
  id("org.springframework.boot") version "2.6.3"
  id("io.spring.dependency-management") version "1.0.11.RELEASE"
  id("org.jetbrains.kotlin.jvm") version "1.6.10"
}

dependencies {
  api(libs.lombok)

  implementation("org.springframework.kafka:spring-kafka:2.8.4")
  implementation("org.springframework.boot:spring-boot")
  implementation("org.springframework.boot:spring-boot-autoconfigure")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-web")

  implementation(libs.h2database)
  implementation(libs.sqlite.jdbc)
  implementation(libs.google.gson)
  implementation(libs.java.stellar.sdk)
  implementation(libs.okhttp3)

  implementation(project(":api-schema"))
  implementation(project(":core"))

  annotationProcessor(libs.lombok)
}

application { mainClass.set("org.stellar.anchor.reference.AnchorReferenceServer") }

