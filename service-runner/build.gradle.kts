import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
import com.bmuschko.gradle.docker.tasks.container.DockerLogsContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
import com.bmuschko.gradle.docker.tasks.image.DockerPullImage

@Suppress(
  // The alias call in plugins scope produces IntelliJ false error which is suppressed here.
  "DSL_SCOPE_VIOLATION"
)
plugins {
  application
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.spring.dependency.management)
  alias(libs.plugins.kotlin.jvm)
  id("com.bmuschko.docker-remote-api") version "9.3.7"
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

tasks { bootJar { archiveBaseName.set("anchor-platform-runner") } }

application { mainClass.set("org.stellar.anchor.platform.ServiceRunner") }

/** Start all the servers based on the `default` test configuration. */
tasks.register<JavaExec>("startAllServers") {
  println("Starting all servers based on the `default` test configuration.")
  group = "application"
  classpath = sourceSets["main"].runtimeClasspath
  mainClass.set("org.stellar.anchor.platform.run_profiles.RunAllServers")
}

/**
 * Start all the servers based on the test configuration specified by the TEST_PROFILE_NAME
 * envionrment variable.
 */
tasks.register<JavaExec>("startServersWithTestProfile") {
  println(
    "Starting the servers based on the test configuration specified by the TEST_PROFILE_NAME envionrment variable."
  )
  group = "application"
  classpath = sourceSets["main"].runtimeClasspath
  mainClass.set("org.stellar.anchor.platform.run_profiles.RunTestProfile")
}

/** Run docker-compose up to start Postgres, Kafka, Zookeeper,etc. */
tasks.register<JavaExec>("dockerComposeUp") {
  println("Running docker-compose up to start Postgres, Kafka, Zookeeper,etc.")
  group = "application"
  classpath = sourceSets["main"].runtimeClasspath
  mainClass.set("org.stellar.anchor.platform.run_profiles.RunDockerDevStack")
}

val pullImage by tasks.creating(DockerPullImage::class) { image.set("stellar/anchor-tests:v0.6.9") }

val createContainer by
  tasks.creating(DockerCreateContainer::class) {
    dependsOn(pullImage)
    targetImageId { pullImage.image.get() }
    val homeDomain = System.getenv("HOME_DOMAIN") ?: "http://host.docker.internal:8080"
    println("HOME_DOMAIN=$homeDomain")
    println("PROJECT_DIR=${project.projectDir}")
    val configPath = "${project.projectDir}/../platform/src/test/resources"
    //      hostConfig.autoRemove.set(true)
    hostConfig.binds.set(mutableMapOf(configPath to "/config"))
    val logConfig = DockerCreateContainer.HostConfig.LogConfig()
    hostConfig.logConfig("json-file", mapOf("max-size" to "100m"))
    hostConfig.network.set("host")
    cmd.set(
      listOf(
        "--home-domain",
        "http://host.docker.internal:8080",
        "--seps",
        "1",
        "10",
        "--sep-config",
        "//config/stellar-anchor-tests-sep-config.json",
        "--verbose"
      )
    )
  }

val printLogs by
  tasks.registering(DockerLogsContainer::class) {
    dependsOn(startContainer)
    targetContainerId(createContainer.containerId)
    follow.set(true)
    tailAll.set(true)
  }

val startContainer by
  tasks.creating(DockerStartContainer::class) {
    dependsOn(createContainer)
    targetContainerId(createContainer.containerId)
  }
