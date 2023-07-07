package org.stellar.anchor.platform.service

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockkStatic
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.api.rpc.RpcErrorCode
import org.stellar.anchor.api.rpc.RpcRequest
import org.stellar.anchor.platform.utils.RpcUtil
import org.stellar.anchor.platform.utils.RpcUtil.JSON_RPC_VERSION

class ActionServiceTest {
  companion object {
    private const val RPC_ID = "1"
    private const val ERROR_MSG = "Error message"
    private const val RPC_METHOD = "test_rpc_method"
  }

  private lateinit var actionService: ActionService

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    actionService = ActionService()
  }

  @Test
  fun `test handle list of valid rpc calls`() {
    val rpcRequest =
      RpcRequest.builder().method(RPC_METHOD).jsonrpc(JSON_RPC_VERSION).id(RPC_ID).build()
    val response = actionService.handleRpcCalls(listOf(rpcRequest, rpcRequest))
    assertEquals(2, response.size)
    assertNull(response[0].error)
    assertNull(response[1].error)
  }

  @Test
  fun `test handle invalid rpc call`() {
    val invalidRpcRequest = RpcRequest.builder().method(RPC_METHOD).id(RPC_ID).build()
    val response = actionService.handleRpcCalls(listOf(invalidRpcRequest))
    assertEquals(1, response.size)
    assertNotNull(response[0].error)
  }

  @Test
  fun `test handle list of valid and invalid rpc calls`() {
    val rpcRequest =
      RpcRequest.builder().method(RPC_METHOD).jsonrpc(JSON_RPC_VERSION).id(RPC_ID).build()
    val invalidRpcRequest = RpcRequest.builder().method(RPC_METHOD).id(RPC_ID).build()
    val response = actionService.handleRpcCalls(listOf(rpcRequest, invalidRpcRequest))
    assertEquals(2, response.size)
    assertNull(response[0].error)
    assertNotNull(response[1].error)
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
  }
}
