package org.stellar.anchor.platform.config

import kotlin.test.assertContains
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.validation.BindException
import org.springframework.validation.ValidationUtils
import org.stellar.anchor.config.event.KafkaConfig
import org.stellar.anchor.config.event.SqsConfig

open class EventConfigTest {
  @Test
  fun testDisabledEventConfig() {
    // events are disabled
    val eventConfig = PropertyEventConfig()
    eventConfig.isEnabled = false
    val errors = BindException(eventConfig, "eventConfig")
    ValidationUtils.invokeValidator(eventConfig, eventConfig, errors)
    assertEquals(0, errors.errorCount)
  }

  @Test
  fun testEnabledForKafka() {
    // Events correctly configured for kafka
    val eventConfig = PropertyEventConfig()
    val kafkaConfig = KafkaConfig()

    kafkaConfig.bootstrapServers = "localhost:29092"
    eventConfig.publisher = PropertyPublisherConfig()
    eventConfig.publisher.type = "kafka"
    eventConfig.publisher.kafka = kafkaConfig
    eventConfig.isEnabled = true

    val errors = BindException(eventConfig, "eventConfig")
    ValidationUtils.invokeValidator(eventConfig, eventConfig, errors)
    assertEquals(0, errors.errorCount)
  }

  @Test
  fun testEnabledForSqs() {
    // Events correctly configured for kafka
    val eventConfig = PropertyEventConfig()
    val sqsConfig = SqsConfig()

    sqsConfig.awsRegion = "us-east"
    eventConfig.publisher = PropertyPublisherConfig()
    eventConfig.publisher.type = "sqs"
    eventConfig.publisher.sqs = sqsConfig
    eventConfig.isEnabled = true

    val errors = BindException(eventConfig, "eventConfig")
    ValidationUtils.invokeValidator(eventConfig, eventConfig, errors)
    assertEquals(0, errors.errorCount)
  }

  @Test
  fun testKafkaMissingFields() {
    val eventConfig = PropertyEventConfig()
    val kafkaConfig = KafkaConfig()

    eventConfig.publisher = PropertyPublisherConfig()
    eventConfig.publisher.type = "kafka"
    eventConfig.publisher.kafka = kafkaConfig
    eventConfig.isEnabled = true

    val errors = BindException(eventConfig, "eventConfig")
    ValidationUtils.invokeValidator(eventConfig, eventConfig, errors)
    assertEquals(1, errors.errorCount)
    errors.message?.let { assertContains(it, "empty-bootstrapServer") }
  }

  @Test
  fun testSqsMissingFields() {
    val eventConfig = PropertyEventConfig()
    val sqsConfig = SqsConfig()

    eventConfig.publisher = PropertyPublisherConfig()
    eventConfig.publisher.type = "sqs"
    eventConfig.publisher.sqs = sqsConfig
    eventConfig.isEnabled = true

    val errors = BindException(eventConfig, "eventConfig")
    ValidationUtils.invokeValidator(eventConfig, eventConfig, errors)
    assertEquals(1, errors.errorCount)
    errors.message?.let { assertContains(it, "empty-aws-region") }
  }
}
