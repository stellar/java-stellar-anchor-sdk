package org.stellar.anchor.platform

import com.google.gson.GsonBuilder
import org.apache.commons.cli.*
import org.stellar.anchor.dto.sep12.Sep12PutCustomerRequest
import org.stellar.anchor.dto.sep12.Sep12Status

val gson = GsonBuilder().setPrettyPrinting().create()

fun main(args: Array<String>) {
  // Start necessary servers
  val options = Options()
  options.addOption("h", "help", false, "Print this message.")
  options.addOption("a", "all", false, "Start all servers.")
  options.addOption("s", "sep-server", false, "Start SEP endpoint server.")
  options.addOption("r", "anchor-reference-server", false, "Start anchor reference server.")

  val sepsOption = Option("p", "seps", true, "SEPS to be test. eg: sep12")
  sepsOption.args = Option.UNLIMITED_VALUES
  options.addOption(sepsOption)

  try {
    val parser: CommandLineParser = DefaultParser()
    val cmd = parser.parse(options, args)
    if (cmd.hasOption("sep-server") || cmd.hasOption("all")) {
      ServiceRunner.startSepServer()
    }
    if (cmd.hasOption("anchor-reference-server") || cmd.hasOption("all")) {
      ServiceRunner.startAnchorReferenceServer()
    }

    val seps = cmd.getOptionValues("p")

    if ("sep12" in seps) {
      testSep12()
    }
  } catch (ex: ParseException) {
    printUsage(options)
  }
}

fun printUsage(options: Options?) {
  val helper = HelpFormatter()
  helper.optionComparator = null
  helper.printHelp("java -jar anchor-platform.jar", options)
}

fun testSep12() {
  val sep12 = Sep12("http://localhost:8080/sep12")
  val customer = getTestPutCustomerRequest()

  println("Calling PUT /customer")
  print("request=")
  println(str(customer))
  var pr = sep12.putCustomer(customer)
  print("response=")
  println(str(pr))

  println("Calling GET /customer")
  var gr = sep12.getCustomer(pr!!.id)
  print("response=")
  println(str(gr))

  assert(gr!!.id.equals(pr.id))
  assert(gr.status.equals(Sep12Status.NEEDS_INFO))

  customer.emailAddress = "john.doe@stellar.org"
  customer.bankAccountNumber = "1234"
  customer.bankNumber = "abcd"
  customer.type = "sep31-receiver"

  println("Calling PUT /customer")
  print("request=")
  println(str(customer))
  pr = sep12.putCustomer(customer)
  print("response=")
  println(str(pr))

  println("Calling GET /customer")
  gr = sep12.getCustomer(pr!!.id)
  print("response=")
  print(str(gr))

  assert(gr!!.id.equals(pr.id))
  assert(gr.status.equals(Sep12Status.ACCEPTED))
}

fun str(value: Any?): String {
  if (value != null) return gson.toJson(value)
  return ""
}

fun getTestPutCustomerRequest(): Sep12PutCustomerRequest {
  val pcr = Sep12PutCustomerRequest()
  pcr.firstName = "John"
  pcr.lastName = "Doe"
  pcr.address = "123 Washington Street"
  pcr.city = "San Francisco"
  pcr.stateOrProvince = "CA"
  pcr.addressCountryCode = "US"

  return pcr
}
