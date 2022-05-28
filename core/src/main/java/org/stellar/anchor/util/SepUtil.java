package org.stellar.anchor.util;

import static org.stellar.sdk.xdr.MemoType.*;
import static org.stellar.sdk.xdr.MemoType.MEMO_NONE;

import org.stellar.anchor.exception.SepException;
import org.stellar.sdk.*;
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

  public static MemoType memoType(Memo memo) throws SepException {
    if (memo instanceof MemoId) {
      return MEMO_ID;
    } else if (memo instanceof MemoHash) {
      return MEMO_HASH;
    } else if (memo instanceof MemoText) {
      return MEMO_TEXT;
    } else if (memo instanceof MemoNone) {
      return MEMO_NONE;
    }
    throw new SepException("Unsupported memo type: " + memo.getClass());
  }
}
