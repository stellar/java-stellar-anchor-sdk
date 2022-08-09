package org.stellar.anchor.platform.payment.observer.stellar

import io.mockk.*
import io.mockk.impl.annotations.MockK
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import java.time.temporal.ChronoUnit.HOURS
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.platform.data.PaymentObservingAccount

class PaymentObservingAccountsManagerTest {
  @MockK private lateinit var paymentObservingAccountStore: PaymentObservingAccountStore

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxed = true)
    every { paymentObservingAccountStore.list() } returns
      listOf(
        PaymentObservingAccount(
          "GCK5ECMM67ZN7RWGUSDKQAW6CAF6Q5WYK2VQJ276SJMINU6WVCIQW6BL",
          Instant.now().minus(0, HOURS)
        ),
        PaymentObservingAccount(
          "GCIWQDKACLW26UJXY5CTLULVYUOYROZPAPDDYEQKNGIERVOAXSPLABMB",
          Instant.now().minus(2, HOURS)
        ),
        PaymentObservingAccount(
          "GAPBFA5ZYG5VVKN7WPMH6K5CBXGU2AM5ED7S54VX27J7S222NKMTWKR6",
          Instant.now().minus(12, HOURS)
        )
      )
  }

  @AfterEach
  fun teardown() {
    clearAllMocks()
    unmockkAll()
  }

  @Test
  fun test_addAndRemove_success() {
    val obs = PaymentObservingAccountsManager(paymentObservingAccountStore)
    obs.initialize()

    obs.upsert("GCIWQDKACLW26UJXY5CTLULVYUOYROZPAPDDYEQKNGIERVOAXSPLABMB", true)
    obs.upsert("GCK5ECMM67ZN7RWGUSDKQAW6CAF6Q5WYK2VQJ276SJMINU6WVCIQW6BL", true)
    obs.upsert("GAPBFA5ZYG5VVKN7WPMH6K5CBXGU2AM5ED7S54VX27J7S222NKMTWKR6", true)

    assertEquals(3, obs.accounts.size)

    obs.remove(null)
    assertEquals(3, obs.accounts.size)

    obs.remove("GCIWQDKACLW26UJXY5CTLULVYUOYROZPAPDDYEQKNGIERVOAXSPLABMB")
    obs.remove("GCIWQDKACLW26UJXY5CTLULVYUOYROZPAPDDYEQKNGIERVOAXSPLABMB")
    assertEquals(2, obs.accounts.size)
    assertTrue(obs.observe("GCK5ECMM67ZN7RWGUSDKQAW6CAF6Q5WYK2VQJ276SJMINU6WVCIQW6BL"))
    assertTrue(obs.observe("GAPBFA5ZYG5VVKN7WPMH6K5CBXGU2AM5ED7S54VX27J7S222NKMTWKR6"))
  }

  @Test
  fun test_add_invalid() {
    val obs = PaymentObservingAccountsManager(paymentObservingAccountStore)
    obs.initialize()

    assertEquals(3, obs.accounts.size)
    obs.upsert("GB4DZFFUWC64MZ3BQ433ME7QBODCSFZRBOLWEWEMJIVHABTWGT3W2Q22", true)
    assertDoesNotThrow {
      obs.upsert("GB4DZFFUWC64MZ3BQ433ME7QBODCSFZRBOLWEWEMJIVHABTWGT3W2Q22", true)
    }
    assertEquals(4, obs.accounts.size)
  }

  @Test
  fun test_evict() {
    val obs = PaymentObservingAccountsManager(paymentObservingAccountStore)

    obs.initialize()

    // Nothing evict-able
    assertEquals(3, obs.accounts.size)
    obs.evict(Duration.of(1, DAYS))
    assertEquals(3, obs.accounts.size)
    obs.evict(Duration.of(1, DAYS))
    assertEquals(3, obs.accounts.size)

    obs.upsert(
      PaymentObservingAccountsManager.ObservingAccount(
        "GB4DZFFUWC64MZ3BQ433ME7QBODCSFZRBOLWEWEMJIVHABTWGT3W2Q22",
        Instant.now().minus(24, HOURS),
        true
      )
    )

    obs.upsert(
      PaymentObservingAccountsManager.ObservingAccount(
        "GCC2B7LML6UBKBA7NOMLCR57HPKMFHRO2VTZGSIMRLXDMECBXQ44MOYO",
        Instant.now().minus(100, DAYS),
        false
      )
    )

    assertEquals(5, obs.accounts.size)

    // Evict GB4DZFFUWC64MZ3BQ433ME7QBODCSFZRBOLWEWEMJIVHABTWGT3W2Q22
    obs.evict(Duration.of(23, HOURS))
    assertFalse(obs.observe("GB4DZFFUWC64MZ3BQ433ME7QBODCSFZRBOLWEWEMJIVHABTWGT3W2Q22"))
    assertEquals(4, obs.accounts.size)

    // Test idempotency
    obs.evict(Duration.of(23, HOURS))
    assertEquals(4, obs.accounts.size)

    // Update the last observed timestamp to avoid being evicted
    assertTrue(obs.observe("GAPBFA5ZYG5VVKN7WPMH6K5CBXGU2AM5ED7S54VX27J7S222NKMTWKR6"))
    obs.evict(Duration.of(11, HOURS))
    assertEquals(4, obs.accounts.size)

    // Make sure observing random account return false
    assertFalse(obs.observe("GBXXYA2NZPCS2LHLXBWOQ6UXXRCH3N5YVTYWZ4DEVYTEWVFV7R7MEKSV"))

    // Evict another one
    obs.evict(Duration.of(1, HOURS))
    assertEquals(3, obs.accounts.size)

    // Evict all expiring
    obs.evict(Duration.ZERO)
    assertEquals(1, obs.accounts.size)
  }

  @Test
  fun test_whenEvictAndPersist_thenSuccessful() {
    val obs = spyk(PaymentObservingAccountsManager(paymentObservingAccountStore))
    assertEquals(0, obs.accounts.size)

    obs.initialize()
    assertEquals(3, obs.accounts.size)

    obs.evictAndPersist()
    assertEquals(3, obs.accounts.size)

    obs.upsert("GBXXYA2NZPCS2LHLXBWOQ6UXXRCH3N5YVTYWZ4DEVYTEWVFV7R7MEKSV", true)
    assertEquals(4, obs.accounts.size)
    Thread.sleep(10)

    every { obs.evictMaxAge } returns Duration.of(1, DAYS)
    obs.evictAndPersist()
    assertEquals(4, obs.accounts.size)

    every { obs.evictMaxAge } returns Duration.ZERO
    obs.evictAndPersist()
    assertEquals(0, obs.accounts.size)
    verify(exactly = 1) {
      paymentObservingAccountStore.delete(
        "GBXXYA2NZPCS2LHLXBWOQ6UXXRCH3N5YVTYWZ4DEVYTEWVFV7R7MEKSV"
      )
    }
    verify(exactly = 1) {
      paymentObservingAccountStore.delete(
        "GCK5ECMM67ZN7RWGUSDKQAW6CAF6Q5WYK2VQJ276SJMINU6WVCIQW6BL"
      )
    }
    verify(exactly = 1) {
      paymentObservingAccountStore.delete(
        "GCIWQDKACLW26UJXY5CTLULVYUOYROZPAPDDYEQKNGIERVOAXSPLABMB"
      )
    }
    verify(exactly = 1) {
      paymentObservingAccountStore.delete(
        "GAPBFA5ZYG5VVKN7WPMH6K5CBXGU2AM5ED7S54VX27J7S222NKMTWKR6"
      )
    }
    verify(exactly = 4) { paymentObservingAccountStore.delete(any()) }
  }
}
