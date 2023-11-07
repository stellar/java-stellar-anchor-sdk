package org.stellar.anchor.platform.integrationtest

import io.ktor.client.plugins.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.api.sep.sep12.Sep12Status
import org.stellar.anchor.platform.AbstractIntegrationTests
import org.stellar.anchor.platform.TestConfig
import org.stellar.anchor.platform.printRequest
import org.stellar.anchor.platform.printResponse
import org.stellar.sdk.KeyPair
import org.stellar.walletsdk.anchor.auth
import org.stellar.walletsdk.horizon.SigningKeyPair

open class Sep12Tests : AbstractIntegrationTests(TestConfig(testProfileName = "default")) {
  init {
    runBlocking {
      // We have to override the default CLIENT_WALLET_SECRET because the deletion of the customer
      // will fail other tests that uses the same wallet.
      walletKeyPair = SigningKeyPair(KeyPair.random())
      token = anchor.auth().authenticate(walletKeyPair)
    }
  }

  @Test
  fun `test put, get customers`() = runBlocking {
    customerJson
    val customer = Json.decodeFromString<Map<String, String>>(customerJson).toMutableMap()
    customer.remove("emailAddress")

    // Upload a customer
    printRequest("Calling PUT /customer", customer)
    var pr = anchor.sep12(token).add(customer, "sep24")
    printResponse(pr)

    // make sure the customer was uploaded correctly.
    printRequest("Calling GET /customer", customer)
    var gr = anchor.sep12(token).getByIdAndType(pr.id, "sep24")
    printResponse(gr)

    assertEquals(pr.id, gr.id)

    customer["emailAddress"] = "john.doe@stellar.org"

    // Modify the customer
    printRequest("Calling PUT /customer", customer)
    pr = anchor.sep12(token).add(customer, "sep31-receiver")
    printResponse(pr)

    // Make sure the customer is modified correctly.
    printRequest("Calling GET /customer", customer)
    gr = anchor.sep12(token).getByIdAndType(pr.id, "sep31-receiver")
    printResponse(gr)

    assertEquals(pr.id, gr.id)
    assertEquals(Sep12Status.ACCEPTED.name, gr.status!!.status)

    // Delete the customer
    printRequest("Calling DELETE /customer/$walletKeyPair.address")
    anchor.sep12(token).delete(walletKeyPair.address)

    val ex: ClientRequestException = assertThrows {
      anchor.sep12(token).getByIdAndType(pr.id, "sep31-receiver")
    }
    println(ex)
  }

  companion object {
    const val customerJson =
      """
{
  "first_name": "John",
  "last_name": "Doe",
  "email_address": "johndoe@test.com",
  "address": "123 Washington Street",
  "city": "San Francisco",
  "state_or_province": "CA",
  "address_country_code": "US",
  "clabe_number": "1234",
  "bank_number": "abcd",
  "bank_account_number": "1234",
  "bank_account_type": "checking"
}
"""

    const val testCustomer1Json =
      """
{
  "first_name": "John",
  "last_name": "Doe",
  "email_address": "johndoe@test.com",
  "address": "123 Washington Street",
  "city": "San Francisco",
  "state_or_province": "CA",
  "address_country_code": "US",
  "clabe_number": "1234",
  "bank_number": "abcd",
  "bank_account_number": "1234",
  "bank_account_type": "checking"
}
"""

    const val testCustomer2Json =
      """
{
  "first_name": "Jane",
  "last_name": "Doe",
  "email_address": "janedoe@test.com",
  "address": "321 Washington Street",
  "city": "San Francisco",
  "state_or_province": "CA",
  "address_country_code": "US",
  "clabe_number": "5678",
  "bank_number": "efgh",
  "bank_account_number": "5678",
  "bank_account_type": "checking"
}
"""
  }
}
