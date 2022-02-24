package org.stellar.platform.callbacks.responses;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class GetRateResponse {
  LocalDateTime expiresAt;
  String price;
}
