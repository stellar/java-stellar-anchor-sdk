@Suppress(
  // The alias call in plugins scope produces IntelliJ false error which is suppressed here.
  "DSL_SCOPE_VIOLATION"
)
plugins {
  application
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.spring.dependency.management)
  alias(libs.plugins.kotlin.jvm)
}

dependencies {
  implementation(
    libs.snakeyaml
  ) // used to force the version of snakeyaml (used by springboot) to a safer one.
  implementation("org.springframework.boot:spring-boot-starter-web")

  implementation(libs.commons.cli)
  implementation(libs.docker.compose.rule)
  implementation(libs.dotenv)
  implementation(libs.google.gson)
  implementation(libs.kotlin.serialization.json)
  implementation(libs.okhttp3)
  implementation(libs.coroutines.core)

  // From projects
  implementation(project(":api-schema"))
  implementation(project(":lib-util"))
  implementation(project(":core"))
  implementation(project(":platform"))
  implementation(project(":kotlin-reference-server"))
  implementation(project(":wallet-reference-server"))
}

tasks {
  bootJar {
    archiveBaseName.set("anchor-platform-runner")
  }
}

application { mainClass.set("org.stellar.anchor.platform.ServiceRunner") }

/**
 * Start all the servers based on the `default` test configuration.
 */
tasks.register<JavaExec>("startAllServers") {
  println("Starting all servers based on the `default` test configuration.")
  group = "application"
  classpath = sourceSets["main"].runtimeClasspath
  mainClass.set("org.stellar.anchor.platform.run_profiles.RunAllServers")
}

tasks.register<JavaExec>("startServersWithTestProfile") {
  println("Starting the servers based on the test configuration specified by the TEST_PROFILE_NAME envionrment variable.")
  group = "application"
  classpath = sourceSets["main"].runtimeClasspath
  println("Test profile name: ${System.getenv("TEST_PROFILE_NAME")}")
  mainClass.set("org.stellar.anchor.platform.run_profiles.RunTestProfile")
}

/**
 * Run docker-compose up to start Postgres, Kafka, Zookeeper,etc.
 */
tasks.register<JavaExec>("dockerComposeUp") {
  println("Running docker-compose up to start Postgres, Kafka, Zookeeper,etc.")
  group = "application"
  classpath = sourceSets["main"].runtimeClasspath
  mainClass.set("org.stellar.anchor.platform.run_profiles.RunDockerDevStack")
}
