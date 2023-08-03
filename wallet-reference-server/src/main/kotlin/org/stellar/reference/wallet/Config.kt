package org.stellar.reference.wallet

data class LocationConfig(val walletServerConfig: String)

data class Config(val port: Int, val anchor: AnchorConfig)

data class AnchorConfig(val domain: String, val endpoint: String)
