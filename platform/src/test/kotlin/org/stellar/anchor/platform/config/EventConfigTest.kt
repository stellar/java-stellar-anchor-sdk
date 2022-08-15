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
    val eventConfig = PropertyEventConfig(null, null)
    eventConfig.isEnabled = false
    val errors = BindException(eventConfig, "eventConfig")
    ValidationUtils.invokeValidator(eventConfig, eventConfig, errors)
    assertEquals(0, errors.errorCount)
  }

  @Test
  fun testEnabledForKafka() {
    // Events correctly configured for kafka
    val kafkaConfig = PropertyKafkaConfig()
    kafkaConfig.isUseSingleQueue = true
    kafkaConfig.bootstrapServer = "localhost:29092"
    kafkaConfig.eventTypeToQueue = HashMap<String, String>()

    val eventConfig = PropertyEventConfig(kafkaConfig, PropertySqsConfig())
    eventConfig.isEnabled = true
    eventConfig.publisherType = "kafka"
    val errors = BindException(eventConfig, "eventConfig")
    ValidationUtils.invokeValidator(eventConfig, eventConfig, errors)
    assertEquals(0, errors.errorCount)
  }

  @Test
  fun testEnabledForSqs() {
    // Events correctly configured for sqs
    val sqsConfig = PropertySqsConfig()
    sqsConfig.accessKey = "accessKey"
    sqsConfig.secretKey = "secretKey"
    sqsConfig.isUseSingleQueue = true
    sqsConfig.region = "region"
    sqsConfig.eventTypeToQueue = HashMap<String, String>()

    val eventConfig = PropertyEventConfig(PropertyKafkaConfig(), sqsConfig)
    eventConfig.isEnabled = true
    eventConfig.publisherType = "sqs"
    val errors = BindException(eventConfig, "eventConfig")
    ValidationUtils.invokeValidator(eventConfig, eventConfig, errors)
    assertEquals(0, errors.errorCount)
  }

  @Test
  fun testKafkaMissingFields() {
    val kafkaConfig = PropertyKafkaConfig()
    kafkaConfig.isUseSingleQueue = true
    kafkaConfig.eventTypeToQueue = HashMap<String, String>()

    val eventConfig = PropertyEventConfig(kafkaConfig, PropertySqsConfig())
    eventConfig.isEnabled = true
    eventConfig.publisherType = "kafka"
    val errors = BindException(eventConfig, "eventConfig")

    ValidationUtils.invokeValidator(eventConfig, eventConfig, errors)
    assertEquals(1, errors.errorCount)
    errors.message?.let { assertContains(it, "badConfig-kafka") }

    val kafkaErrors = kafkaConfig.validate()
    assertEquals(1, kafkaErrors.errorCount)
    kafkaErrors.message?.let { assertContains(it, "empty-bootstrapServer") }
  }

  @Test
  fun testSqsMissingFields() {
    val sqsConfig = PropertySqsConfig()
    sqsConfig.isUseSingleQueue = true
    sqsConfig.eventTypeToQueue = HashMap<String, String>()
    sqsConfig.accessKey = "accessKey"
    sqsConfig.secretKey = "secretKey"

    val eventConfig = PropertyEventConfig(PropertyKafkaConfig(), sqsConfig)
    eventConfig.isEnabled = true
    eventConfig.publisherType = "sqs"
    val errors = BindException(eventConfig, "eventConfig")

    ValidationUtils.invokeValidator(eventConfig, eventConfig, errors)
    assertEquals(1, errors.errorCount)
    errors.message?.let { assertContains(it, "badConfig-sqs") }

    val sqsErrors = sqsConfig.validate()
    assertEquals(1, sqsErrors.errorCount)
    sqsErrors.message?.let { assertContains(it, "empty-region") }
  }
}
