package org.stellar.anchor.server

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import org.stellar.anchor.reference.AnchorReferenceServer
import org.stellar.anchor.reference.model.Customer

@SpringBootTest(
  classes = [AnchorReferenceServer::class],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(locations = ["classpath:application-integration-test.properties"])
class AnchorReferenceServerTest {
  @Autowired lateinit var restTemplate: TestRestTemplate
  @Test
  fun getCustomer() {
    val result = restTemplate.getForEntity("/customers?id=1", Customer::class.java)

    assertNotNull(result)
    assertEquals(HttpStatus.NOT_FOUND, result?.statusCode)
  }
}
