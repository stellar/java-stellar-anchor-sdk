package org.stellar.anchor.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BeanHelperTest {
  companion object {
    private val gson = GsonUtils.getInstance()
  }

  @Test
  fun `test updateField`() {
    val patch = gson.fromJson(jsonPatch, PatchData::class.java)
    val txn = gson.fromJson(jsonData, Data::class.java)

    patch.simpleField = "updated"
    Assertions.assertTrue(BeanHelper.updateField(patch, txn, "simpleField", false))
    Assertions.assertEquals(txn.simpleField, patch.simpleField)

    patch.complexField.a = "200"
    Assertions.assertTrue(
      BeanHelper.updateField(patch, "complexField.a", txn, "simpleField", false)
    )
    Assertions.assertEquals(txn.simpleField, patch.complexField.a)
  }
}

data class PatchData(var simpleField: String, var complexField: ComplexPatchData)

data class ComplexPatchData(var a: String)

data class Data(var simpleField: String, var complexField: ComplexField)

data class ComplexField(var a: String, var b: Int)

val jsonPatch =
  """
    {
      "simpleField": "simple",
      "complexField": {
         "a": "a"
      }
    }
  """
    .trimIndent()

val jsonData =
  """
    {
      "simpleField": "simple",
      "complexField": {
         "a": "a",
         "b": 1
      }
    }
  """
    .trimIndent()
