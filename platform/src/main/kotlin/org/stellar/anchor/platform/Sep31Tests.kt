package org.stellar.anchor.platform

import org.stellar.anchor.util.Sep1Helper

lateinit var sep31: Sep31Client

fun sep31TestAll(toml: Sep1Helper.TomlContent, jwt: String) {
  println("Performing SEP31 tests...")

  sep31 = Sep31Client(toml.getString("KYC_SERVER"), jwt)

  sep31TestHappyPath()
}

fun sep31TestHappyPath() {}
