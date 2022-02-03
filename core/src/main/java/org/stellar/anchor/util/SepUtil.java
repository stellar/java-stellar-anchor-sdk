package org.stellar.anchor.util;

import org.stellar.sdk.xdr.MemoType;

public class SepUtil {
  public static String memoTypeString(MemoType memoType) {
    String result = "";
    switch (memoType) {
      case MEMO_ID:
        result = "id";
        break;
      case MEMO_HASH:
        result = "hash";
        break;
      case MEMO_TEXT:
        result = "text";
        break;
      case MEMO_NONE:
        result = "none";
        break;
      case MEMO_RETURN:
        result = "return";
        break;
    }

    return result;
  }
}
