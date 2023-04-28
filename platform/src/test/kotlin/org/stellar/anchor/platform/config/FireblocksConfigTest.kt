package org.stellar.anchor.platform.config

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.validation.BindException
import org.springframework.validation.Errors
import org.stellar.anchor.util.FileUtil

class FireblocksConfigTest {

  private lateinit var config: FireblocksConfig
  private lateinit var secretConfig: PropertySecretConfig
  private lateinit var errors: Errors

  @BeforeEach
  fun setUp() {
    secretConfig = mockk()
    every { secretConfig.fireblocksApiKey } returns "testApiKey"
    every { secretConfig.fireblocksSecretKey } returns "testSecretKey"
    config = FireblocksConfig(secretConfig)
    config.baseUrl = "https://test.com"
    config.vaultAccountId = "testAccountId"
    config.transactionsReconciliationCron = "* * * * * *"
    config.publicKey = FileUtil.getResourceFileAsString("custody/public_key.txt")
    errors = BindException(config, "config")
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = [""])
  fun `test empty host_url`(url: String?) {
    config.baseUrl = url
    config.validate(config, errors)
    assertErrorCode(errors, "custody-fireblocks-base-url-empty")
  }

  @ParameterizedTest
  @ValueSource(strings = ["https://fireblocks.com", "https://fireblocks.org:8080"])
  fun `test valid host_url`(url: String) {
    config.baseUrl = url
    config.validate(config, errors)
    assertFalse(errors.hasErrors())
  }

  @ParameterizedTest
  @ValueSource(strings = ["https ://fireblocks.com", "fireblocks.com", "abc"])
  fun `test invalid host_url`(url: String) {
    config.baseUrl = url
    config.validate(config, errors)
    assertErrorCode(errors, "custody-fireblocks-base-url-invalid")
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = [""])
  fun `test empty vault_account_id`(url: String?) {
    config.vaultAccountId = url
    config.validate(config, errors)
    assertErrorCode(errors, "custody-fireblocks-vault-account-id-empty")
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = [""])
  fun `test empty api_key`(apiKey: String?) {
    every { secretConfig.fireblocksApiKey } returns apiKey
    config.validate(config, errors)
    assertErrorCode(errors, "secret-custody-fireblocks-api-key-empty")
  }

  @Test
  fun `test valid api_key`() {
    config.validate(config, errors)
    assertFalse(errors.hasErrors())
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = [""])
  fun `test empty secret_key`(secretKey: String?) {
    every { secretConfig.fireblocksSecretKey } returns secretKey
    config.validate(config, errors)
    assertErrorCode(errors, "secret-custody-fireblocks-secret-key-empty")
  }

  @Test
  fun `test valid secret_key`() {
    config.validate(config, errors)
    assertFalse(errors.hasErrors())
  }

  @ParameterizedTest
  @ValueSource(strings = ["* * * * * *", "0 0/15 * * * *"])
  fun `test valid transactions_reconciliation_cron`(cron: String) {
    config.transactionsReconciliationCron = cron
    config.validate(config, errors)
    assertFalse(errors.hasErrors())
  }

  @ParameterizedTest
  @ValueSource(strings = [""])
  fun `test empty transactions_reconciliation_cron`(cron: String) {
    config.transactionsReconciliationCron = cron
    config.validate(config, errors)
    assertErrorCode(errors, "custody-fireblocks-transactions-reconciliation-cron-empty")
  }

  @ParameterizedTest
  @ValueSource(strings = ["* * * * *", "* * * * * * *", "0/a * * * * *"])
  fun `test invalid transactions_reconciliation_cron`(cron: String) {
    config.transactionsReconciliationCron = cron
    config.validate(config, errors)
    assertErrorCode(errors, "custody-fireblocks-transactions-reconciliation-cron-invalid")
  }

  @Test
  fun `test valid public_key`() {
    config.validate(config, errors)
    assertFalse(errors.hasErrors())
  }

  @ParameterizedTest
  @ValueSource(strings = [""])
  fun `test empty public_key`(publicKey: String) {
    config.publicKey = publicKey
    config.validate(config, errors)
    assertErrorCode(errors, "custody-fireblocks-public_key-empty")
  }

  @ParameterizedTest
  @ValueSource(
    strings =
      ["test_certificate", "-----BEGIN PUBLIC KEY----- test_certificate -----END PUBLIC KEY-----"]
  )
  fun `test invalid public_key`(publicKey: String) {
    config.publicKey = publicKey
    config.validate(config, errors)
    assertErrorCode(errors, "custody-fireblocks-public_key-invalid")
  }
}
