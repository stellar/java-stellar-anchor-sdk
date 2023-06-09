@file:Suppress("unused")

package org.stellar.anchor.sep12

import io.mockk.*
import io.mockk.impl.annotations.MockK
import java.time.Instant
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.skyscreamer.jsonassert.JSONAssert
import org.stellar.anchor.api.callback.*
import org.stellar.anchor.api.exception.*
import org.stellar.anchor.api.sep.sep12.*
import org.stellar.anchor.api.shared.CustomerField
import org.stellar.anchor.api.shared.ProvidedCustomerField
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.DefaultAssetService
import org.stellar.anchor.auth.Sep10Jwt
import org.stellar.anchor.util.StringHelper.json

class Sep12ServiceTest {
  companion object {
    private const val TEST_ACCOUNT = "GBFZNZTFSI6TWLVAID7VOLCIFX2PMUOS2X7U6H4TNK4PAPSHPWMMUIZG"
    private const val TEST_MEMO = "123456"
    private const val TEST_MUXED_ACCOUNT =
      "MBFZNZTFSI6TWLVAID7VOLCIFX2PMUOS2X7U6H4TNK4PAPSHPWMMUAAAAAAAAAPCIA2IM"
    private const val CLIENT_DOMAIN = "demo-wallet.stellar.org"
    private const val TEST_HOST_URL = "http://localhost:8080"
    private const val wantedSep12GetCustomerResponse =
      """
{
  "id": "customer-id",
  "status": "ACCEPTED",
  "fields": {
    "email_address": {
      "type": "string",
      "description": "email address of the customer",
      "optional": false
    }
  },
  "provided_fields": {
    "last_name": {
      "type": "string",
      "description": "The customer\u0027s last name",
      "optional": false,
      "status": "ACCEPTED"
    }
  },
  "message": "foo bar"
}
    """
  }

  private val issuedAt = Instant.now().epochSecond
  private val expiresAt = issuedAt + 9000
  private val mockFields =
    mapOf<String, CustomerField>(
      "email_address" to
        CustomerField.builder()
          .type("string")
          .description("email address of the customer")
          .optional(false)
          .build()
    )
  private val mockProvidedFields =
    mapOf<String, ProvidedCustomerField>(
      "last_name" to
        ProvidedCustomerField.builder()
          .type("string")
          .description("The customer's last name")
          .optional(false)
          .status(Sep12Status.ACCEPTED.name)
          .build()
    )

  private lateinit var sep12Service: Sep12Service
  @MockK(relaxed = true) private lateinit var customerIntegration: CustomerIntegration
  @MockK(relaxed = true) private lateinit var assetService: AssetService

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)

    val rjas = DefaultAssetService.fromJsonResource("test_assets.json")
    val assets = rjas.listAllAssets()

    every { assetService.listAllAssets() } returns assets

    sep12Service = Sep12Service(customerIntegration, assetService)
  }

  @AfterEach
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
  }

  @Test
  fun `test validate request and token accounts`() {
    val mockRequestBase = mockk<Sep12CustomerRequestBase>(relaxed = true)

    // request account fails if not the same as token account
    var jwtToken = createJwtToken(TEST_ACCOUNT)
    every { mockRequestBase.account } returns "random-account"
    val ex: SepException = assertThrows {
      sep12Service.validateRequestAndTokenAccounts(mockRequestBase, jwtToken)
    }
    assertInstanceOf(SepNotAuthorizedException::class.java, ex)
    assertEquals("The account specified does not match authorization token", ex.message)

    // request account succeeds when the same as token account
    every { mockRequestBase.account } returns TEST_ACCOUNT
    assertDoesNotThrow { sep12Service.validateRequestAndTokenAccounts(mockRequestBase, jwtToken) }

    // request account succeeds when the same as token's base (demuxed) account
    jwtToken = createJwtToken(TEST_MUXED_ACCOUNT)
    assertDoesNotThrow { sep12Service.validateRequestAndTokenAccounts(mockRequestBase, jwtToken) }

    // request account succeeds when the same as token's muxed account
    jwtToken = createJwtToken(TEST_MUXED_ACCOUNT)
    every { mockRequestBase.account } returns TEST_MUXED_ACCOUNT
    assertDoesNotThrow { sep12Service.validateRequestAndTokenAccounts(mockRequestBase, jwtToken) }

    // request account succeeds when the same as token's base account when using "account:memo"
    jwtToken = createJwtToken("$TEST_ACCOUNT:$TEST_MEMO")
    every { mockRequestBase.account } returns TEST_ACCOUNT
    assertDoesNotThrow { sep12Service.validateRequestAndTokenAccounts(mockRequestBase, jwtToken) }
  }

  @Test
  fun `test validate request and token memos`() {
    val mockRequestBase = mockk<Sep12CustomerRequestBase>(relaxed = true)

    // If the token doesn't have a memo nor a Muxed account id, does not fail for empty request memo
    var jwtToken = createJwtToken(TEST_ACCOUNT)
    assertDoesNotThrow { sep12Service.validateRequestAndTokenMemos(mockRequestBase, jwtToken) }

    // If the token doesn't have a memo nor a Muxed account id, does not fail for any request memo
    every { mockRequestBase.memo } returns "random-memo"
    assertDoesNotThrow { sep12Service.validateRequestAndTokenMemos(mockRequestBase, jwtToken) }

    // If the token has a memo that's different from the request's, throw an error
    jwtToken = createJwtToken("$TEST_ACCOUNT:$TEST_MEMO")
    every { mockRequestBase.memo } returns "random-memo"
    var ex: SepException = assertThrows {
      sep12Service.validateRequestAndTokenMemos(mockRequestBase, jwtToken)
    }
    assertInstanceOf(SepNotAuthorizedException::class.java, ex)
    assertEquals("The memo specified does not match the memo ID authorized via SEP-10", ex.message)

    // If the token has a memo that's equals the request's, succeed!
    jwtToken = createJwtToken("$TEST_ACCOUNT:$TEST_MEMO")
    every { mockRequestBase.memo } returns TEST_MEMO
    assertDoesNotThrow { sep12Service.validateRequestAndTokenMemos(mockRequestBase, jwtToken) }

    // If the token has a memo that's different from the request's Muxed id, throw an error
    jwtToken = createJwtToken(TEST_MUXED_ACCOUNT)
    every { mockRequestBase.memo } returns "random-memo"
    ex = assertThrows { sep12Service.validateRequestAndTokenMemos(mockRequestBase, jwtToken) }
    assertInstanceOf(SepNotAuthorizedException::class.java, ex)
    assertEquals("The memo specified does not match the memo ID authorized via SEP-10", ex.message)

    // If the token has a memo that's equals the request's Muxed id, succeed!
    jwtToken = createJwtToken(TEST_MUXED_ACCOUNT)
    every { mockRequestBase.memo } returns TEST_MEMO
    assertDoesNotThrow { sep12Service.validateRequestAndTokenMemos(mockRequestBase, jwtToken) }
  }

  @Test
  fun `test update request memo and memo type`() {
    val mockRequestBase = mockk<Sep12CustomerRequestBase>(relaxed = true)

    // if the request doesn't have any kind of memo, make sure the memo type is empty and return
    var jwtToken = createJwtToken(TEST_ACCOUNT)
    every { mockRequestBase.memo } returns null
    assertDoesNotThrow { sep12Service.updateRequestMemoAndMemoType(mockRequestBase, jwtToken) }
    verify(exactly = 1) { mockRequestBase.memo }
    verify(exactly = 1) { mockRequestBase.memoType = null }

    // if the request memo is present but memoType is empty, default memoType to MEMO_ID
    every { mockRequestBase.memo } returns TEST_MEMO
    every { mockRequestBase.memoType } returns null
    assertDoesNotThrow { sep12Service.updateRequestMemoAndMemoType(mockRequestBase, jwtToken) }
    verify(exactly = 2) { mockRequestBase.memo }
    verify(exactly = 1) { mockRequestBase.memoType = "id" }

    // if the token memo is present, default memoType to MEMO_ID
    jwtToken = createJwtToken("$TEST_ACCOUNT:$TEST_MEMO")
    every { mockRequestBase.memo } returns TEST_MEMO
    every { mockRequestBase.memoType } returns "text"
    assertDoesNotThrow { sep12Service.updateRequestMemoAndMemoType(mockRequestBase, jwtToken) }
    verify(exactly = 3) { mockRequestBase.memo }
    verify(exactly = 2) { mockRequestBase.memoType = "id" }

    // if the token muxed id is present, default memoType to MEMO_ID
    jwtToken = createJwtToken(TEST_MUXED_ACCOUNT)
    every { mockRequestBase.memo } returns TEST_MEMO
    every { mockRequestBase.memoType } returns "text"
    assertDoesNotThrow { sep12Service.updateRequestMemoAndMemoType(mockRequestBase, jwtToken) }
    verify(exactly = 4) { mockRequestBase.memo }
    verify(exactly = 3) { mockRequestBase.memoType = "id" }

    // works with memoType text when no memo was used in the token
    jwtToken = createJwtToken(TEST_ACCOUNT)
    every { mockRequestBase.memo } returns TEST_MEMO
    every { mockRequestBase.memoType } returns "text"
    assertDoesNotThrow { sep12Service.updateRequestMemoAndMemoType(mockRequestBase, jwtToken) }
    verify(exactly = 5) { mockRequestBase.memo }
    verify(exactly = 1) { mockRequestBase.memoType = "text" }

    // tests if the memos are being validated according to their types
    jwtToken = createJwtToken(TEST_ACCOUNT)
    every { mockRequestBase.memo } returns TEST_MEMO
    every { mockRequestBase.memoType } returns "hash"
    val ex: SepException = assertThrows {
      sep12Service.updateRequestMemoAndMemoType(mockRequestBase, jwtToken)
    }
    assertInstanceOf(SepValidationException::class.java, ex)
    assertEquals("Invalid 'memo' for 'memo_type'", ex.message)
  }

  @Test
  fun `Test put customer request ok`() {
    // mock `PUT {callbackApi}/customer` response
    val callbackApiPutRequestSlot = slot<PutCustomerRequest>()
    val mockCallbackApiPutCustomerResponse = PutCustomerResponse()
    mockCallbackApiPutCustomerResponse.id = "customer-id"
    every { customerIntegration.putCustomer(capture(callbackApiPutRequestSlot)) } returns
      mockCallbackApiPutCustomerResponse

    // Execute the request
    val mockPutRequest =
      Sep12PutCustomerRequest.builder()
        .account(TEST_ACCOUNT)
        .memo(TEST_MEMO)
        .memoType("id")
        .type("sending_user")
        .firstName("John")
        .build()
    val jwtToken = createJwtToken(TEST_ACCOUNT)
    assertDoesNotThrow { sep12Service.putCustomer(jwtToken, mockPutRequest) }

    // validate the request
    val wantCallbackApiPutRequest =
      PutCustomerRequest.builder()
        .account(TEST_ACCOUNT)
        .memo(TEST_MEMO)
        .memoType("id")
        .type("sending_user")
        .firstName("John")
        .build()
    assertEquals(wantCallbackApiPutRequest, callbackApiPutRequestSlot.captured)

    // validate the response
    verify(exactly = 1) { customerIntegration.putCustomer(any()) }
    assertEquals(TEST_ACCOUNT, mockPutRequest.account)
  }

  @Test
  fun `Test get customer request ok`() {
    // mock `GET {callbackApi}/customer` response
    val callbackApiGetRequestSlot = slot<GetCustomerRequest>()
    val mockCallbackApiGetCustomerResponse = GetCustomerResponse()
    mockCallbackApiGetCustomerResponse.id = "customer-id"
    mockCallbackApiGetCustomerResponse.status = Sep12Status.ACCEPTED.name
    mockCallbackApiGetCustomerResponse.fields = mockFields
    mockCallbackApiGetCustomerResponse.providedFields = mockProvidedFields
    mockCallbackApiGetCustomerResponse.message = "foo bar"
    every { customerIntegration.getCustomer(capture(callbackApiGetRequestSlot)) } returns
      mockCallbackApiGetCustomerResponse

    // Execute the request
    val mockGetRequest =
      Sep12GetCustomerRequest.builder()
        .memo(TEST_MEMO)
        .memoType("text")
        .type("sep31_sender")
        .lang("en")
        .build()
    val jwtToken = createJwtToken(TEST_ACCOUNT)
    assertDoesNotThrow {
      val resp = sep12Service.getCustomer(jwtToken, mockGetRequest)
      JSONAssert.assertEquals(wantedSep12GetCustomerResponse, json(resp), false)
    }

    // validate the request
    val wantCallbackApiGetRequest =
      GetCustomerRequest.builder()
        .account(TEST_ACCOUNT)
        .memo(TEST_MEMO)
        .memoType("text")
        .type("sep31_sender")
        .lang("en")
        .build()
    assertEquals(wantCallbackApiGetRequest, callbackApiGetRequestSlot.captured)

    // validate the response
    verify(exactly = 1) { customerIntegration.getCustomer(any()) }
    assertEquals(TEST_ACCOUNT, mockGetRequest.account)
  }

  @Test
  fun `test delete customer validation`() {
    every { customerIntegration.deleteCustomer(any()) } just Runs

    // PART 1 - account without memo
    // throws exception if request is missing the account
    var jwtToken = createJwtToken(TEST_ACCOUNT)
    var ex: AnchorException = assertThrows {
      sep12Service.deleteCustomer(jwtToken, null, null, null)
    }
    assertInstanceOf(SepNotAuthorizedException::class.java, ex)
    assertEquals("Not authorized to delete account [null] with memo [null]", ex.message)

    // throws exception if request account is different from token account
    ex = assertThrows { sep12Service.deleteCustomer(jwtToken, "foo", null, null) }
    assertInstanceOf(SepNotAuthorizedException::class.java, ex)
    assertEquals("Not authorized to delete account [foo] with memo [null]", ex.message)

    // succeeds if request account is equals the token's
    assertDoesNotThrow { sep12Service.deleteCustomer(jwtToken, TEST_ACCOUNT, null, null) }

    // PART 2 - account with memo
    // throws exception if request is missing the memo
    jwtToken = createJwtToken("$TEST_ACCOUNT:$TEST_MEMO")
    ex = assertThrows { sep12Service.deleteCustomer(jwtToken, TEST_ACCOUNT, null, null) }
    assertInstanceOf(SepNotAuthorizedException::class.java, ex)
    assertEquals("Not authorized to delete account [$TEST_ACCOUNT] with memo [null]", ex.message)

    // throws exception if request account is different from token account
    ex = assertThrows { sep12Service.deleteCustomer(jwtToken, TEST_ACCOUNT, "bar", null) }
    assertInstanceOf(SepNotAuthorizedException::class.java, ex)
    assertEquals("Not authorized to delete account [$TEST_ACCOUNT] with memo [bar]", ex.message)

    // succeeds if request account and memo are equal the token's
    assertDoesNotThrow { sep12Service.deleteCustomer(jwtToken, TEST_ACCOUNT, TEST_MEMO, null) }

    // succeeds if the request account is equals the token's, and the token memo is empty while the
    // request's is not
    jwtToken = createJwtToken(TEST_ACCOUNT)
    assertDoesNotThrow { sep12Service.deleteCustomer(jwtToken, TEST_ACCOUNT, "foo_bar", null) }

    // PART 3 - muxed account
    // throws exception if request is missing the memo
    jwtToken = createJwtToken(TEST_MUXED_ACCOUNT)
    ex = assertThrows { sep12Service.deleteCustomer(jwtToken, TEST_ACCOUNT, null, null) }
    assertInstanceOf(SepNotAuthorizedException::class.java, ex)
    assertEquals("Not authorized to delete account [$TEST_ACCOUNT] with memo [null]", ex.message)

    // throws exception if request account is different from token account
    ex = assertThrows { sep12Service.deleteCustomer(jwtToken, TEST_ACCOUNT, "bar", null) }
    assertInstanceOf(SepNotAuthorizedException::class.java, ex)
    assertEquals("Not authorized to delete account [$TEST_ACCOUNT] with memo [bar]", ex.message)

    // succeeds if request account is equals the token's and the memo is equals the token muxed id
    assertDoesNotThrow { sep12Service.deleteCustomer(jwtToken, TEST_ACCOUNT, TEST_MEMO, null) }

    // succeeds if request account is equals the token's muxed account
    assertDoesNotThrow { sep12Service.deleteCustomer(jwtToken, TEST_MUXED_ACCOUNT, null, null) }
  }

  @Test
  fun `test delete customer`() {
    // mock callbackApi customer integration
    val deleteCustomerIdSlot = slot<String>()
    every { customerIntegration.deleteCustomer(capture(deleteCustomerIdSlot)) } just Runs

    // attempting to delete a non-existent customer returns 404
    val mockNoCustomerFound = GetCustomerResponse()
    every { customerIntegration.getCustomer(any()) } returns mockNoCustomerFound

    val jwtToken = createJwtToken(TEST_ACCOUNT)
    val ex: AnchorException = assertThrows {
      sep12Service.deleteCustomer(jwtToken, TEST_ACCOUNT, TEST_MEMO, null)
    }
    assertInstanceOf(SepNotFoundException::class.java, ex)
    assertEquals("User not found.", ex.message)
    // Verify getting customer for every existing type
    verify(exactly = 5) { customerIntegration.getCustomer(any()) }
    verify(exactly = 0) { customerIntegration.deleteCustomer(any()) }

    // customer deletion succeeds
    val mockValidCustomerFound = GetCustomerResponse()
    mockValidCustomerFound.id = "customer-id"
    every { customerIntegration.getCustomer(any()) } returns mockValidCustomerFound
    assertDoesNotThrow { sep12Service.deleteCustomer(jwtToken, TEST_ACCOUNT, TEST_MEMO, null) }
    verify(exactly = 10) { customerIntegration.getCustomer(any()) }
    // callback API is called twice
    verify(exactly = 5) { customerIntegration.deleteCustomer(any()) }
    val wantDeleteCustomerId = "customer-id"
    assertEquals(wantDeleteCustomerId, deleteCustomerIdSlot.captured)
  }

  private fun createJwtToken(subject: String): Sep10Jwt {
    return Sep10Jwt.of("$TEST_HOST_URL/auth", subject, issuedAt, expiresAt, "", CLIENT_DOMAIN)
  }
}
