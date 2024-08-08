package org.stellar.anchor.api.rpc.method;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Sort;
import org.stellar.anchor.api.platform.TransactionsOrderBy;
import org.stellar.anchor.api.platform.TransactionsSeps;
import org.stellar.anchor.api.sep.SepTransactionStatus;

@Data
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class GetTransactionsRpcRequest extends RpcMethodParamsRequest {

  @NotNull private TransactionsSeps sep;

  @SerializedName("order_by")
  private TransactionsOrderBy orderBy;

  private Sort.Direction order;

  private List<SepTransactionStatus> statuses;

  @SerializedName("page_number")
  private Integer pageNumber;

  @SerializedName("page_size")
  private Integer pageSize;
}
