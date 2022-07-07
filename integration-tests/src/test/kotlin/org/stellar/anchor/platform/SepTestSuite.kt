package org.stellar.anchor.platform

import org.apache.commons.cli.*
import org.stellar.anchor.auth.JwtService
import org.stellar.anchor.auth.JwtToken
import org.stellar.anchor.util.Sep1Helper
import org.stellar.anchor.util.Sep1Helper.TomlContent

var CLIENT_WALLET_ACCOUNT = "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
var CLIENT_WALLET_SECRET = "SBHTWEF5U7FK53FLGDMBQYGXRUJ24VBM3M6VDXCHRIGCRG3Z64PH45LW"

var jwt: String? = null
val jwtService = JwtService("secret")

fun main(args: Array<String>) {
  // Start necessary servers
  val options = Options()
  options.addOption("h", "help", false, "Print this message.")
  options.addOption("a", "start-all", false, "Start all servers.")
  options.addOption("s", "start-sep-server", false, "Start SEP endpoint test server.")
  options.addOption(
    "r",
    "start-anchor-reference-server",
    false,
    "Start anchor reference test server."
  )
  options.addOption("t", "sep1-toml", true, "The path where the SEP1 TOML file can be read.")
  val sepTestOptions =
    Option(
      "p",
      "tests",
      false,
      "Tests to be performed. One or multiple of [sep1,sep10,sep12,sep24,sep31,ref,platform]"
    )
  sepTestOptions.isRequired = true
  sepTestOptions.args = Option.UNLIMITED_VALUES
  options.addOption(sepTestOptions)

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
      if (cmd.hasOption("sep1-toml")) {
        resourceAsString(cmd.getOptionValue("t"))
      } else {
        resourceAsString("classpath:/sep1/test-stellar.toml")
      }

    val toml = Sep1Helper.parse(tomlString)
    val tests = cmd.getOptionValues("p")

    if ("sep10" in tests) {
      jwt = sep10TestAll(toml)
    }

    if ("sep12" in tests) {
      sep12TestAll(toml, getOrCreateJwt(toml)!!)
    }

    if ("sep24" in tests) {
      sep24TestAll(toml, getOrCreateJwt(toml)!!)
    }

    if ("sep31" in tests) {
      sep31TestAll(toml, getOrCreateJwt(toml)!!)
    }

    if ("sep38" in tests) {
      sep38TestAll(toml, getOrCreateJwt(toml)!!)
    }

    if ("platform" in tests) {
      platformTestAll(toml, getOrCreateJwt(toml)!!)
    }

    if ("ref" in tests) {
      referenceServerTestAll()
    }
  } catch (ex: ParseException) {
    printUsage(options)
  }
}

fun getOrCreateJwt(tomlContent: TomlContent): String? {
  if (jwt == null) {
    val issuedAt: Long = System.currentTimeMillis() / 1000L
    val token =
      JwtToken.of(
        tomlContent.getString("WEB_AUTH_ENDPOINT"),
        CLIENT_WALLET_ACCOUNT,
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
  // TODO: Change this when we refactor the project structure.
  helper.printHelp("java -jar anchor-platform.jar", options)
}
