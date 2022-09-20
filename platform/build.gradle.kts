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
  implementation(libs.snakeyaml)
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-reactor-netty")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-aop")

  implementation(libs.spring.aws.messaging)
  implementation(libs.spring.kafka)
  implementation(libs.aws.rds)
  implementation(libs.aws.iam.auth)
  implementation(libs.commons.cli)
  implementation(libs.commons.io)
  implementation(libs.flyway.core)
  implementation(libs.hibernate.types)
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
  implementation(project(":anchor-reference-server"))

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation(libs.okhttp3.mockserver)
  testImplementation(libs.okhttp3.tls)
}

tasks.test {
  testLogging {
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    events = setOf(org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED)
  }
}

tasks { bootJar { enabled = false } }
