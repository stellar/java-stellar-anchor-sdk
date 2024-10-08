package org.stellar.anchor.platform.integrationtest

import io.mockk.every
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.stream.Stream
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.core.io.ClassPathResource
import org.stellar.anchor.LockAndMockStatic
import org.stellar.anchor.LockAndMockTest
import org.stellar.anchor.event.EventService.EventQueue.TEST
import org.stellar.anchor.platform.config.KafkaConfig
import org.stellar.anchor.platform.config.KafkaConfig.SaslMechanism.PLAIN
import org.stellar.anchor.platform.config.KafkaConfig.SecurityProtocol.*
import org.stellar.anchor.platform.config.PropertySecretConfig.*
import org.stellar.anchor.platform.configurator.SecretManager
import org.stellar.anchor.platform.event.KafkaSession

@ExtendWith(LockAndMockTest::class)
class KafkaTests {
  companion object {
    @JvmStatic
    fun kafkaTestDataProvider(): Stream<Arguments> {
      return Stream.of(
        Arguments.of("kafka:29092", PLAINTEXT),
        Arguments.of("kafka:29093", SSL),
        Arguments.of("kafka:29094", SASL_SSL),
        Arguments.of("kafka:29095", SASL_PLAINTEXT)
      )
    }

    val keyStoreFile: File =
      copyResourceToFile("common/secrets/kafka.keystore.jks", "kafka.keystore")
    val trustStoreFile: File =
      copyResourceToFile("common/secrets/kafka.truststore.jks", "kafka.truststore")

    private fun copyResourceToFile(resourcePath: String, prefix: String): File {
      val resource = ClassPathResource(resourcePath)
      val tempFile: Path = Files.createTempFile(prefix, ".jks")
      Files.copy(resource.inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING)
      tempFile.toFile().deleteOnExit()
      return tempFile.toFile()
    }
  }

  @ParameterizedTest
  @LockAndMockStatic([SecretManager::class])
  @MethodSource("kafkaTestDataProvider")
  fun `test Kafka session with PLAINTEXT mode`(
    server: String,
    protocol: KafkaConfig.SecurityProtocol
  ) {
    val config =
      KafkaConfig().also {
        it.bootstrapServer = server
        it.retries = 0
        it.lingerMs = 10
        it.batchSize = 1
        it.pollTimeoutSeconds = 1
        it.securityProtocol = protocol
        it.saslMechanism = PLAIN
        it.sslTruststoreLocation = trustStoreFile.absolutePath
        it.sslKeystoreLocation = keyStoreFile.absolutePath
      }

    every { SecretManager.secret(SECRET_EVENTS_QUEUE_KAFKA_USERNAME) } returns "admin"
    every { SecretManager.secret(SECRET_EVENTS_QUEUE_KAFKA_PASSWORD) } returns "admin-secret"
    every { SecretManager.secret(SECRET_SSL_KEY_PASSWORD) } returns "test123"
    every { SecretManager.secret(SECRET_SSL_KEYSTORE_PASSWORD) } returns "test123"
    every { SecretManager.secret(SECRET_SSL_TRUSTSTORE_PASSWORD) } returns "test123"

    KafkaSession(config, "testKafkaSession", TEST).testConnection()
  }
}
