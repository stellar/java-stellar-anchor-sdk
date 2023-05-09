package org.stellar.anchor.util;

import java.util.List;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Sort;
import org.stellar.anchor.api.platform.TransactionsOrderBy;
import org.stellar.anchor.api.sep.SepTransactionStatus;

@Data
@AllArgsConstructor
public class TransactionsParams {
  TransactionsOrderBy order_by;
  Sort.Direction order;
  @Nullable List<SepTransactionStatus> statuses;
  Integer pageNumber;
  Integer pageSize;
}
