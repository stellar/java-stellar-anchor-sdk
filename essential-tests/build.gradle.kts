// The alias call in plugins scope produces IntelliJ false error which is suppressed here.
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  `java-library`
  `java-test-fixtures`
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.spring.dependency.management)
  alias(libs.plugins.kotlin.jvm)
}

repositories { maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") } }

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
  testFixturesImplementation("org.springframework.boot:spring-boot")
  testFixturesImplementation("org.springframework.boot:spring-boot-autoconfigure")
  testFixturesImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
  testFixturesImplementation(
      libs.snakeyaml) // used to force the version of snakeyaml (used by springboot) to a safer one.
  testFixturesImplementation("org.springframework.boot:spring-boot-starter-web")

  testFixturesImplementation(libs.commons.cli)
  testFixturesImplementation(libs.dotenv)
  testFixturesImplementation(libs.google.gson)
  testFixturesImplementation(libs.httpclient)
  testFixturesImplementation(libs.okhttp3)
  testFixturesImplementation(libs.log4j2.api)
  testFixturesImplementation(libs.log4j2.core)
  testFixturesImplementation(libs.log4j2.slf4j)
  testFixturesImplementation(libs.docker.compose.rule)
  testFixturesImplementation(libs.kotlin.serialization.json)
  testFixturesImplementation(libs.ktor.client.core)
  testFixturesImplementation(libs.ktor.client.okhttp)
  testFixturesImplementation(variantOf(libs.java.stellar.sdk) { classifier("uber") })
  testFixturesImplementation(libs.stellar.wallet.sdk)

  testFixturesImplementation("org.assertj:assertj-core:3.24.2")

  // project dependencies
  testFixturesImplementation(project(":api-schema"))
  testFixturesImplementation(project(":core"))
  testFixturesImplementation(project(":platform"))
  testFixturesImplementation(project(":kotlin-reference-server"))
  testFixturesImplementation(project(":wallet-reference-server"))
  testFixturesImplementation(project(":service-runner"))
  testFixturesImplementation(project(":lib-util"))

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.boot:spring-boot-starter-web")

  testImplementation(libs.google.gson)
  testImplementation(libs.stellar.wallet.sdk)
  testImplementation(libs.okhttp3.mockserver)
  testImplementation(libs.docker.compose.rule)
  testImplementation(libs.dotenv)

  testImplementation(project(":lib-util"))
  testImplementation(project(":api-schema"))
  testImplementation(project(":core"))
  testImplementation(project(":platform"))
  testImplementation(project(":service-runner"))
  testImplementation(project(":wallet-reference-server"))
  testImplementation(project(":kotlin-reference-server"))
}

tasks { bootJar { enabled = false } }

apply(from = "$rootDir/scripts.gradle.kts")

@Suppress("UNCHECKED_CAST")
val enableTestConcurrency = extra["enableTestConcurrency"] as (Test) -> Unit

tasks.test {
  enableTestConcurrency(this)
  exclude("**/org/stellar/anchor/platform/*Test.class")
  exclude("**/org/stellar/anchor/platform/integrationtest/**")
  exclude("**/org/stellar/anchor/platform/e2etest/**")
}
