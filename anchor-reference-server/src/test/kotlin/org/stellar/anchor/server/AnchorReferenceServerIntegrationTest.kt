package org.stellar.anchor.server

import com.google.gson.Gson
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.TestPropertySource
import org.stellar.anchor.reference.AnchorReferenceServer
import org.stellar.platform.apis.callbacks.requests.GetCustomerRequest
import org.stellar.platform.apis.callbacks.responses.GetCustomerResponse

@SpringBootTest(
  classes = [AnchorReferenceServer::class],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(locations = ["classpath:/anchor-reference-server.yaml"])
class AnchorReferenceServerIntegrationTest {
  companion object {
    val gson = Gson()
  }

  @Autowired lateinit var restTemplate: TestRestTemplate

  @Test
  fun getCustomer() {
    val result = restGetCustomer(GetCustomerRequest.builder().id("1").build())
    println(result.body)
    assertNotNull(result)
    assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
  }

  fun restGetCustomer(getCustomerRequest: GetCustomerRequest): ResponseEntity<GetCustomerResponse> {
    val json = gson.toJson(getCustomerRequest)
    val params = gson.fromJson(json, HashMap::class.java)

    return restTemplate.getForEntity("/customer?id={id}", GetCustomerResponse::class.java, params)
  }
}
