package org.stellar.anchor.platform.service

import java.io.FileReader
import java.io.UncheckedIOException
import java.net.URL
import java.nio.file.Paths
import org.apache.commons.io.IOUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class SpringResourceReaderTest {

  @Test
  fun getListAllAssets() {
    val reader = SpringResourceReader()
    val result1 = reader.readResourceAsString("test_assets.json")
    val result2 = reader.readResourceAsString("classpath:/test_assets.json")
    assertEquals(result1, result2)

    val resource: URL = SpringResourceReader::class.java.getResource("/test_assets.json")
    var file = Paths.get(resource.toURI()).toFile()
    val result3 = IOUtils.toString(FileReader(file))
    assertEquals(result3, result2)
  }

  @Test
  fun testJsonNotFound() {
    val reader = SpringResourceReader()
    assertThrows<UncheckedIOException> { reader.readResourceAsString("file:test_assets.json") }
    assertThrows<UncheckedIOException> { reader.readResourceAsString("file:test_assets.json.bad") }
  }
}
