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
    config.eventTypeToQueue.put("quote_created", "ap_quote_created")
    config.eventTypeToQueue.put("transaction_created", "ap_transaction_created")
    config.eventTypeToQueue.put("transaction_status_changed", "ap_transaction_status_changed")
    config.eventTypeToQueue.put("transaction_error", "ap_transaction_error")

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

  companion object {
    @JvmStatic
    fun generatedKafkaConfig(): Stream<Arguments> {
      return Stream.of(
        Arguments.of(0, "no-error", KafkaConfig("localhost:29092", "client_id", 5, 10, 500)),
        Arguments.of(
          1,
          "kafka-retries-invalid",
          KafkaConfig("localhost:29092", "client_id", -1, 10, 500)
        ),
        Arguments.of(
          1,
          "kafka-linger-ms-invalid",
          KafkaConfig("localhost:29092", "client_id", 5, -10, 500)
        ),
        Arguments.of(
          1,
          "kafka-batch-size-invalid",
          KafkaConfig("localhost:29092", "client_id", 5, 10, -1)
        ),
        Arguments.of(1, "kafka-bootstrap-server-empty", KafkaConfig("", "client_id", 1, 10, 500)),
        Arguments.of(1, "kafka-bootstrap-server-empty", KafkaConfig(null, "client_id", 1, 10, 500))
      )
    }

    @JvmStatic
    fun generatedSqsConfig(): Stream<Arguments> {
      return Stream.of(
        Arguments.of(0, "no-error", SqsConfig(true, "us-east-1")),
        Arguments.of(0, "no-error", SqsConfig(false, "us-east-1")),
        Arguments.of(1, "sqs-aws-region-empty", SqsConfig(true, null)),
        Arguments.of(1, "sqs-aws-region-empty", SqsConfig(false, null))
      )
    }

    @JvmStatic
    fun generatedMskConfig(): Stream<Arguments> {
      return Stream.of(
        Arguments.of(0, "no-error", MskConfig(true, "localhost:29092", "client_id", 5, 10, 500)),
        Arguments.of(
          1,
          "msk-retries-invalid",
          MskConfig(true, "localhost:29092", "client_id", -1, 10, 500)
        ),
        Arguments.of(
          1,
          "msk-linger-ms-invalid",
          MskConfig(true, "localhost:29092", "client_id", 5, -10, 500)
        ),
        Arguments.of(
          1,
          "msk-batch-size-invalid",
          MskConfig(true, "localhost:29092", "client_id", 5, 10, -1)
        ),
        Arguments.of(1, "msk-bootstrap-server-empty", MskConfig(true, "", "client_id", 1, 10, 500)),
        Arguments.of(
          1,
          "msk-bootstrap-server-empty",
          MskConfig(true, null, "client_id", 1, 10, 500)
        )
      )
    }
  }
}
