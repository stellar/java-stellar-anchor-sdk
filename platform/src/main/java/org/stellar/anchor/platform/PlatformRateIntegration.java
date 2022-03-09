package org.stellar.anchor.platform;

import java.net.URI;
import java.net.URISyntaxException;
import okhttp3.*;
import org.stellar.anchor.exception.*;
import org.stellar.anchor.integration.rate.GetRateRequest;
import org.stellar.anchor.integration.rate.GetRateResponse;
import org.stellar.anchor.integration.rate.RateIntegration;

public class PlatformRateIntegration implements RateIntegration {
  private final String anchorEndpoint;
  private final OkHttpClient httpClient;

  public PlatformRateIntegration(String anchorEndpoint, OkHttpClient httpClient) {
    try {
      new URI(anchorEndpoint);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("invalid 'baseUri'");
    }

    this.anchorEndpoint = anchorEndpoint;
    this.httpClient = httpClient;
  }

  @Override
  public GetRateResponse getRate(GetRateRequest request) throws AnchorException {
    return null;
  }
}
