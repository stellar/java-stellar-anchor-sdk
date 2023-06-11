@file:JvmName("TestProfileRunner")

package org.stellar.anchor.platform

import com.palantir.docker.compose.DockerComposeExtension
import com.palantir.docker.compose.configuration.ProjectName
import com.palantir.docker.compose.connection.waiting.HealthChecks
import java.io.File
import java.lang.Thread.sleep
import java.lang.reflect.Field
import java.util.*
import kotlinx.coroutines.*
import org.springframework.context.ConfigurableApplicationContext
import org.stellar.anchor.util.Log.info

lateinit var testProfileExecutor: TestProfileExecutor

fun main() = runBlocking {
  info("Starting TestPfofileExecutor...")
  testProfileExecutor = TestProfileExecutor(TestConfig(profileName = "default"))

  launch {
    Runtime.getRuntime()
      .addShutdownHook(
        object : Thread() {
          override fun run() {
            testProfileExecutor.shutdown()
          }
        }
      )
  }

  testProfileExecutor.start(true)
}

class TestProfileExecutor(val config: TestConfig) {
  private lateinit var docker: DockerComposeExtension
  private var runningServers: MutableList<ConfigurableApplicationContext> = mutableListOf()
  private var shouldStartDockerCompose: Boolean = false
  private var shouldStartAllServers: Boolean = false
  private var shouldStartSepServer: Boolean = false
  private var shouldStartPlatformServer: Boolean = false
  private var shouldStartReferenceServer: Boolean = false
  private var shouldStartObserver: Boolean = false
  private var shouldStartKotlinReferenceServer: Boolean = false

  fun start(wait: Boolean = false, preStart: (config: TestConfig) -> Unit = {}) {
    info("Starting TestProfileExecutor...")

    preStart(this.config)

    shouldStartDockerCompose = config.env["run_docker"].toBoolean()
    shouldStartAllServers = config.env["run_all_servers"].toBoolean()
    shouldStartSepServer = config.env["run_sep_server"].toBoolean()
    shouldStartPlatformServer = config.env["run_platform_server"].toBoolean()
    shouldStartReferenceServer = config.env["run_reference_server"].toBoolean()
    shouldStartObserver = config.env["run_observer"].toBoolean()
    shouldStartKotlinReferenceServer = config.env["run_kotlin_reference_server"].toBoolean()

    startDocker()
    // TODO: Check server readiness instead of wait for 5 seconds
    sleep(5000)
    startServers(wait)
  }

  fun shutdown() {
    shutdownServers()
    if (shouldStartDockerCompose) shutdownDocker()
  }

  private fun startServers(wait: Boolean): MutableList<ConfigurableApplicationContext> {
    runBlocking {
      val envMap = config.env

      envMap["assets.value"] = getResourceFile(envMap["assets.value"]!!).absolutePath
      envMap["sep1.toml.value"] = getResourceFile(envMap["sep1.toml.value"]!!).absolutePath

      // Start servers
      val jobs = mutableListOf<Job>()
      val scope = CoroutineScope(Dispatchers.Default)

      if (shouldStartAllServers || shouldStartKotlinReferenceServer) {
        info("Starting Kotlin reference server...")
        jobs += scope.launch { ServiceRunner.startKotlinReferenceServer(envMap, false) }
      }
      if (shouldStartAllServers || shouldStartReferenceServer) {
        info("Starting Java reference server...")
        jobs +=
          scope.launch { runningServers.add(ServiceRunner.startAnchorReferenceServer(envMap)) }
      }
      if (shouldStartAllServers || shouldStartObserver) {
        info("Starting observer...")
        jobs += scope.launch { runningServers.add(ServiceRunner.startStellarObserver(envMap)) }
      }
      if (shouldStartAllServers || shouldStartSepServer) {
        info("Starting SEP server...")
        jobs += scope.launch { runningServers.add(ServiceRunner.startSepServer(envMap)) }
      }
      if (shouldStartAllServers || shouldStartPlatformServer) {
        info("Starting platform server...")
        jobs += scope.launch { runningServers.add(ServiceRunner.startPlatformServer(envMap)) }
      }

      if (jobs.size > 0) {
        jobs.forEach { it.join() }

        if (wait)
          do {
            val anyActive = runningServers.any { it.isActive || it.isRunning }
            delay(5000)
          } while (anyActive)
      }
    }

    return runningServers
  }

  private fun startDocker() {
    if (shouldStartDockerCompose) {
      info("Starting docker compose...")
      if (isWindows()) {
        setupWindowsEnv()
      }

      info("Initializing TestProfileRunner...")
      val dockerComposeFile = getResourceFile("docker-compose-test.yaml")
      val userHomeFolder = File(System.getProperty("user.home"))
      docker =
        DockerComposeExtension.builder()
          .saveLogsTo("${userHomeFolder}/docker-logs/anchor-platform-integration-test")
          .file(dockerComposeFile.absolutePath)
          .waitingForService("kafka", HealthChecks.toHaveAllPortsOpen())
          .waitingForService("db", HealthChecks.toHaveAllPortsOpen())
          .pullOnStartup(true)
          .projectName(
            ProjectName.fromString("anchorplatform${UUID.randomUUID().toString().takeLast(6)}")
          )
          .build()

      docker.beforeAll(null)
    }
  }

  private fun shutdownServers() {
    runningServers.forEach {
      it.close()
      it.stop()
    }
    org.stellar.reference.stop()
  }

  private fun shutdownDocker() {
    docker.afterAll(null)
  }

  private fun isWindows(): Boolean {
    return System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win")
  }

  private fun setupWindowsEnv() {
    val windowsDockerLocation =
      System.getenv("WIN_DOCKER_LOCATION")
        ?: throw RuntimeException("WIN_DOCKER_LOCATION env variable is not set")

    setEnv(mapOf("DOCKER_LOCATION" to File(windowsDockerLocation, "docker.exe").absolutePath))
    setEnv(
      mapOf(
        "DOCKER_COMPOSE_LOCATION" to File(windowsDockerLocation, "docker-compose.exe").absolutePath
      )
    )
  }

  @Suppress("UNCHECKED_CAST")
  private fun setEnv(envs: Map<String, String>?) {
    try {
      val processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment")
      val theEnvironmentField: Field = processEnvironmentClass.getDeclaredField("theEnvironment")
      theEnvironmentField.isAccessible = true
      val env = theEnvironmentField.get(null) as MutableMap<String, String>
      env.putAll(envs!!)
      val theCaseInsensitiveEnvironmentField: Field =
        processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment")
      theCaseInsensitiveEnvironmentField.isAccessible = true
      val cienv = theCaseInsensitiveEnvironmentField.get(null) as MutableMap<String, String>
      cienv.putAll(envs)
    } catch (e: NoSuchFieldException) {
      val classes = Collections::class.java.declaredClasses
      val env = System.getenv()
      for (cl in classes) {
        if ("java.util.Collections\$UnmodifiableMap" == cl.name) {
          val field: Field = cl.getDeclaredField("m")
          field.isAccessible = true
          val obj: Any = field.get(env)
          val map = obj as MutableMap<String, String>
          map.clear()
          map.putAll(envs!!)
        }
      }
    }
  }
}
