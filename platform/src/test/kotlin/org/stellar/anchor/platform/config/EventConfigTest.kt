package org.stellar.anchor.platform.config

import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.validation.BindException
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils

class EventConfigTest {
  lateinit var config: PropertyEventConfig
  lateinit var errors: Errors

  @BeforeEach
  fun setUp() {
    config = PropertyEventConfig()
    errors = BindException(config, "config")
  }

  @Test
  fun `test enabled flag`() {
    config.isEnabled = false
    ValidationUtils.invokeValidator(config, config, errors)
    assertEquals(0, errors.errorCount)
  }

  @ParameterizedTest
  @MethodSource("generatedKafkaConfig")
  fun `test Kafka configurations`(errorCount: Int, errorCode: String, kafkaConfig: KafkaConfig) {
    config.isEnabled = true
    config.publisher = PropertyPublisherConfig()
    config.publisher.type = "kafka"
    config.publisher.kafka = kafkaConfig
    config.validateKafka(config, errors)
    assertEquals(errorCount, errors.errorCount)
    if (errorCount > 0) {
      assertEquals(errorCode, errors.allErrors[0].code)
    }
  }

  @ParameterizedTest
  @MethodSource("generatedSqsConfig")
  fun `test Sqs configurations`(errorCount: Int, errorCode: String, sqsConfig: SqsConfig) {
    config.isEnabled = true
    config.publisher = PropertyPublisherConfig()
    config.publisher.type = "sqs"
    config.publisher.sqs = sqsConfig
    config.validateSqs(config, errors)
    assertEquals(errorCount, errors.errorCount)
    if (errorCount > 0) {
      assertEquals(errorCode, errors.allErrors[0].code)
    }
  }

  @ParameterizedTest
  @MethodSource("generatedMskConfig")
  fun `test Msk configurations`(errorCount: Int, errorCode: String, mskConfig: MskConfig) {
    config.isEnabled = true
    config.publisher = PropertyPublisherConfig()
    config.publisher.type = "msk"
    config.publisher.msk = mskConfig
    config.validateMsk(config, errors)
    assertEquals(errorCount, errors.errorCount)
    if (errorCount > 0) {
      assertEquals(errorCode, errors.allErrors[0].code)
    }
  }
  //  @Test
  //  fun testKafkaMissingFields() {
  //    val eventConfig = PropertyEventConfig()
  //    val kafkaConfig = KafkaConfig()
  //
  //    eventConfig.publisher = PropertyPublisherConfig()
  //    eventConfig.publisher.type = "kafka"
  //    eventConfig.publisher.kafka = kafkaConfig
  //    eventConfig.isEnabled = true
  //
  //    val errors = BindException(eventConfig, "eventConfig")
  //    ValidationUtils.invokeValidator(eventConfig, eventConfig, errors)
  //    assertEquals(1, errors.errorCount)
  //    errors.message?.let { assertContains(it, "empty-bootstrapServer") }
  //  }
  //
  //  @Test
  //  fun testSqsMissingFields() {
  //    val eventConfig = PropertyEventConfig()
  //    val sqsConfig = SqsConfig()
  //
  //    eventConfig.publisher = PropertyPublisherConfig()
  //    eventConfig.publisher.type = "sqs"
  //    eventConfig.publisher.sqs = sqsConfig
  //    eventConfig.isEnabled = true
  //
  //    val errors = BindException(eventConfig, "eventConfig")
  //    ValidationUtils.invokeValidator(eventConfig, eventConfig, errors)
  //    assertEquals(1, errors.errorCount)
  //    errors.message?.let { assertContains(it, "empty-aws-region") }
  //  }

  companion object {
    @JvmStatic
    fun generatedKafkaConfig(): Stream<Arguments> {
      return Stream.of(
        Arguments.of(0, "no-error", KafkaConfig("localhost:29092", "client_id", 5, 10, 500)),
        Arguments.of(
          1,
          "bad-kafka-retries",
          KafkaConfig("localhost:29092", "client_id", -1, 10, 500)
        ),
        Arguments.of(
          1,
          "bad-kafka-linger-ms",
          KafkaConfig("localhost:29092", "client_id", 5, -10, 500)
        ),
        Arguments.of(
          1,
          "bad-kafka-batch-size",
          KafkaConfig("localhost:29092", "client_id", 5, 10, -1)
        ),
        Arguments.of(1, "empty-kafka-bootstrap-server", KafkaConfig("", "client_id", 1, 10, 500)),
        Arguments.of(1, "empty-kafka-bootstrap-server", KafkaConfig(null, "client_id", 1, 10, 500))
      )
    }

    @JvmStatic
    fun generatedSqsConfig(): Stream<Arguments> {
      return Stream.of(
        Arguments.of(0, "no-error", SqsConfig(true, "us-east-1")),
        Arguments.of(0, "no-error", SqsConfig(false, "us-east-1")),
        Arguments.of(1, "empty-sqs-aws-region", SqsConfig(true, null)),
        Arguments.of(1, "empty-sqs-aws-region", SqsConfig(false, null))
      )
    }

    @JvmStatic
    fun generatedMskConfig(): Stream<Arguments> {
      return Stream.of(
        Arguments.of(0, "no-error", MskConfig(true, "localhost:29092", "client_id", 5, 10, 500)),
        Arguments.of(
          1,
          "bad-msk-retries",
          MskConfig(true, "localhost:29092", "client_id", -1, 10, 500)
        ),
        Arguments.of(
          1,
          "bad-msk-linger-ms",
          MskConfig(true, "localhost:29092", "client_id", 5, -10, 500)
        ),
        Arguments.of(
          1,
          "bad-msk-batch-size",
          MskConfig(true, "localhost:29092", "client_id", 5, 10, -1)
        ),
        Arguments.of(1, "empty-msk-bootstrap-server", MskConfig(true, "", "client_id", 1, 10, 500)),
        Arguments.of(
          1,
          "empty-msk-bootstrap-server",
          MskConfig(true, null, "client_id", 1, 10, 500)
        )
      )
    }
  }
}
