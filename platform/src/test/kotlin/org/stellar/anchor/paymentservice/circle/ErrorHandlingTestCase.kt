package org.stellar.anchor.paymentservice.circle

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import reactor.core.publisher.Mono

class ErrorHandlingTestCase {
  val requestMono: Mono<*>
  private var mockResponses: List<MockResponse>? = null
  var mockResponsesMap: Map<String, MockResponse>? = null
    private set

  constructor(_requestMono: Mono<*>, _mockResponses: List<MockResponse>) {
    this.requestMono = _requestMono
    this.mockResponses = _mockResponses
  }

  constructor(_requestMono: Mono<*>, _mockResponsesMap: Map<String, MockResponse>) {
    this.requestMono = _requestMono
    this.mockResponsesMap = _mockResponsesMap
  }

  private fun getDispatcher(): Dispatcher? {
    if (mockResponsesMap == null) {
      return null
    }

    val dispatcher: Dispatcher =
      object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {

          if (!mockResponsesMap!!.containsKey(request.path)) {
            return MockResponse().setResponseCode(404)
          }

          return mockResponsesMap!![request.path]!!
        }
      }
    return dispatcher
  }

  fun prepareMockWebServer(server: MockWebServer) {
    val dispatcher = getDispatcher()
    if (dispatcher != null) {
      server.dispatcher = dispatcher
    } else if (mockResponses != null) {
      mockResponses!!.forEach { mockResponse -> server.enqueue(mockResponse) }
    }
  }
}
