package org.stellar.anchor.reference.model;

import java.time.Instant;
import java.util.UUID;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.Data;
import org.stellar.anchor.api.callback.GetRateRequest;
import org.stellar.anchor.api.callback.GetRateResponse;
import org.stellar.anchor.api.sep.sep38.RateFee;

@Data
@Entity
public class Quote {
  @Id String id;

  String price;

  String totalPrice;

  Instant expiresAt;

  Instant createdAt;

  String sellAsset;

  String sellAmount;

  String sellDeliveryMethod;

  String buyAsset;

  String buyAmount;

  String buyDeliveryMethod;

  String countryCode;

  // used to store the stellar account
  String clientId;

  String transactionId;

  @Convert(converter = RateFeeConverter.class)
  RateFee fee;

  public static Quote of(GetRateRequest request) {
    Quote quote = new Quote();
    quote.setId(UUID.randomUUID().toString());
    quote.setSellAsset(request.getSellAsset());
    quote.setSellAmount(request.getSellAmount());
    quote.setSellDeliveryMethod(request.getSellDeliveryMethod());
    quote.setBuyAsset(request.getBuyAsset());
    quote.setBuyAmount(request.getBuyAmount());
    quote.setSellDeliveryMethod(request.getSellDeliveryMethod());
    quote.setCountryCode(request.getCountryCode());
    quote.setCreatedAt(Instant.now());
    quote.setClientId(request.getClientId());

    return quote;
  }

  public GetRateResponse toGetRateResponse() {
    GetRateResponse.Rate rate =
        GetRateResponse.Rate.builder()
            .id(getId())
            .price(getPrice())
            .sellAmount(getSellAmount())
            .buyAmount(getBuyAmount())
            .expiresAt(getExpiresAt())
            .fee(getFee())
            .build();

    return new GetRateResponse(rate);
  }
}
