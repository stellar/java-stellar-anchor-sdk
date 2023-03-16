package org.stellar.anchor.platform.e2e

import com.palantir.docker.compose.DockerComposeExtension
import com.palantir.docker.compose.connection.DockerMachine
import com.palantir.docker.compose.connection.waiting.HealthChecks
import java.io.File
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class Sep24EndToEndTest {
  companion object {
    private val dockerMachine = DockerMachine.localMachine().build()
    @JvmStatic
    @RegisterExtension
    val docker: DockerComposeExtension =
      DockerComposeExtension.builder()
        //        .saveLogsTo("c:/tmp/docker-compose-logs")
        //        .saveLogsTo("build/dockerLogs/dockerComposeRuleTest")
        .machine(dockerMachine)
        //        .dockerCompose(MyDockerCompose())
        .logCollector(MyFileLogCollector(File("build/dockerLogs/dockerComposeRuleTest")))
        .file(
          "C:/projects/java-stellar-anchor-sdk/docs/resources/docker-examples/kafka/docker-compose.yaml"
        )
        //        .file("src/test/resources/docker-compose/sep24/docker-compose.yaml")
        .waitingForService("kafka", HealthChecks.toHaveAllPortsOpen())
        .pullOnStartup(true)
        .build()
  }

  init {
    println(docker)
  }

  @Test
  fun testHelloWorld() {
    val containers = docker.containers()
    println("$containers")
  }
  @Test
  fun testHelloWorld2() {
    val containers = docker.containers()
    println("$containers")
  }
}
