// The alias call in plugins scope produces IntelliJ false error which is suppressed here.
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  `java-library`
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.spring.dependency.management)
  alias(libs.plugins.kotlin.jvm)
}

repositories { maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") } }

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation(libs.google.gson)
  testImplementation(libs.stellar.wallet.sdk)
  testImplementation(libs.okhttp3.mockserver)
  testImplementation(libs.docker.compose.rule)
  testImplementation(libs.dotenv)

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.boot:spring-boot-starter-web")

  // project dependencies
  testImplementation(project(":lib-util"))
  testImplementation(project(":api-schema"))
  testImplementation(project(":core"))
  testImplementation(project(":platform"))
  testImplementation(project(":service-runner"))
  testImplementation(project(":wallet-reference-server"))
  testImplementation(project(":kotlin-reference-server"))
  testImplementation(testFixtures(project(":essential-tests")))
}

tasks { bootJar { enabled = false } }

apply(from = "$rootDir/scripts.gradle.kts")

@Suppress("UNCHECKED_CAST")
val enableTestConcurrency = extra["enableTestConcurrency"] as (Test) -> Unit

tasks.test {
// Useful exclusions for debugging tests
//  exclude("**/org/stellar/anchor/platform/AnchorPlatformApiRpcEnd2EndTest**")
//  exclude("**/org/stellar/anchor/platform/AnchorPlatformCustodyApiRpcEnd2EndTest**")
//  exclude("**/org/stellar/anchor/platform/AnchorPlatformCustodyEnd2EndTest**")
//  exclude("**/org/stellar/anchor/platform/AnchorPlatformCustodyIntegrationTest**")
//  exclude("**/org/stellar/anchor/platform/CustodyApiKeyAuthIntegrationTest**")
//  exclude("**/org/stellar/anchor/platform/CustodyJwtAuthIntegrationTest**")
//  exclude("**/org/stellar/anchor/platform/PostgresMigrationTest**")
}
