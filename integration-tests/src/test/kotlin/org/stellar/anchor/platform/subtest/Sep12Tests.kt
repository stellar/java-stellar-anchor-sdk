package org.stellar.anchor.platform.subtest

import io.ktor.client.plugins.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.api.sep.sep12.Sep12Status
import org.stellar.anchor.platform.CLIENT_WALLET_ACCOUNT
import org.stellar.anchor.platform.TestConfig
import org.stellar.anchor.platform.printRequest
import org.stellar.anchor.platform.printResponse

class Sep12Tests : SepTests(TestConfig(testProfileName = "default")) {

  @Test
  fun `test put, get customers`() {
    runBlocking {
      customerJson
      val customer = Json.decodeFromString<Map<String, String>>(customerJson).toMutableMap()
      customer.remove("emailAddress")

      // Upload a customer
      printRequest("Calling PUT /customer", customer)
      var pr = anchor.sep12(token).add(customer, "sep24")
      printResponse(pr)

      // make sure the customer was uploaded correctly.
      printRequest("Calling GET /customer", customer)
      var gr = anchor.sep12(token).getByIdAndType(pr!!.id, "sep24")
      printResponse(gr)

      assertEquals(pr.id, gr?.id)

      customer["emailAddress"] = "john.doe@stellar.org"

      // Modify the customer
      printRequest("Calling PUT /customer", customer)
      pr = anchor.sep12(token).add(customer, "sep31-receiver")
      printResponse(pr)

      // Make sure the customer is modified correctly.
      printRequest("Calling GET /customer", customer)
      gr = anchor.sep12(token).getByIdAndType(pr!!.id, "sep31-receiver")
      printResponse(gr)

      assertEquals(pr.id, gr.id)
      assertEquals(Sep12Status.ACCEPTED.name, gr.status!!.status)

      // Delete the customer
      printRequest("Calling DELETE /customer/$CLIENT_WALLET_ACCOUNT")
      anchor.sep12(token).delete(CLIENT_WALLET_ACCOUNT)

      val ex: ClientRequestException = assertThrows {
        anchor.sep12(token).getByIdAndType(pr.id, "sep31-receiver")
      }
      println(ex)
    }
  }
}

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
