package org.stellar.anchor.util;

import static org.stellar.anchor.api.sep.SepTransactionStatus.*;
import static org.stellar.anchor.util.MathHelper.decimal;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.SepValidationException;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.config.AppConfig;
import org.stellar.sdk.xdr.MemoType;

public class SepHelper {
  /**
   * Generates an Id for SEP transactions.
   *
   * @return An Id in UUID format
   */
  public static String generateSepTransactionId() {
    return UUID.randomUUID().toString();
  }

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

  public static boolean amountEquals(String amount1, String amount2) {
    return decimal(amount1).compareTo(decimal(amount2)) == 0;
  }

  public static void validateAmount(String amount) throws AnchorException {
    validateAmount("", amount);
  }

  public static void validateAmount(String messagePrefix, String amount) throws AnchorException {
    // assetName
    if (Objects.toString(amount, "").isEmpty()) {
      throw new BadRequestException(messagePrefix + "amount cannot be empty");
    }

    BigDecimal sAmount;
    try {
      sAmount = decimal(amount);
    } catch (NumberFormatException e) {
      throw new BadRequestException(messagePrefix + "amount is invalid", e);
    }
    if (sAmount.signum() < 1) {
      throw new BadRequestException(messagePrefix + "amount should be positive");
    }
  }

  public static String validateLanguage(AppConfig appConfig, String lang)
      throws SepValidationException {
    if (lang != null) {
      List<String> languages = appConfig.getLanguages();
      if (languages != null && languages.size() > 0) {
        if (languages.stream().noneMatch(l -> l.equalsIgnoreCase(lang))) {
          throw new SepValidationException(String.format("unsupported language: %s", lang));
        }
      }
      return lang;
    } else {
      return "en-US";
    }
  }

  /**
   * Checks if the status is valid in a SEP.
   *
   * @param sep The sep number.
   * @param status The name (String) of the status to be checked.
   * @return true, if valid. Otherwise false
   */
  public static boolean validateTransactionStatus(String status, int sep) {
    for (SepTransactionStatus transactionStatus : values()) {
      if (transactionStatus.getName().equals(status)) {
        return validateTransactionStatus(transactionStatus, sep);
      }
    }

    return false;
  }

  /**
   * Checks if the status is valid in a SEP.
   *
   * @param sep The sep number.
   * @param status The status to be checked.
   * @return true, if valid. Otherwise false
   */
  public static boolean validateTransactionStatus(SepTransactionStatus status, int sep) {
    switch (sep) {
      case 24:
        return (sep24Statuses.contains(status));
      case 31:
        return (sep31Statuses.contains(status));
      default:
        return false;
    }
  }

  static List<SepTransactionStatus> sep24Statuses =
      List.of(
          INCOMPLETE,
          PENDING_USR_TRANSFER_START,
          PENDING_USR_TRANSFER_COMPLETE,
          PENDING_EXTERNAL,
          PENDING_ANCHOR,
          PENDING_STELLAR,
          PENDING_TRUST,
          PENDING_USER,
          COMPLETED,
          NO_MARKET,
          TOO_SMALL,
          TOO_LARGE,
          ERROR);

  static List<SepTransactionStatus> sep31Statuses =
      List.of(
          PENDING_SENDER,
          PENDING_STELLAR,
          PENDING_CUSTOMER_INFO_UPDATE,
          PENDING_TRANSACTION_INFO_UPDATE,
          PENDING_RECEIVER,
          PENDING_EXTERNAL,
          COMPLETED,
          ERROR);
}
