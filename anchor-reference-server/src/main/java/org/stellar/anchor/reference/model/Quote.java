package org.stellar.anchor.reference.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.Data;
import org.stellar.anchor.api.callback.GetRateRequest;
import org.stellar.anchor.api.sep.sep38.PriceDetail;

@Data
@Entity
public class Quote {
  @Id String id;

  String price;

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

  @Convert(converter = QuotePriceDetailConverter.class)
  List<PriceDetail> priceDetails;

  public static Quote of(GetRateRequest request, String price) {
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
    quote.setPrice(price);
    quote.setClientId(request.getClientId());

    return quote;
  }
}
