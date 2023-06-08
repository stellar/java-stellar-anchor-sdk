package org.stellar.anchor.platform.test

import java.lang.Thread.sleep
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.api.exception.SepNotFoundException
import org.stellar.anchor.api.sep.sep12.Sep12PutCustomerRequest
import org.stellar.anchor.api.sep.sep12.Sep12Status
import org.stellar.anchor.platform.*
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.Sep1Helper

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

class Sep12Tests(config: TestConfig, toml: Sep1Helper.TomlContent, jwt: String) {
  private lateinit var sep12Client: Sep12Client

  init {
    println("Performing SEP12 tests...")
    if (!::sep12Client.isInitialized) {
      sep12Client = Sep12Client(toml.getString("KYC_SERVER"), jwt)
    }
  }

  fun `test put, get customers`() {
    val customer =
      GsonUtils.getInstance().fromJson(testCustomer1Json, Sep12PutCustomerRequest::class.java)
    customer.emailAddress = null

    // Upload a customer
    printRequest("Calling PUT /customer", customer)
    var pr = sep12Client.putCustomer(customer)
    printResponse(pr)

    sleep(1000)

    // make sure the customer was uploaded correctly.
    printRequest("Calling GET /customer", customer)
    var gr = sep12Client.getCustomer(pr!!.id)
    printResponse(gr)

    assertEquals(Sep12Status.NEEDS_INFO, gr?.status)
    assertEquals(pr.id, gr?.id)

    customer.emailAddress = "john.doe@stellar.org"
    customer.type = "sep31-receiver"

    // Modify the customer
    printRequest("Calling PUT /customer", customer)
    pr = sep12Client.putCustomer(customer)
    printResponse(pr)

    // Make sure the customer is modified correctly.
    printRequest("Calling GET /customer", customer)
    gr = sep12Client.getCustomer(pr!!.id)
    printResponse(gr)

    assertEquals(pr.id, gr?.id)
    assertEquals(Sep12Status.ACCEPTED, gr?.status)

    // Delete the customer
    printRequest("Calling DELETE /customer/$CLIENT_WALLET_ACCOUNT")
    val code = sep12Client.deleteCustomer(CLIENT_WALLET_ACCOUNT)
    printResponse(code)
    // currently, not implemented
    assertEquals(200, code)

    val id = pr.id
    val ex: SepNotFoundException = assertThrows { sep12Client.getCustomer(id) }
    assertEquals("customer for 'id' '$id' not found", ex.message)
    println(ex)
  }
  fun testAll() {
    println("Performing Sep12 tests...")
    `test put, get customers`()
  }
}
