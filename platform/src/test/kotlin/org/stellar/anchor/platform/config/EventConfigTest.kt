package org.stellar.anchor.platform.config

import kotlin.test.assertContains
import kotlin.test.assertEquals
import org.junit.jupiter.api.*
import org.springframework.validation.BindException
import org.springframework.validation.ValidationUtils

open class EventConfigTest {
  @Test
  fun testDisabledEventConfig() {
    // events are disabled
    val eventConfig = PropertyEventConfig(null)
    eventConfig.isEnabled = false
    val errors = BindException(eventConfig, "eventConfig")
    ValidationUtils.invokeValidator(eventConfig, eventConfig, errors)
    assertEquals(0, errors.errorCount)
  }

  @Test
  fun testEnabledForKafka() {
    // Events correctly configured for kafka
    val kafkaConfig = PropertyPublisherConfig(
      PropertyEventTypeToQueueConfig()
    )
    kafkaConfig.isUseSingleQueue = true
    kafkaConfig.bootstrapServer = "localhost:29092"
    kafkaConfig.eventTypeToQueue = HashMap<String, String>()

    val eventConfig = PropertyEventConfig(kafkaConfig)
    eventConfig.isEnabled = true
    eventConfig.publisherType = "kafka"
    val errors = BindException(eventConfig, "eventConfig")
    ValidationUtils.invokeValidator(eventConfig, eventConfig, errors)
    assertEquals(0, errors.errorCount)
  }

  @Test
  fun testEnabledForSqs() {
    // Events correctly configured for sqs
    val sqsConfig = PropertyPublisherConfig(PropertyEventTypeToQueueConfig())
    sqsConfig.accessKey = "accessKey"
    sqsConfig.secretKey = "secretKey"
    sqsConfig.isUseSingleQueue = true
    sqsConfig.region = "region"
    sqsConfig.eventTypeToQueue = HashMap<String, String>()

    val eventConfig = PropertyEventConfig(sqsConfig)
    eventConfig.isEnabled = true
    eventConfig.publisherType = "sqs"
    val errors = BindException(eventConfig, "eventConfig")
    ValidationUtils.invokeValidator(eventConfig, eventConfig, errors)
    assertEquals(0, errors.errorCount)
  }

  @Test
  fun testKafkaMissingFields() {
    val kafkaConfig = PropertyPublisherConfig(
      PropertyEventTypeToQueueConfig()
    )
    kafkaConfig.isUseSingleQueue = true
    kafkaConfig.eventTypeToQueue = HashMap<String, String>()

    val eventConfig = PropertyEventConfig(kafkaConfig)
    eventConfig.isEnabled = true
    eventConfig.publisherType = "kafka"
    val errors = BindException(eventConfig, "eventConfig")

    ValidationUtils.invokeValidator(eventConfig, eventConfig, errors)
    assertEquals(1, errors.errorCount)
    errors.message?.let { assertContains(it, "badPublisherConfig") }

    val kafkaErrors = kafkaConfig.validate(eventConfig.publisherType)
    assertEquals(1, kafkaErrors.errorCount)
    kafkaErrors.message?.let { assertContains(it, "empty-bootstrapServer") }
  }

  @Test
  fun testSqsMissingFields() {
    val sqsConfig = PropertyPublisherConfig(PropertyEventTypeToQueueConfig())
    sqsConfig.isUseSingleQueue = true
    sqsConfig.eventTypeToQueue = HashMap<String, String>()
    sqsConfig.accessKey = "accessKey"
    sqsConfig.secretKey = "secretKey"

    val eventConfig = PropertyEventConfig(sqsConfig)
    eventConfig.isEnabled = true
    eventConfig.publisherType = "sqs"
    val errors = BindException(eventConfig, "eventConfig")

    ValidationUtils.invokeValidator(eventConfig, eventConfig, errors)
    assertEquals(1, errors.errorCount)
    errors.message?.let { assertContains(it, "badPublisherConfig") }

    val sqsErrors = sqsConfig.validate(eventConfig.publisherType)
    assertEquals(1, sqsErrors.errorCount)
    sqsErrors.message?.let { assertContains(it, "empty-region") }
  }
}
