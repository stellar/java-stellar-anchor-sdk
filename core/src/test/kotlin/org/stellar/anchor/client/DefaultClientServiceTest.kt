package org.stellar.anchor.client

import org.apache.commons.io.FilenameUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class DefaultClientServiceTest {
  @BeforeEach fun setup() {}

  @ParameterizedTest
  @ValueSource(strings = ["test_clients.yaml", "test_clients.json"])
  fun `test clients loading and listing`(filename: String) {
    lateinit var dcs: DefaultClientService
    when (FilenameUtils.getExtension(filename)) {
      "yaml" -> dcs = DefaultClientService.fromYamlResourceFile(filename)
      "json" -> dcs = DefaultClientService.fromJsonResourceFile(filename)
    }

    // check listing function.
    val clients = dcs.listAllClients()
    Assertions.assertEquals(3, clients.size)
  }
}
