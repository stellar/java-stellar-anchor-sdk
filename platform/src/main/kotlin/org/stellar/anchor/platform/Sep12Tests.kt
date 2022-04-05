package org.stellar.anchor.platform

import org.stellar.anchor.dto.sep12.Sep12PutCustomerRequest
import org.stellar.anchor.dto.sep12.Sep12Status
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.Sep1Helper

const val testCustomerJson =
  """
{
  "first_name": "John",
  "last_name": "Doe",
  "address": "123 Washington Street",
  "city": "San Francisco",
  "state_or_province": "CA",
  "address_country_code": "US"
}
"""

fun sep12TestAll(toml: Sep1Helper.TomlContent, jwt: String) {
  println("Performing SEP12 tests...")
  sep12Client = Sep12Client(toml.getString("KYC_SERVER"), jwt)

  sep12TestHappyPath()
}

fun sep12TestHappyPath() {
  val customer =
    GsonUtils.getInstance().fromJson(testCustomerJson, Sep12PutCustomerRequest::class.java)

  // Upload a customer
  printRequest("Calling PUT /customer", customer)
  var pr = sep12Client.putCustomer(customer)
  printResponse(pr)

  // make sure the customer was uploaded correctly.
  printRequest("Calling GET /customer", customer)
  var gr = sep12Client.getCustomer(pr!!.id)
  printResponse(gr)

  assert(gr!!.id.equals(pr.id))
  assert(gr.status.equals(Sep12Status.NEEDS_INFO))

  customer.emailAddress = "john.doe@stellar.org"
  customer.bankAccountNumber = "1234"
  customer.bankNumber = "abcd"
  customer.type = "sep31-receiver"

  // Modify the customer
  printRequest("Calling PUT /customer", customer)
  pr = sep12Client.putCustomer(customer)
  printResponse(pr)

  // Make sure the customer is modified correctly.
  printRequest("Calling GET /customer", customer)
  gr = sep12Client.getCustomer(pr!!.id)
  printResponse(gr)

  assert(gr!!.id.equals(pr.id))
  assert(gr.status.equals(Sep12Status.ACCEPTED))

  // Delete the customer
  printRequest("Calling DELETE /customer/${CLIENT_WALLET_ACCOUNT}")
  val code = sep12Client.deleteCustomer(CLIENT_WALLET_ACCOUNT)
  printResponse(code)
  // currently, not implemented
  assert(code == 500)
}
