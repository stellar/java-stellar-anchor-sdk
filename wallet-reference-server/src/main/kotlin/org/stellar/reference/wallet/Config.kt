package org.stellar.reference.wallet

data class LocationConfig(val walletServerConfig: String)

data class Config(val wallet: WalletConfig, val anchor: AnchorConfig)

data class AnchorConfig(val domain: String, val endpoint: String)

data class WalletConfig(val port: Int, val hostname: String)
