package org.stellar.anchor.api.sep.sep6;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.stellar.anchor.api.sep.AssetInfo;

/**
 * The response to the GET /info endpoint of SEP-6.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0006.md#response-4">GET
 *     /info response</a>
 */
@Data
@Builder
public class InfoResponse {
  /**
   * All assets enabled for deposit. The key is the asset code and the value is the deposit
   * configuration.
   */
  Map<String, DepositAssetResponse> deposit;

  /**
   * All assets enabled for deposit via an exchange. The key is the asset code and the value is the
   * deposit configuration.
   */
  @SerializedName("deposit-exchange")
  Map<String, DepositAssetResponse> depositExchange;

  /**
   * All assets enabled for withdrawal. The key is the asset code and the value is the withdrawal
   * configuration.
   */
  Map<String, WithdrawAssetResponse> withdraw;

  /**
   * All assets enabled for withdrawal via an exchange. The key is the asset code and the value is
   * the withdrawal configuration.
   */
  @SerializedName("withdraw-exchange")
  Map<String, WithdrawAssetResponse> withdrawExchange;

  /** Fee endpoint configuration. */
  FeeResponse fee;

  /** Transactions endpoint configuration. */
  TransactionsResponse transactions;

  /** Transaction endpoint configuration. */
  TransactionResponse transaction;

  /** Feature flags. */
  FeaturesResponse features;

  /** Deposit configuration. */
  @Data
  @Builder
  public static class DepositAssetResponse {
    /** If true, the anchor allows the client to initiate a deposit. */
    Boolean enabled;

    /**
     * If true, the anchor requires the client to be authenticated before initiating a deposit.
     *
     * <p>This will always be set to true by the platform.
     */
    @SerializedName("authentication_required")
    Boolean authenticationRequired;

    /**
     * The fields required to initiate a deposit.
     *
     * <p>The only field supported by the platform is <code>type</code>. Additional fields required
     * for KYC are supplied asynchronously through SEP-12 requests.
     */
    Map<String, AssetInfo.Field> fields;
  }

  /** Withdrawal configuration. */
  @Data
  @Builder
  public static class WithdrawAssetResponse {
    /** If true, the anchor allows the client to initiate a withdrawal. */
    Boolean enabled;

    /**
     * If true, the anchor requires the client to be authenticated before initiating a withdrawal.
     *
     * <p>This will always be set to true by the platform.
     */
    @SerializedName("authentication_required")
    Boolean authenticationRequired;

    /**
     * The types of withdrawal methods supported and their fields.
     *
     * <p>The platform does not allow fields to be configured for withdrawal methods. Financial
     * account and KYC information is supplied asynchronously through PATCH requests and SEP-12
     * requests respectively.
     */
    Map<String, Map<String, AssetInfo.Field>> types;
  }

  /** Fee endpoint configuration */
  @Data
  @Builder
  public static class FeeResponse {
    /** Always set to false by the platform. This will eventually be deprecated in SEP. */
    Boolean enabled;

    /** Description of why the fee endpoint is not supported. */
    String description;
  }

  /** Transactions endpoint configuration */
  @Data
  @Builder
  public static class TransactionsResponse {
    /** If true, the anchor allows clients to request transaction details. */
    Boolean enabled;

    /**
     * If true, the anchor requires the client to be authenticated before making a request.
     *
     * <p>This will always be set to true by the platform.
     */
    @SerializedName("authentication_required")
    Boolean authenticationRequired;
  }

  /** Transaction endpoint configuration */
  @Data
  @Builder
  public static class TransactionResponse {
    /** If true, the anchor allows clients to request transaction details. */
    Boolean enabled;

    /**
     * If true, the anchor requires the client to be authenticated before making a request.
     *
     * <p>This will always be set to true by the platform.
     */
    @SerializedName("authentication_required")
    Boolean authenticationRequired;
  }

  /** Feature flags. */
  @Data
  @Builder
  public static class FeaturesResponse {
    /**
     * If true, when the Stellar account does not exist, on receipt of the deposit, the anchor will
     * create a new account with enough XLM for the minimum reserve and a trustline for the
     * requested asset.
     */
    @SerializedName("account_creation")
    Boolean accountCreation;

    /** If true, the anchor supports claimable balances. */
    @SerializedName("claimable_balances")
    Boolean claimableBalances;
  }
}
