package org.stellar.anchor.api.platform;

import java.util.List;
import lombok.Data;

@Data
public class GetQuotesResponse {
  List<GetQuoteResponse> records;
  String cursor;
}
