// The alias call in plugins scope produces IntelliJ false error which is suppressed here.
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  application
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.spring.dependency.management)
  alias(libs.plugins.kotlin.jvm)
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
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-aop")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework:spring-context")
  // https://mvnrepository.com/artifact/jakarta.servlet/jakarta.servlet-api
  implementation("jakarta.servlet:jakarta.servlet-api:6.1.0")
// https://mvnrepository.com/artifact/javax.xml.bind/jaxb-api
  implementation("javax.xml.bind:jaxb-api:2.3.1")

//  implementation(libs.spring.aws.messaging)
  implementation(libs.spring.kafka)
  implementation(libs.aws.rds)
  implementation(libs.aws.iam.auth)
  implementation(libs.commons.cli)
  implementation(libs.commons.io)
  implementation(libs.flyway.core)
  implementation(libs.hibernate.types)
  implementation(libs.google.gson)
  implementation(variantOf(libs.java.stellar.sdk) { classifier("uber") })

  implementation(libs.sqlite.jdbc)
  implementation(libs.okhttp3)
  implementation(libs.jackson.dataformat.yaml)
  implementation(libs.log4j2.core)
  implementation(libs.log4j2.slf4j)

  // From projects
  implementation(project(":api-schema"))
  implementation(project(":core"))
  implementation(project(":anchor-reference-server"))

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation(libs.okhttp3.mockserver)
  testImplementation(libs.slf4j.api)
}

tasks.test {
  testLogging {
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    events = setOf(org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED)
  }
}

tasks { bootJar { enabled = false } }
