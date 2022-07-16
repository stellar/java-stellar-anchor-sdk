package org.stellar.anchor.platform.callback

import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.unmockkAll
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.stellar.anchor.api.exception.AnchorException
import org.stellar.anchor.api.exception.BadRequestException
import org.stellar.anchor.api.exception.ServerErrorException
import org.stellar.anchor.api.sep.sep12.Field
import org.stellar.anchor.api.sep.sep12.ProvidedField
import org.stellar.anchor.api.sep.sep12.Sep12GetCustomerRequest
import org.stellar.anchor.api.sep.sep12.Sep12GetCustomerResponse
import org.stellar.anchor.api.sep.sep12.Sep12Status
import org.stellar.anchor.auth.AuthHelper
import org.stellar.anchor.util.GsonUtils

class RestCustomerIntegrationTest {
  private val httpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.MINUTES)
      .readTimeout(10, TimeUnit.MINUTES)
      .writeTimeout(10, TimeUnit.MINUTES)
      .build()
  private val authHelper = AuthHelper.forNone()
  private val gson = GsonUtils.getInstance()
  private lateinit var mockAnchor: MockWebServer
  private lateinit var mockAnchorUrl: String
  private lateinit var customerIntegration: RestCustomerIntegration

  @BeforeEach
  fun setup() {
    // mock Anchor backend
    mockAnchor = MockWebServer()
    mockAnchor.start()
    mockAnchorUrl = mockAnchor.url("").toString()

    MockKAnnotations.init(this, relaxUnitFun = true)
    customerIntegration = RestCustomerIntegration(mockAnchorUrl, httpClient, authHelper, gson)
  }

  @AfterEach
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
  }

  @Test
  fun test_getCustomer() {
    val getCustomerRequest =
      Sep12GetCustomerRequest.builder()
        .id("customer-id")
        .account("account")
        .memo("memo")
        .memoType("memoType")
        .type("sending_user")
        .lang("en")
        .build()

    mockAnchor.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(
          """{
            "id": "customer-id",
            "status": "NEEDS_INFO",
            "message": "foo bar",
            "fields": {
              "email_address": {
                "description": "email address of the customer",
                "type": "string",
                "optional": false
              }
            },
            "provided_fields": {
              "last_name": {
                "description": "The customer's last name",
                "type": "string",
                "status": "ACCEPTED"
              }
            }
        }""".trimMargin()
        )
    )

    val wantResponse = Sep12GetCustomerResponse()
    wantResponse.id = "customer-id"
    wantResponse.status = Sep12Status.NEEDS_INFO
    wantResponse.message = "foo bar"
    val emailField = Field()
    emailField.description = "email address of the customer"
    emailField.type = Field.Type.STRING
    emailField.optional = false
    wantResponse.fields = mapOf("email_address" to emailField)
    val lastNameProvidedField = ProvidedField()
    lastNameProvidedField.description = "The customer's last name"
    lastNameProvidedField.type = Field.Type.STRING
    lastNameProvidedField.status = Sep12Status.ACCEPTED
    wantResponse.providedFields = mapOf("last_name" to lastNameProvidedField)

    val gotResponse = customerIntegration.getCustomer(getCustomerRequest)
    assertEquals(wantResponse, gotResponse)

    val request = mockAnchor.takeRequest()
    assertEquals("GET", request.method)
    assertEquals("application/json", request.headers["Content-Type"])
    assertNull(request.headers["Authorization"])
    val wantEndpoint =
      """/customer
        ?id=customer-id
        &account=account
        &memo=memo
        &memo_type=memoType
        &type=sending_user
        &lang=en
        """.replace(
        "\n        ",
        ""
      )
    MatcherAssert.assertThat(request.path, CoreMatchers.endsWith(wantEndpoint))
    assertEquals("", request.body.readUtf8())
  }

  @Test
  fun test_getCustomer_failure() {
    // invalid customer.status in the response
    val getCustomerRequest = Sep12GetCustomerRequest.builder().id("customer-id").build()

    mockAnchor.enqueue(MockResponse().setResponseCode(200).setBody("{}".trimMargin()))

    val wantResponse = Sep12GetCustomerResponse()
    wantResponse.id = "customer-id"

    var ex: AnchorException = assertThrows { customerIntegration.getCustomer(getCustomerRequest) }
    assertInstanceOf(ServerErrorException::class.java, ex)
    assertEquals("internal server error: result from Anchor backend is invalid", ex.message)

    val request = mockAnchor.takeRequest()
    assertEquals("GET", request.method)
    assertEquals("application/json", request.headers["Content-Type"])
    assertNull(request.headers["Authorization"])
    val wantEndpoint = """/customer?id=customer-id"""
    MatcherAssert.assertThat(request.path, CoreMatchers.endsWith(wantEndpoint))
    assertEquals("", request.body.readUtf8())

    // invalid request status code
    mockAnchor.enqueue(
      MockResponse()
        .setResponseCode(400)
        .setBody("""{"error": "foo bar went wrong"}""".trimMargin())
    )
    ex = assertThrows { customerIntegration.getCustomer(getCustomerRequest) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("foo bar went wrong", ex.message)
  }
}
