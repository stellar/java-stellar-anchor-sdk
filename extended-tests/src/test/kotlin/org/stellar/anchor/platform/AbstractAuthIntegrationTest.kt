package org.stellar.anchor.platform

import org.stellar.anchor.auth.AuthHelper
import org.stellar.anchor.auth.JwtService
import org.stellar.anchor.platform.AbstractIntegrationTest.Companion.ANCHOR_TO_PLATFORM_SECRET
import org.stellar.anchor.platform.AbstractIntegrationTest.Companion.PLATFORM_TO_ANCHOR_SECRET
import org.stellar.anchor.platform.AbstractIntegrationTest.Companion.PLATFORM_TO_CUSTODY_SECRET

const val GET_TRANSACTIONS_ENDPOINT = "GET,/transactions"
const val PATCH_TRANSACTIONS_ENDPOINT = "PATCH,/transactions"
const val GET_TRANSACTIONS_MY_ID_ENDPOINT = "GET,/transactions/my_id"
const val GET_EXCHANGE_QUOTES_ENDPOINT = "GET,/exchange/quotes"
const val GET_EXCHANGE_QUOTES_ID_ENDPOINT = "GET,/exchange/quotes/id"
const val POST_CUSTODY_TRANSACTION_ENDPOINT = "POST,/transactions"

abstract class AbstractAuthIntegrationTest {
  companion object {
    private val jwtService =
      JwtService(
        null,
        null,
        null,
        PLATFORM_TO_ANCHOR_SECRET,
        ANCHOR_TO_PLATFORM_SECRET,
        PLATFORM_TO_CUSTODY_SECRET
      )
    private val jwtWrongKeyService =
      JwtService(
        null,
        null,
        null,
        PLATFORM_TO_ANCHOR_SECRET + "bad",
        ANCHOR_TO_PLATFORM_SECRET + "bad",
        PLATFORM_TO_CUSTODY_SECRET + "bad"
      )

    internal val jwtAuthHelper = AuthHelper.forJwtToken(jwtService, 10000)
    internal val jwtWrongKeyAuthHelper = AuthHelper.forJwtToken(jwtWrongKeyService, 10000)
    internal val jwtExpiredAuthHelper = AuthHelper.forJwtToken(jwtService, 0)
    internal lateinit var testProfileRunner: TestProfileExecutor
  }
}
