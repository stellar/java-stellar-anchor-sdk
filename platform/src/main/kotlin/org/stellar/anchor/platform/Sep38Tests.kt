package org.stellar.anchor.platform

import org.stellar.anchor.util.Sep1Helper

lateinit var sep38: Sep38Client

fun sep38TestAll(toml: Sep1Helper.TomlContent, jwt: String) {
  sep38 = Sep38Client(toml.getString("KYC_SERVER"), jwt)

  sep38TestHappyPath()
}

fun sep38TestHappyPath() {}
