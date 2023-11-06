package org.stellar.anchor.api.sep;

import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class CustomerInfoNeededResponse {
  private final String type = "non_interactive_customer_info_needed";
  private final List<String> fields;
}
