package org.stellar.anchor.api.platform;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.stellar.anchor.api.sep.SepTransactionStatus;

/**
 * The request body of the GET /transactions endpoint of the Platform API.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-docs/blob/main/openapi/anchor-platform/Platform%20API.yml">Platform
 *     API</a>
 */
@Data
@Builder
public class GetTransactionsRequest {
  @NonNull private TransactionsSeps sep;

  @SerializedName("order_by")
  private TransactionsOrderBy orderBy;

  private TransactionsOrder order;

  private List<SepTransactionStatus> statuses;

  @SerializedName("page_size")
  private Integer pageSize;

  @SerializedName("page_number")
  private Integer pageNumber;
}
