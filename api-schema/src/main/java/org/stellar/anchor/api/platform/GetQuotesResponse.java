package org.stellar.anchor.api.platform;

import java.util.List;
import lombok.Data;
import org.stellar.anchor.api.shared.Quote;

@Data
public class GetQuotesResponse {
  List<Quote> records;
  String cursor;
}
