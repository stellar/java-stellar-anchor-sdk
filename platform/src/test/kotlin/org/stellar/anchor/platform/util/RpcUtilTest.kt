package org.stellar.anchor.platform.util

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.stellar.anchor.api.exception.BadRequestException
import org.stellar.anchor.api.exception.rpc.InternalErrorException
import org.stellar.anchor.api.exception.rpc.InvalidParamsException
import org.stellar.anchor.api.exception.rpc.InvalidRequestException
import org.stellar.anchor.api.exception.rpc.MethodNotFoundException
import org.stellar.anchor.api.rpc.RpcErrorCode
import org.stellar.anchor.api.rpc.RpcRequest
import org.stellar.anchor.api.rpc.RpcResponse
import org.stellar.anchor.api.rpc.method.RpcMethodParamsRequest
import org.stellar.anchor.platform.utils.RpcUtil
import org.stellar.anchor.platform.utils.RpcUtil.JSON_RPC_VERSION

class RpcUtilTest {

  companion object {
    private const val RPC_ID = 1
    private const val TXN_ID = "123454321"
    private const val RPC_RESULT = 2L
    private const val ERROR_MSG = "Error message"
    private const val RPC_METHOD = "test_rpc_method"
    private val rpcRequest =
      RpcRequest.builder()
        .id(RPC_ID)
        .params(RpcMethodParamsRequest.builder().transactionId(TXN_ID).build())
        .build()
  }

  @Test
  fun `test get rpc success response`() {
    val response = RpcUtil.getRpcSuccessResponse(RPC_ID, RPC_RESULT)
    assertNotNull(response)
    assertNull(response.error)
    assertEquals(RPC_ID, response.id)
    assertEquals(RPC_RESULT, response.result)
    assertEquals(JSON_RPC_VERSION, response.jsonrpc)
  }

  @Test
  fun `test get rpc invalid request response`() {
    val response = RpcUtil.getRpcErrorResponse(rpcRequest, InvalidRequestException(ERROR_MSG))
    verifyErrorResponse(response)
    assertEquals(ERROR_MSG, response.error.message)
    assertEquals(RpcErrorCode.INVALID_REQUEST.errorCode, response.error.code)
  }

  @Test
  fun `test get rpc internal error response`() {
    val response = RpcUtil.getRpcErrorResponse(rpcRequest, InternalErrorException(ERROR_MSG))
    verifyErrorResponse(response)
    assertEquals(ERROR_MSG, response.error.message)
    assertEquals(TXN_ID, response.error.id)
    assertEquals(RpcErrorCode.INTERNAL_ERROR.errorCode, response.error.code)
  }

  @Test
  fun `test get rpc method not found response`() {
    val response = RpcUtil.getRpcErrorResponse(rpcRequest, MethodNotFoundException(ERROR_MSG))
    verifyErrorResponse(response)
    assertEquals(ERROR_MSG, response.error.message)
    assertEquals(TXN_ID, response.error.id)
    assertEquals(RpcErrorCode.METHOD_NOT_FOUND.errorCode, response.error.code)
  }

  @Test
  fun `test get rpc invalid params response`() {
    val response = RpcUtil.getRpcErrorResponse(rpcRequest, InvalidParamsException(ERROR_MSG))
    verifyErrorResponse(response)
    assertEquals(ERROR_MSG, response.error.message)
    assertEquals(TXN_ID, response.error.id)
    assertEquals(RpcErrorCode.INVALID_PARAMS.errorCode, response.error.code)
  }

  @Test
  fun `test get rpc bad request response`() {
    val response = RpcUtil.getRpcErrorResponse(rpcRequest, BadRequestException(ERROR_MSG))
    verifyErrorResponse(response)
    assertEquals(ERROR_MSG, response.error.message)
    assertEquals(TXN_ID, response.error.id)
    assertEquals(RpcErrorCode.INVALID_PARAMS.errorCode, response.error.code)
  }

  @ParameterizedTest
  @ValueSource(strings = ["", "1.0", "2", "2.", "2.1"])
  fun `test validate unsupported JSON-RPC protocol`(protocolVersion: String) {
    val rpcRequest =
      RpcRequest.builder().id(RPC_ID).jsonrpc(protocolVersion).method(RPC_METHOD).build()
    val exception = assertThrows<InvalidRequestException> { RpcUtil.validateRpcRequest(rpcRequest) }
    assertEquals(
      java.lang.String.format("Unsupported JSON-RPC protocol version[%s]", protocolVersion),
      exception.message
    )
  }

  @Test
  fun `test validate NULL method name`() {
    val rpcRequest = RpcRequest.builder().id(RPC_ID).jsonrpc(JSON_RPC_VERSION).build()
    val exception = assertThrows<InvalidRequestException> { RpcUtil.validateRpcRequest(rpcRequest) }
    assertEquals("Method name can't be NULL or empty", exception.message)
  }

  @Test
  fun `test validate NULL id`() {
    val rpcRequest = RpcRequest.builder().jsonrpc(JSON_RPC_VERSION).method(RPC_METHOD).build()
    val exception = assertThrows<InvalidRequestException> { RpcUtil.validateRpcRequest(rpcRequest) }
    assertEquals("Id can't be NULL", exception.message)
  }

  @Test
  fun `test validate empty method name`() {
    val rpcRequest =
      RpcRequest.builder().id(RPC_ID).jsonrpc(JSON_RPC_VERSION).method(StringUtils.EMPTY).build()
    val exception = assertThrows<InvalidRequestException> { RpcUtil.validateRpcRequest(rpcRequest) }
    assertEquals("Method name can't be NULL or empty", exception.message)
  }

  @Test
  fun `test validate invalid id type`() {
    val rpcRequest =
      RpcRequest.builder().jsonrpc(JSON_RPC_VERSION).method(RPC_METHOD).id(true).build()
    val exception = assertThrows<InvalidRequestException> { RpcUtil.validateRpcRequest(rpcRequest) }
    assertEquals("An identifier MUST contain a String or a Number", exception.message)
  }

  private fun verifyErrorResponse(response: RpcResponse) {
    assertNotNull(response)
    assertNull(response.result)
    assertNotNull(response.error)
    assertEquals(RPC_ID, response.id)
    assertEquals(JSON_RPC_VERSION, response.jsonrpc)
  }
}
