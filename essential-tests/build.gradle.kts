// The alias call in plugins scope produces IntelliJ false error which is suppressed here.
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  `java-library`
  `java-test-fixtures`
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.spring.dependency.management)
  alias(libs.plugins.kotlin.jvm)
  id("org.gradle.test-retry") version "1.6.2"
}

dependencies {
  testFixturesImplementation(libs.assertj.core)
  testFixturesImplementation(libs.httpclient)
  testFixturesImplementation(libs.kotlin.serialization.json)
  testFixturesImplementation(libs.dotenv)
  testFixturesImplementation(libs.coroutines.core)

  // Stellar dependencies
  testFixturesImplementation(libs.stellar.wallet.sdk)
  testFixturesImplementation(variantOf(libs.java.stellar.sdk) { classifier("uber") })
  testFixturesImplementation(libs.okhttp3.mockserver)

  // Spring dependencies
  testFixturesImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
  testFixturesImplementation("org.springframework.boot:spring-boot-starter-web")

  // project dependencies
  testFixturesImplementation(project(":api-schema"))
  testFixturesImplementation(project(":core"))
  testFixturesImplementation(project(":platform"))
  testFixturesImplementation(project(":kotlin-reference-server"))
  testFixturesImplementation(project(":wallet-reference-server"))
  testFixturesImplementation(project(":service-runner"))
  testFixturesImplementation(project(":lib-util"))

  testImplementation(libs.stellar.wallet.sdk)
}

tasks { bootJar { enabled = false } }

tasks.test {
  exclude("**/org/stellar/anchor/platform/*Test.class")
  exclude("**/org/stellar/anchor/platform/integrationtest/**")
  exclude("**/org/stellar/anchor/platform/e2etest/**")

  retry {
    maxRetries.set(2)
    maxFailures.set(20)
    failOnPassedAfterRetry.set(false)
  }
}

val essentialSuites =
  mapOf(
    "Infrastructure" to
      listOf(
        "integrationtest.PlatformServerHealthTests",
        "integrationtest.KafkaTests",
        "integrationtest.LedgerClientTests",
      ),
    "Sep10" to listOf("integrationtest.Sep10Tests", "integrationtest.Sep10ServiceIntegrationTests"),
    "Sep12" to
      listOf(
        "integrationtest.Sep12Tests",
        "integrationtest.Sep12AuthTests",
        "integrationtest.Sep12DeleteOwnershipTests",
        "integrationtest.Sep12TokenResolutionTests",
        "e2etest.Sep12CallbackSignatureTests",
      ),
    "Sep38" to listOf("integrationtest.Sep38Tests", "integrationtest.Sep38PlatformApiTests"),
    "Sep6" to
      listOf(
        "integrationtest.Sep6Tests",
        "integrationtest.Sep6PlatformApiTests",
        "integrationtest.AnchorReferenceServerClientRequestTests",
        "e2etest.Sep6End2EndTest",
      ),
    "Sep24" to
      listOf(
        "integrationtest.Sep24Tests",
        "integrationtest.Sep24PlatformApiTests",
        "integrationtest.Sep24AccountIsolationTests",
        "integrationtest.Sep24ClientRequestTests",
        "integrationtest.Sep24TransactionSchemaTests",
        "e2etest.Sep24End2EndTests",
      ),
    "Sep31" to
      listOf(
        "integrationtest.Sep31Tests",
        "integrationtest.Sep31PlatformApiTests",
        "integrationtest.Sep31CustomerOwnershipTests",
        "e2etest.Sep31End2EndTests",
      ),
    "Sep45" to listOf("integrationtest.Sep45Tests"),
    "Platform" to
      listOf(
        "integrationtest.CallbackApiTests",
        "integrationtest.CallbackSignatureTest",
        "integrationtest.ClientConfigApiTests",
        "integrationtest.EventProcessingServerTests",
        "integrationtest.MultiClientCallbackTests",
        "integrationtest.PaymentObserverTests",
        "integrationtest.RpcPlatformApiTests",
        "integrationtest.StellarObserverTests",
        "integrationtest.WalletCallbackRecordTests",
      ),
  )

essentialSuites.forEach { (suite, classes) ->
  tasks.register<Test>("test$suite") {
    description = "Run the $suite essential tests against a running server."
    group = "verification"
    testClassesDirs = sourceSets["testFixtures"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform()
    systemProperty(
      "junit.jupiter.testclass.order.default",
      "org.junit.jupiter.api.ClassOrderer\$OrderAnnotation",
    )
    filter { classes.forEach { includeTestsMatching("org.stellar.anchor.platform.$it") } }
    testLogging {
      events("SKIPPED", "FAILED")
      showExceptions = true
      showStandardStreams = true
      exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    retry {
      maxRetries.set(2)
      maxFailures.set(20)
      failOnPassedAfterRetry.set(false)
    }
  }
}

val verifyEssentialSuites by
  tasks.registering {
    description = "Fail when an essential test class is in no suite, in several, or does not exist."
    group = "verification"
    val roots =
      listOf("integrationtest", "e2etest").map {
        file("src/testFixtures/kotlin/org/stellar/anchor/platform/$it")
      }
    inputs.files(roots)
    doLast {
      val testAnnotation = Regex("""@(Test|ParameterizedTest|RepeatedTest|TestFactory)\b""")
      val testClass = Regex("""^(?:open )?class (\w+)""", RegexOption.MULTILINE)
      val declared =
        roots
          .flatMap { root ->
            root
              .walk()
              .filter { it.extension == "kt" && testAnnotation.containsMatchIn(it.readText()) }
              .flatMap { f -> testClass.findAll(f.readText()).map { "${root.name}.${it.groupValues[1]}" } }
              .toList()
          }
          .toSet()
      val assigned = essentialSuites.values.flatten()
      val duplicated = assigned.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
      val missing = declared - assigned.toSet()
      val unknown = assigned.toSet() - declared
      if (duplicated.isNotEmpty() || missing.isNotEmpty() || unknown.isNotEmpty()) {
        throw GradleException(
          "Essential suites out of sync. In no suite: $missing. In several: $duplicated. Not found: $unknown."
        )
      }
    }
  }

tasks.check { dependsOn(verifyEssentialSuites) }
