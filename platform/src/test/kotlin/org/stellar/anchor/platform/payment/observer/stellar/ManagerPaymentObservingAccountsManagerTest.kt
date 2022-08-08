package org.stellar.anchor.platform.payment.observer.stellar

import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.api.exception.AlreadyExistsException

class ManagerPaymentObservingAccountsManagerTest {
  @MockK private lateinit var paymentObservingAccountStore: PaymentObservingAccountStore

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxed = true)
  }

  @AfterEach
  fun teardown() {
    clearAllMocks()
    unmockkAll()
  }

  @Test
  fun test_addAndRemove_success() {
    val obs = PaymentObservingAccountsManager(paymentObservingAccountStore)
    obs.add("GCIWQDKACLW26UJXY5CTLULVYUOYROZPAPDDYEQKNGIERVOAXSPLABMB", true)
    obs.add("GCK5ECMM67ZN7RWGUSDKQAW6CAF6Q5WYK2VQJ276SJMINU6WVCIQW6BL", true)
    obs.add("GAPBFA5ZYG5VVKN7WPMH6K5CBXGU2AM5ED7S54VX27J7S222NKMTWKR6", true)

    assertEquals(3, obs.accounts.size)

    obs.remove(null)
    assertEquals(3, obs.accounts.size)

    obs.remove("GCIWQDKACLW26UJXY5CTLULVYUOYROZPAPDDYEQKNGIERVOAXSPLABMB")
    obs.remove("GCIWQDKACLW26UJXY5CTLULVYUOYROZPAPDDYEQKNGIERVOAXSPLABMB")
    assertEquals(2, obs.accounts.size)
    assertTrue(obs.isObserving("GCK5ECMM67ZN7RWGUSDKQAW6CAF6Q5WYK2VQJ276SJMINU6WVCIQW6BL"))
    assertTrue(obs.isObserving("GAPBFA5ZYG5VVKN7WPMH6K5CBXGU2AM5ED7S54VX27J7S222NKMTWKR6"))
  }

  @Test
  fun test_add_invalid() {
    val obs = PaymentObservingAccountsManager(paymentObservingAccountStore)
    obs.add("GCIWQDKACLW26UJXY5CTLULVYUOYROZPAPDDYEQKNGIERVOAXSPLABMB", true)
    val ex =
      assertThrows<AlreadyExistsException> {
        obs.add("GCIWQDKACLW26UJXY5CTLULVYUOYROZPAPDDYEQKNGIERVOAXSPLABMB", true)
      }
    assertEquals(
      "The account GCIWQDKACLW26UJXY5CTLULVYUOYROZPAPDDYEQKNGIERVOAXSPLABMB is already being observed",
      ex.message
    )
  }

  @Test
  fun test_purge() {
    val obs = PaymentObservingAccountsManager(paymentObservingAccountStore)
    obs.add("GCIWQDKACLW26UJXY5CTLULVYUOYROZPAPDDYEQKNGIERVOAXSPLABMB", true)
    obs.add("GCK5ECMM67ZN7RWGUSDKQAW6CAF6Q5WYK2VQJ276SJMINU6WVCIQW6BL", true)
    obs.add("GAPBFA5ZYG5VVKN7WPMH6K5CBXGU2AM5ED7S54VX27J7S222NKMTWKR6", true)

    obs.purge(Duration.of(1, DAYS))
    assertEquals(3, obs.accounts.size)
    obs.purge(Duration.of(1, DAYS))
    assertEquals(3, obs.accounts.size)

    obs.add(
      PaymentObservingAccountsManager.ObservingAccount(
        "GB4DZFFUWC64MZ3BQ433ME7QBODCSFZRBOLWEWEMJIVHABTWGT3W2Q22",
        Instant.now().minus(10, DAYS),
        true
      )
    )
    obs.add(
      PaymentObservingAccountsManager.ObservingAccount(
        "GCC2B7LML6UBKBA7NOMLCR57HPKMFHRO2VTZGSIMRLXDMECBXQ44MOYO",
        Instant.now().minus(20, DAYS),
        true
      )
    )

    obs.purge(Duration.of(21, DAYS))
    assertEquals(5, obs.accounts.size)
    obs.purge(Duration.of(11, DAYS))
    assertEquals(4, obs.accounts.size)
    assertTrue(obs.isObserving("GB4DZFFUWC64MZ3BQ433ME7QBODCSFZRBOLWEWEMJIVHABTWGT3W2Q22"))
    assertFalse(obs.isObserving("GCC2B7LML6UBKBA7NOMLCR57HPKMFHRO2VTZGSIMRLXDMECBXQ44MOYO"))
    obs.purge(Duration.of(9, DAYS))
    assertEquals(3, obs.accounts.size)
    obs.purge(Duration.ZERO)
    assertEquals(0, obs.accounts.size)
  }
}
