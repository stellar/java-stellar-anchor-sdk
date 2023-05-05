package org.stellar.anchor.platform.util

import com.google.gson.annotations.SerializedName
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.api.exception.BadRequestException
import org.stellar.anchor.api.platform.TransactionsOrderBy
import org.stellar.anchor.platform.utils.StringEnumConverter
import org.stellar.anchor.platform.utils.StringEnumConverter.TransactionsOrderByConverter

class EnumConverterTest {
  @Test
  fun testConvert() {
    val converter = TransactionsOrderByConverter()
    assertEquals(TransactionsOrderBy.STARTED_AT, converter.convert("STARTED_AT"))
    assertEquals(TransactionsOrderBy.STARTED_AT, converter.convert("started_at"))
    assertEquals(TransactionsOrderBy.STARTED_AT, converter.convert("stArTed_at"))
    assertThrows<BadRequestException> { converter.convert("not_enum") }
  }

  @Test
  fun testConverterSerialized() {
    val converter = TestEnumMismatchNameConverter()
    assertEquals(TestEnumMismatchName.FOO, converter.convert("bar"))
  }
}

enum class TestEnumMismatchName() {
  @SerializedName("bar") FOO
}

class TestEnumMismatchNameConverter :
  StringEnumConverter<TestEnumMismatchName>(TestEnumMismatchName::class.java) {}
