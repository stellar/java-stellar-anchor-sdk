package org.stellar.anchor.platform.util

import com.google.gson.annotations.SerializedName
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.api.exception.BadRequestException
import org.stellar.anchor.apiclient.TransactionsOrderBy
import org.stellar.anchor.platform.utils.StringEnumConverter
import org.stellar.anchor.platform.utils.StringEnumConverter.TransactionsOrderByConverter

class EnumConverterTest {
  @Test
  fun testConvert() {
    val converter = TransactionsOrderByConverter()
    assertEquals(TransactionsOrderBy.CREATED_AT, converter.convert("CREATED_AT"))
    assertEquals(TransactionsOrderBy.CREATED_AT, converter.convert("created_at"))
    assertEquals(TransactionsOrderBy.CREATED_AT, converter.convert("cReAtEd_at"))
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
