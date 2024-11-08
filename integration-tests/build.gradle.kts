// The alias call in plugins scope produces IntelliJ false error which is suppressed here.
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  `java-library`
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.spring.dependency.management)
  alias(libs.plugins.kotlin.jvm)
}

dependencies {
  implementation("org.springframework.boot:spring-boot")
  implementation("org.springframework.boot:spring-boot-autoconfigure")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-reactor-netty")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-aop")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework:spring-context")

  implementation(libs.commons.cli)
  implementation(variantOf(libs.java.stellar.sdk) { classifier("uber") })
  implementation(libs.google.gson)
  implementation(libs.jakarta.servlet.api)
  implementation(libs.javax.jaxb.api)
  implementation(libs.okhttp3)
  implementation(libs.log4j2.core)
  implementation(libs.log4j2.slf4j)

  // project dependencies
  implementation(project(":api-schema"))
  implementation(project(":core"))
  implementation(project(":platform"))
  implementation(project(":anchor-reference-server"))
  implementation(project(":service-runner"))

  //  // Hibernate dependency
  implementation(libs.hibernate.core)
  implementation(libs.hibernate.orm)
  implementation(libs.hibernate.types)

  // PostgreSQL JDBC driver dependency
  implementation(libs.postgresql) // Use the appropriate version

  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation(libs.okhttp3.mockserver)
  testImplementation(libs.slf4j.api)

  implementation("org.apache.logging.log4j:log4j-core:2.24.1")
}

tasks { bootJar { enabled = false } }

configurations {
  all {
    //    exclude(group = "ch.qos.logback", module = "logback-classic")
    //    exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
    //    exclude(group = "org.slf4j", module = "slf4j-log4j12")
    //    exclude(group = "org.slf4j", module = "slf4j-simple")
    //    exclude(group = "commons-logging", module = "commons-logging")
    //    exclude(group = "org.apache.logging.log4j", module = "log4j-core")
    //    exclude(group = "org.apache.logging.log4j", module = "log4j-api")
  }
}
