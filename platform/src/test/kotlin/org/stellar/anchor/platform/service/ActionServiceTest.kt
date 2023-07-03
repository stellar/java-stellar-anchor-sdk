package org.stellar.anchor.platform.service

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.verify
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.api.rpc.ActionMethod.NOTIFY_INTERACTIVE_FLOW_COMPLETED
import org.stellar.anchor.api.rpc.NotifyInteractiveFlowCompletedRequest
import org.stellar.anchor.api.rpc.RpcErrorCode
import org.stellar.anchor.api.rpc.RpcRequest
import org.stellar.anchor.platform.action.ActionHandler
import org.stellar.anchor.platform.utils.RpcUtil
import org.stellar.anchor.platform.utils.RpcUtil.JSON_RPC_VERSION

class ActionServiceTest {
  companion object {
    private const val RPC_ID = 1
    private const val RPC_PARAMS = "testParams"
    private const val ERROR_MSG = "Error message"
    private const val VALID_RPC_METHOD_1 = "notify_interactive_flow_completed"
    private const val VALID_RPC_METHOD_2 = "request_offchain_funds"
    private const val INVALID_RPC_METHOD = "ivalid_rpc_method"
  }

  @MockK(relaxed = true)
  private lateinit var actionHandler: ActionHandler<NotifyInteractiveFlowCompletedRequest>

  private lateinit var actionService: ActionService

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    every { actionHandler.actionType } returns NOTIFY_INTERACTIVE_FLOW_COMPLETED
    actionService = ActionService(listOf(actionHandler))
  }

  @Test
  fun `test handle list of valid rpc calls`() {
    val rpcRequest =
      RpcRequest.builder()
        .method(VALID_RPC_METHOD_1)
        .jsonrpc(JSON_RPC_VERSION)
        .id(RPC_ID)
        .params(RPC_PARAMS)
        .build()

    val response = actionService.handleRpcCalls(listOf(rpcRequest, rpcRequest))

    assertEquals(2, response.size)
    assertNull(response[0].error)
    assertNull(response[1].error)

    verify(exactly = 2) { actionHandler.handle(RPC_PARAMS) }
  }

  @Test
  fun `test handle invalid rpc protocol`() {
    val invalidRpcRequest = RpcRequest.builder().method(VALID_RPC_METHOD_1).id(RPC_ID).build()

    val response = actionService.handleRpcCalls(listOf(invalidRpcRequest))

    assertEquals(1, response.size)
    assertNull(response[0].error.data)
    assertEquals("Unsupported JSON-RPC protocol version [null]", response[0].error.message)
    assertEquals(-32600, response[0].error.code)

    verify(exactly = 0) { actionHandler.handle(any()) }
  }

  @Test
  fun `test handle invalid rpc method`() {
    val invalidRpcRequest =
      RpcRequest.builder()
        .method(INVALID_RPC_METHOD)
        .jsonrpc(JSON_RPC_VERSION)
        .id(RPC_ID)
        .params(RPC_PARAMS)
        .build()

    val response = actionService.handleRpcCalls(listOf(invalidRpcRequest))

    assertEquals(1, response.size)
    assertNull(response[0].error.data)
    assertEquals(
      String.format("No matching action method[%s]", INVALID_RPC_METHOD),
      response[0].error.message
    )
    assertEquals(-32601, response[0].error.code)

    verify(exactly = 0) { actionHandler.handle(any()) }
  }

  @Test
  fun `test handle rpc method without handler`() {
    val invalidRpcRequest =
      RpcRequest.builder()
        .method(VALID_RPC_METHOD_2)
        .jsonrpc(JSON_RPC_VERSION)
        .id(RPC_ID)
        .params(RPC_PARAMS)
        .build()

    val response = actionService.handleRpcCalls(listOf(invalidRpcRequest))

    assertEquals(1, response.size)
    assertNull(response[0].error.data)
    assertEquals(
      String.format("Action[%s] handler is not found", VALID_RPC_METHOD_2),
      response[0].error.message
    )
    assertEquals(-32603, response[0].error.code)

    verify(exactly = 0) { actionHandler.handle(any()) }
  }

  @Test
  fun `test handle list of valid and invalid rpc calls`() {
    val rpcRequest =
      RpcRequest.builder()
        .method(VALID_RPC_METHOD_1)
        .jsonrpc(JSON_RPC_VERSION)
        .id(RPC_ID)
        .params(RPC_PARAMS)
        .build()
    val invalidRpcRequest = RpcRequest.builder().method(VALID_RPC_METHOD_1).id(RPC_ID).build()

    val response = actionService.handleRpcCalls(listOf(rpcRequest, invalidRpcRequest))

    assertEquals(2, response.size)
    assertNull(response[0].error)
    assertNull(response[1].error.data)
    assertEquals("Unsupported JSON-RPC protocol version [null]", response[1].error.message)
    assertEquals(-32600, response[1].error.code)

    verify(exactly = 1) { actionHandler.handle(RPC_PARAMS) }
    verify(exactly = 1) { actionHandler.handle(any()) }
  }

  @Test
  fun `test handle internal exception`() {
    val rpcRequest = RpcRequest.builder().build()

    mockkStatic(RpcUtil::class)
    every { RpcUtil.validateRpcRequest(rpcRequest) } throws NullPointerException(ERROR_MSG)

    val response = actionService.handleRpcCalls(listOf(rpcRequest))

    assertEquals(1, response.size)
    assertNotNull(response[0].error)
    assertEquals(ERROR_MSG, response[0].error.message)
    assertEquals(RpcErrorCode.INTERNAL_ERROR.errorCode, response[0].error.code)

    verify(exactly = 0) { actionHandler.handle(any()) }
  }
}
