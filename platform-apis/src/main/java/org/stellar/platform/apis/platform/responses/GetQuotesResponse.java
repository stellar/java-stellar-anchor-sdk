package org.stellar.platform.apis.platform.responses;

import java.util.List;
import lombok.Data;
import org.stellar.platform.apis.shared.Quote;

@Data
public class GetQuotesResponse {
  List<Quote> records;
  String cursor;
}
