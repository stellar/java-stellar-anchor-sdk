package org.stellar.anchor.reference.service;

import java.util.List;
import java.util.Map;
import kotlin.Pair;
import org.springframework.stereotype.Service;
import org.stellar.anchor.exception.*;
import org.stellar.platform.apis.callbacks.requests.GetRateRequest;
import org.stellar.platform.apis.callbacks.responses.GetRateResponse;

@Service
public class RateService {

  public GetRateResponse getRate(GetRateRequest request) throws AnchorException {
    if (request.getId() != null) {
      throw new ServerErrorException("getting quote by id is not implemented yet");
    }

    if (request.getType() == null) {
      throw new BadRequestException("type cannot be empty");
    }

    if (!List.of("firm", "indicative").contains(request.getType())) {
      throw new BadRequestException("type is not supported");
    }

    if (request.getSellAsset() == null) {
      throw new BadRequestException("sell_asset cannot be empty");
    }
    if (request.getBuyAsset() == null) {
      throw new BadRequestException("buy_asset cannot be empty");
    }

    String price = ConversionPrice.getPrice(request.getSellAsset(), request.getBuyAsset());
    if (price == null) {
      throw new UnprocessableEntityException("the price for the given pair could not be found");
    }

    return new GetRateResponse(price);
  }

  private static class ConversionPrice {
    private static final String fiatUSD = "iso4217:USD";
    private static final String stellarUSDC =
        "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5";
    private static final String stellarJPYC =
        "stellar:JPYC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5";
    private static final Map<Pair<String, String>, String> hardcodedPrices =
        Map.of(
            new Pair<>(fiatUSD, stellarUSDC), "1.02",
            new Pair<>(stellarUSDC, fiatUSD), "1.05",
            new Pair<>(fiatUSD, stellarJPYC), "0.0083333",
            new Pair<>(stellarJPYC, fiatUSD), "122",
            new Pair<>(stellarUSDC, stellarJPYC), "0.0084",
            new Pair<>(stellarJPYC, stellarUSDC), "120");

    public static String getPrice(String sellAsset, String buyAsset) {
      return hardcodedPrices.get(new Pair<>(sellAsset, buyAsset));
    }
  }
}
