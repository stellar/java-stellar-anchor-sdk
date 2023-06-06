package org.stellar.anchor.api.sep.sep24;

import lombok.AllArgsConstructor;
import lombok.Data;

/** The response to the GET /fee endpoint of SEP-24. */
@AllArgsConstructor
@Data
public class Sep24GetFeeResponse {
  String fee;
}
