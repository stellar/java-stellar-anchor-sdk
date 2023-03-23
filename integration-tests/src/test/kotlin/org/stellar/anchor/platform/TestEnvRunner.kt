package org.stellar.anchor.platform

import com.palantir.docker.compose.DockerComposeExtension
import com.palantir.docker.compose.connection.waiting.HealthChecks
import java.io.File
import kotlinx.coroutines.*
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext

class TestEnvRunner(val config: TestConfig) {
  private val docker: DockerComposeExtension
  private var runningServers: MutableList<ConfigurableApplicationContext> = mutableListOf()
  private var shouldStartDockerCompose: Boolean = false
  private var shouldStartServers: Boolean = false

  init {
    val dockerComposeFolder = File(config.testEnvFile).parent
    docker =
      DockerComposeExtension.builder()
        .saveLogsTo("build/docker-logs/anchor-platform-integration-test")
        .file("${dockerComposeFolder}/docker-compose.yaml")
        .waitingForService("kafka", HealthChecks.toHaveAllPortsOpen())
        .waitingForService("db", HealthChecks.toHaveAllPortsOpen())
        .pullOnStartup(true)
        .build()
  }

  fun start(wait: Boolean = false, preStart: (config: TestConfig) -> Unit = {}) {
    preStart(this.config)

    shouldStartDockerCompose = config.env["run_docker"].toBoolean()
    shouldStartServers = config.env["run_servers"].toBoolean()

    if (shouldStartDockerCompose) startDocker()
    if (shouldStartServers) startServers(wait)
  }

  fun shutdown() {
    if (shouldStartServers) shutdownServers()
    if (shouldStartDockerCompose) shutdownDocker()
  }

  private fun startServers(wait: Boolean): MutableList<ConfigurableApplicationContext> {
    runBlocking {
      println("Running tests...")
      val envMap = config.env

//      envMap["data.type"] = "h2"
//      envMap["events.enabled"] = "false"
      envMap["assets.value"] = getResourceFilePath(envMap["assets.value"]!!)
      envMap["sep1.toml.value"] = getResourceFilePath(envMap["sep1.toml.value"]!!)

      // Start servers
      val jobs = mutableListOf<Job>()
      val scope = CoroutineScope(Dispatchers.Default)
      jobs += scope.launch { ServiceRunner.startKotlinReferenceServer(false) }
      jobs += scope.launch { runningServers.add(ServiceRunner.startAnchorReferenceServer()) }
      jobs += scope.launch { runningServers.add(ServiceRunner.startStellarObserver(envMap)) }
      jobs += scope.launch { runningServers.add(ServiceRunner.startSepServer(envMap)) }
      jobs.forEach { it.join() }

      if (wait) {
        while (true) {
          delay(60000)
        }
      }
    }
    return runningServers
  }

  private fun startDocker() {
    docker.beforeAll(null)
  }
  private fun shutdownServers() {
    runningServers.forEach { SpringApplication.exit(it) }
    org.stellar.reference.stop()
  }

  private fun shutdownDocker() {
    docker.afterAll(null)
  }
}
