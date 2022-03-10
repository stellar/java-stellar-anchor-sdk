package org.stellar.anchor.platform

import org.stellar.anchor.dto.sep12.Sep12PutCustomerRequest
import org.stellar.anchor.dto.sep12.Sep12Status
import org.stellar.anchor.util.Sep1Helper

lateinit var sep12 : Sep12Client

fun sep12TestAll(toml: Sep1Helper.TomlContent, jwt: String) {
  sep12 = Sep12Client(toml.getString("KYC_SERVER"), jwt)

  sep12TestHappyPath()
}

fun sep12TestHappyPath() {
  val customer = getTestPutCustomerRequest("test_put_customer_request.json")

  // Upload a customer
  printRequest("Calling PUT /customer", customer)
  var pr = sep12.putCustomer(customer)
  printResponse(pr)

  // make sure the customer was uploaded correctly.
  printRequest("Calling GET /customer", customer)
  var gr = sep12.getCustomer(pr!!.id)
  printResponse(gr)

  assert(gr!!.id.equals(pr.id))
  assert(gr.status.equals(Sep12Status.NEEDS_INFO))

  customer.emailAddress = "john.doe@stellar.org"
  customer.bankAccountNumber = "1234"
  customer.bankNumber = "abcd"
  customer.type = "sep31-receiver"

  // Modify the customer
  printRequest("Calling PUT /customer", customer)
  pr = sep12.putCustomer(customer)
  printResponse(pr)

  // Make sure the customer is modified correctly.
  printRequest("Calling GET /customer", customer)
  gr = sep12.getCustomer(pr!!.id)
  printResponse(gr)

  assert(gr!!.id.equals(pr.id))
  assert(gr.status.equals(Sep12Status.ACCEPTED))

  // Delete the customer
  printRequest("Calling DELETE /customer/${walletAccount}", null)
  val code = sep12.deleteCustomer(walletAccount)
  printResponse(code)
  // currently, not implemented
  assert(code==500)
}

fun getTestPutCustomerRequest(resourcePath: String): Sep12PutCustomerRequest {
  return gson.fromJson(
      resourceAsString("classpath:/org/stellar/anchor/platform/sep12/$resourcePath"),
      Sep12PutCustomerRequest::class.java)
}
