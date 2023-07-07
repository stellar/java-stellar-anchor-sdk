package org.stellar.anchor.platform.test

import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpStatus
import org.junit.jupiter.api.Assertions.*
import org.stellar.anchor.api.rpc.RpcErrorCode.INVALID_REQUEST
import org.stellar.anchor.api.rpc.RpcRequest
import org.stellar.anchor.api.rpc.RpcResponse
import org.stellar.anchor.api.rpc.action.ActionMethod.NOTIFY_TRANSACTION_ERROR
import org.stellar.anchor.api.rpc.action.NotifyTransactionErrorRequest
import org.stellar.anchor.apiclient.PlatformApiClient
import org.stellar.anchor.auth.AuthHelper
import org.stellar.anchor.platform.TestConfig
import org.stellar.anchor.platform.utils.RpcUtil.JSON_RPC_VERSION
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.Sep1Helper.TomlContent

// TODO: Update tests
class PlatformApiTests(config: TestConfig, toml: TomlContent, jwt: String) {
  companion object {
    private const val TX_ID = "testTxId"
    private const val RPC_ID_1 = 1
    private const val RPC_ID_2 = 2
  }

  private val type: Type = object : TypeToken<ArrayList<RpcResponse>>() {}.type
  private val gson = GsonUtils.getInstance()
  private val rpcMethod = NOTIFY_TRANSACTION_ERROR.toString()

  private val platformApiClient =
    PlatformApiClient(AuthHelper.forNone(), config.env["platform.server.url"]!!)

  fun testAll() {
    println("Performing Platform API tests...")
    `send rpc action`()
    `send batch of rpc actions`()
    `send batch of invalid rpc actions`()
  }

  private fun `send rpc action`() {
    val request =
      RpcRequest.builder()
        .id(RPC_ID_1)
        .jsonrpc(JSON_RPC_VERSION)
        .method(rpcMethod)
        .params(NotifyTransactionErrorRequest.builder().transactionId(TX_ID).build())
        .build()
    val response = platformApiClient.rpcAction(listOf(request))
    assertEquals(HttpStatus.SC_BAD_REQUEST, response.code)
    val responses = gson.fromJson<List<RpcResponse>>(response.body?.string(), type)
    assertEquals(HttpStatus.SC_BAD_REQUEST, response.code)
    assertEquals(1, responses.size)
    responses.forEach {
      assertNull(it.result)
      assertNotNull(it.error)
      assertEquals(request.jsonrpc, it.jsonrpc)
      assertNotNull(it.error.message)
      assertEquals(INVALID_REQUEST.errorCode, it.error.code)
    }
  }

  private fun `send batch of rpc actions`() {
    val request1 =
      RpcRequest.builder()
        .id(RPC_ID_1)
        .jsonrpc(JSON_RPC_VERSION)
        .method(rpcMethod)
        .params(NotifyTransactionErrorRequest.builder().transactionId(TX_ID).build())
        .build()
    val request2 =
      RpcRequest.builder()
        .id(RPC_ID_2)
        .jsonrpc(JSON_RPC_VERSION)
        .method(rpcMethod)
        .params(NotifyTransactionErrorRequest.builder().transactionId(TX_ID).build())
        .build()
    val response = platformApiClient.rpcAction(listOf(request1, request2))
    assertEquals(HttpStatus.SC_BAD_REQUEST, response.code)
    val responses = gson.fromJson<List<RpcResponse>>(response.body?.string(), type)
    assertEquals(2, responses.size)
    responses.forEach {
      assertNull(it.result)
      assertNotNull(it.error)
      assertEquals(request1.jsonrpc, it.jsonrpc)
      assertNotNull(it.error.message)
      assertEquals(INVALID_REQUEST.errorCode, it.error.code)
    }
  }

  private fun `send batch of invalid rpc actions`() {
    val request1 = RpcRequest.builder().id(RPC_ID_1).method(rpcMethod).build()
    val request2 =
      RpcRequest.builder().id(RPC_ID_2).jsonrpc(JSON_RPC_VERSION).method(StringUtils.EMPTY).build()
    val request3 =
      RpcRequest.builder().id(true).jsonrpc(JSON_RPC_VERSION).method(rpcMethod).params("{}").build()
    val response = platformApiClient.rpcAction(listOf(request1, request2, request3))
    assertEquals(HttpStatus.SC_BAD_REQUEST, response.code)
    val responses = gson.fromJson<List<RpcResponse>>(response.body?.string(), type)
    assertEquals(3, responses.size)
    responses.forEach {
      assertNull(it.result)
      assertNotNull(it.error)
      assertNotNull(it.error.message)
      assertEquals(INVALID_REQUEST.errorCode, it.error.code)
    }
  }
}
