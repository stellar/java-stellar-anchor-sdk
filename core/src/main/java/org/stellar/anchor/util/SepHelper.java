package org.stellar.anchor.util;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.exception.AnchorException;
import org.stellar.anchor.exception.BadRequestException;
import org.stellar.anchor.exception.SepValidationException;
import org.stellar.sdk.xdr.MemoType;

public class SepHelper {
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

  public static void validateAmount(String messagePrefix, String amount) throws AnchorException {
    // assetName
    if (Objects.toString(amount, "").isEmpty()) {
      throw new BadRequestException(messagePrefix + "amount cannot be empty");
    }

    BigDecimal sAmount;
    try {
      sAmount = new BigDecimal(amount);
    } catch (NumberFormatException e) {
      throw new BadRequestException(messagePrefix + "amount is invalid", e);
    }
    if (sAmount.signum() < 1) {
      throw new BadRequestException(messagePrefix + "amount should be positive");
    }
  }

  public static void validateLanguage(AppConfig appConfig, String lang)
      throws SepValidationException {
    if (lang != null) {
      List<String> languages = appConfig.getLanguages();
      if (languages != null && languages.size() > 0) {
        if (languages.stream().noneMatch(l -> l.equalsIgnoreCase(lang))) {
          throw new SepValidationException(String.format("unsupported language: %s", lang));
        }
      }
      // TODO: Implement later
      // activateLanguage();
    }
  }

  public static String generateTransactionId() {
    return UUID.randomUUID().toString();
  }
}
