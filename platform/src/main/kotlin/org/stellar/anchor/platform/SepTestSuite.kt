package org.stellar.anchor.platform

import org.apache.commons.cli.*
import org.stellar.anchor.sep10.JwtService
import org.stellar.anchor.sep10.JwtToken
import org.stellar.anchor.util.Sep1Helper
import org.stellar.anchor.util.Sep1Helper.TomlContent

fun main(args: Array<String>) {
  // Start necessary servers
  val options = Options()
  options.addOption("h", "help", false, "Print this message.")
  options.addOption("a", "all", false, "Start all servers.")
  options.addOption("s", "sep-server", false, "Start SEP endpoint server.")
  options.addOption("r", "anchor-reference-server", false, "Start anchor reference server.")
  options.addOption("t", "sep1-toml-path", true, "The path where the SEP1 TOML file can be read.")
  val sepsOption = Option("p", "seps", true, "SEPS to be test. eg: sep12")
  sepsOption.args = Option.UNLIMITED_VALUES
  options.addOption(sepsOption)

  try {
    val parser: CommandLineParser = DefaultParser()
    val cmd = parser.parse(options, args)

    // Start sep server if enabled.
    if (cmd.hasOption("sep-server") || cmd.hasOption("all")) {
      ServiceRunner.startSepServer()
    }

    // Start anchor reference server if enabled.
    if (cmd.hasOption("anchor-reference-server") || cmd.hasOption("all")) {
      ServiceRunner.startAnchorReferenceServer()
    }

    // Read TOML file
    val tomlString =
      if (cmd.hasOption("sep1-toml-path")) {
        resourceAsString(cmd.getOptionValue("t"))
      } else {
        resourceAsString("classpath:/org/stellar/anchor/platform/sep1/test_default_stellar.toml")
      }

    val toml = Sep1Helper.parse(tomlString)
    val seps = cmd.getOptionValues("p")

    if ("sep12" in seps) {
      sep12TestAll(toml, getOrCreateJwt(toml)!!)
    }

    if ("sep38" in seps) {
      sep38TestAll(toml, getOrCreateJwt(toml)!!)
    }

    if ("sep31" in seps) {
      sep31TestAll(toml, getOrCreateJwt(toml)!!)
    }
  } catch (ex: ParseException) {
    printUsage(options)
  }
}

var walletAccount = "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4"
var walletPrivateKey = "SAXKNDNU4FE2PCM5SOUXNXIQ7ZLCQMRZMMQAC2SIKZH7BAHQTTOLTIHW"
var jwt: String? = null
val jwtService = JwtService("secret")

fun getOrCreateJwt(tomlContent: TomlContent): String? {
  if (jwt == null) {
    val issuedAt: Long = System.currentTimeMillis() / 1000L
    val token =
      JwtToken.of(
        tomlContent.getString("WEB_AUTH_ENDPOINT"),
        walletAccount,
        issuedAt,
        issuedAt + 60,
        "",
        null
      )
    jwt = jwtService.encode(token)
  }

  return jwt
}

fun printUsage(options: Options?) {
  val helper = HelpFormatter()
  helper.optionComparator = null
  helper.printHelp("java -jar anchor-platform.jar", options)
}
