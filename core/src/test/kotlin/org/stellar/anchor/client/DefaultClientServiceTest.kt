package org.stellar.anchor.client

import com.google.gson.JsonSyntaxException
import org.apache.commons.io.FilenameUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.asset.DefaultAssetService
import org.stellar.anchor.client.DefaultClientService.gson

class DefaultClientServiceTest {
  @BeforeEach fun setup() {}

  @ParameterizedTest
  @ValueSource(strings = ["test_clients.yaml", "test_clients.json"])
  fun `test file format clients config loading and listing`(filename: String) {
    lateinit var dcs: DefaultClientService
    when (FilenameUtils.getExtension(filename)) {
      "yaml" -> dcs = DefaultClientService.fromYamlResourceFile(filename)
      "json" -> dcs = DefaultClientService.fromJsonResourceFile(filename)
    }

    // check listing function.
    val clients = dcs.getAllClients()
    Assertions.assertEquals(3, clients.size)
    JSONAssert.assertEquals(expectedAllClientsJson, gson.toJson(clients), JSONCompareMode.LENIENT)
  }

  @Test
  fun `test invalid file format`() {
    assertThrows<JsonSyntaxException> {
      DefaultAssetService.fromJsonResource("test_clients_file_format_not_valid.yaml.bad")
    }
  }

  private val expectedAllClientsJson =
    """
      [
        {
          "name": "referenceCustodial",
          "signing_keys": ["GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"]
        },
        {
          "name": "stellar_anchor_tests",
          "signing_keys": ["GDOHXZYP5ABGCTKAEROOJFN6X5GY7VQNXFNK2SHSAD32GSVMUJBPG75E"]
        },
        {
          "name": "reference",
          "domains": ["wallet-server:8092"],
          "callback_url": "http://wallet-server:8092/callbacks"
        }
      ]
    """
      .trimIndent()
}
