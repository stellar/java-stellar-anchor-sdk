package org.stellar.anchor.platform.paymentobserver;

import com.google.gson.Gson;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import okhttp3.*;
import org.stellar.anchor.config.CirclePaymentObserverConfig;
import org.stellar.anchor.exception.ServerErrorException;
import org.stellar.anchor.paymentservice.circle.model.CirclePaymentStatus;
import org.stellar.anchor.paymentservice.circle.model.CircleTransactionParty;
import org.stellar.anchor.paymentservice.circle.model.CircleTransfer;
import org.stellar.anchor.paymentservice.circle.util.CircleAsset;
import org.stellar.anchor.platform.paymentobserver.circlemodels.CircleNotification;
import org.stellar.anchor.platform.paymentobserver.circlemodels.TransferNotificationBody;
import org.stellar.anchor.util.GsonUtils;
import org.stellar.anchor.util.Log;
import org.stellar.anchor.util.StellarNetworkHelper;
import org.stellar.sdk.Network;
import org.stellar.sdk.responses.GsonSingleton;
import org.stellar.sdk.responses.Page;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PathPaymentBaseOperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;
import shadow.com.google.common.reflect.TypeToken;

public class CirclePaymentObserverService {
  private final OkHttpClient httpClient;
  private final CirclePaymentObserverConfig circlePaymentObserverConfig;
  private final String usdcIssuer;

  private final Gson gson =
      GsonUtils.builder()
          .registerTypeAdapter(CircleTransfer.class, new CircleTransfer.Serialization())
          .setPrettyPrinting()
          .create();

  public CirclePaymentObserverService(
      OkHttpClient httpClient, CirclePaymentObserverConfig circlePaymentObserverConfig) {
    this.httpClient = httpClient;
    this.circlePaymentObserverConfig = circlePaymentObserverConfig;
    Network stellarNetwork =
        StellarNetworkHelper.toStellarNetwork(circlePaymentObserverConfig.getStellarNetwork());
    String[] assetIdPieces = CircleAsset.stellarUSDC(stellarNetwork).split(":");
    this.usdcIssuer = assetIdPieces[assetIdPieces.length - 1];
  }

  public void handleCircleNotification(Map<String, Object> requestBody) {
    CircleNotification circleNotification =
        gson.fromJson(gson.toJson(requestBody), CircleNotification.class);
    String type = Objects.toString(circleNotification.getType(), "");

    switch (type) {
      case "SubscriptionConfirmation":
        handleSubscriptionConfirmationNotification(circleNotification);
        return;

      case "Notification":
        handleTransferNotification(circleNotification);
        return;

      default:
        Log.warn("Not handling notification of unsupported type \"" + type + "\".");
    }
  }

  /**
   * This will auto-subscribe to Circle when we receive a subscription available notification.
   *
   * @param circleNotification is the circle notification object.
   */
  public void handleSubscriptionConfirmationNotification(CircleNotification circleNotification) {
    String subscribeUrl = circleNotification.getSubscribeUrl();
    Log.info("=====> subscriptionNotification.subscribeUrl: " + subscribeUrl);

    Request httpRequest =
        new Request.Builder()
            .url(subscribeUrl)
            .header("Content-Type", "application/json")
            .get()
            .build();
    Response response;
    try {
      response = httpClient.newCall(httpRequest).execute();
    } catch (IOException e) {
      Log.error("Failed to call endpoint " + subscribeUrl);
      return;
    }

    Log.info(
        String.format(
            "Called subscribeUrl %s and got status code %d", subscribeUrl, response.code()));
    if (response.code() != 200) {
      Log.error(String.format("Status code: %d. \nResponse: %s", response.code(), response.body()));
    }
  }

  /**
   * Handle incoming circle notifications of type "transfers". A transfer notification can contain
   * circle<>circle or circle<>stellar events.
   *
   * @param circleNotification is the circle notification object.
   */
  public void handleTransferNotification(CircleNotification circleNotification) {
    TransferNotificationBody transferNotification =
        gson.fromJson(circleNotification.getMessage(), TransferNotificationBody.class);

    CircleTransfer circleTransfer = transferNotification.getTransfer();
    if (circleTransfer == null) {
      Log.info("Missing \"transfer\" value in notification of type \"transfers\".");
      return;
    }

    if (!circleTransfer.getStatus().equals(CirclePaymentStatus.COMPLETE)) {
      Log.info("Incomplete transfer:\n" + gson.toJson(circleTransfer));
      return;
    }

    Log.info("Completed transfer:\n" + gson.toJson(circleTransfer));
    CircleTransactionParty source = circleTransfer.getSource();
    boolean isSourceOnStellar =
        source.getType().equals(CircleTransactionParty.Type.BLOCKCHAIN)
            && source.getChain().equals("XLM");
    CircleTransactionParty destination = circleTransfer.getDestination();
    boolean isDestinationOnStellar =
        destination.getType().equals(CircleTransactionParty.Type.BLOCKCHAIN)
            && destination.getChain().equals("XLM");

    if (!isSourceOnStellar && !isDestinationOnStellar) {
      Log.info(
          "Neither source nor destination are stellar accounts. Giving up on processing this transfer.");
      return;
    }

    if (!circleTransfer.getAmount().getCurrency().equals("USD")) {
      Log.info("The only circle currency supported is USDC.");
      return;
    }

    ObservedPayment observedPayment = null;
    try {
      observedPayment = fetchObservedPayment(circleTransfer);
    } catch (IOException | ServerErrorException ex) {
      ex.printStackTrace();
    }

    if (observedPayment == null) {
      Log.error("Observed payment could not be fetched");
      return;
    }

    // TODO: send event
  }

  /**
   * This will fetch the Stellar payment (or path payment) that originated thee Circle transfer.
   *
   * @param circleTransfer the Circle transfer
   * @return an ObservedPayment or null if unable to convert
   * @throws IOException
   * @throws ServerErrorException
   */
  public ObservedPayment fetchObservedPayment(CircleTransfer circleTransfer)
      throws IOException, ServerErrorException {
    String txHash = circleTransfer.getTransactionHash();
    HttpUrl url = HttpUrl.parse(circlePaymentObserverConfig.getStellarNetwork());
    url =
        new HttpUrl.Builder()
            .scheme(url.scheme())
            .host(url.host())
            .port(url.port())
            .addPathSegment("transactions")
            .addPathSegment(txHash)
            .addPathSegment("payments")
            .addQueryParameter("limit", "100")
            .build();
    Request httpRequest =
        new Request.Builder().url(url).header("Content-Type", "application/json").get().build();
    Response response = httpClient.newCall(httpRequest).execute();
    ResponseBody responseBody = response.body();
    if (responseBody == null) throw new ServerErrorException("unable to fetch response body");

    Type type = new TypeToken<Page<OperationResponse>>() {}.getType();
    shadow.com.google.gson.Gson stellarSdkGson = GsonSingleton.getInstance();
    Page<OperationResponse> responsePage = stellarSdkGson.fromJson(responseBody.string(), type);

    for (OperationResponse opResponse : responsePage.getRecords()) {
      if (!opResponse.isTransactionSuccessful()) continue;

      if (!List.of("payment", "path_payment_strict_send", "path_payment_strict_receive")
          .contains(opResponse.getType())) continue;

      ObservedPayment observedPayment = null;
      if (opResponse instanceof PaymentOperationResponse) {
        PaymentOperationResponse payment = (PaymentOperationResponse) opResponse;
        observedPayment = ObservedPayment.fromPaymentOperationResponse(payment);
      } else if (opResponse instanceof PathPaymentBaseOperationResponse) {
        PathPaymentBaseOperationResponse pathPayment =
            (PathPaymentBaseOperationResponse) opResponse;
        observedPayment = ObservedPayment.fromPathPaymentOperationResponse(pathPayment);
      } else {
        continue;
      }

      if (!observedPayment.assetCode.equals("USDC")
          || !observedPayment.assetIssuer.equals(usdcIssuer)) {
        continue;
      }

      BigDecimal transferAmount = new BigDecimal(circleTransfer.getAmount().getAmount());
      BigDecimal paymentAmount = new BigDecimal(observedPayment.amount);
      if (transferAmount.compareTo(paymentAmount) != 0) {
        continue;
      }

      return observedPayment;
    }
    return null;
  }
}
