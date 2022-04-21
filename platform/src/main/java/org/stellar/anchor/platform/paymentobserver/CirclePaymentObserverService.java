package org.stellar.anchor.platform.paymentobserver;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.stellar.anchor.config.CirclePaymentObserverConfig;
import org.stellar.anchor.paymentservice.circle.model.CirclePaymentStatus;
import org.stellar.anchor.paymentservice.circle.model.CircleTransfer;
import org.stellar.anchor.platform.paymentobserver.circlemodels.CircleNotification;
import org.stellar.anchor.platform.paymentobserver.circlemodels.TransferNotificationBody;
import org.stellar.anchor.util.GsonUtils;
import org.stellar.anchor.util.Log;

public class CirclePaymentObserverService {
  private final OkHttpClient httpClient;
  private final CirclePaymentObserverConfig circlePaymentObserverConfig;

  private final Gson gson = GsonUtils.builder()
      .registerTypeAdapter(CircleTransfer.class, new CircleTransfer.Serialization())
      .setPrettyPrinting()
      .create();

  public CirclePaymentObserverService(
      OkHttpClient httpClient, CirclePaymentObserverConfig circlePaymentObserverConfig) {
    this.httpClient = httpClient;
    this.circlePaymentObserverConfig = circlePaymentObserverConfig;
  }

  public void handleCircleNotification(Map<String, Object> requestBody) {
    CircleNotification circleNotification = gson.fromJson(gson.toJson(requestBody), CircleNotification.class);
    String type = circleNotification.getType();

    switch(type) {
      case "SubscriptionConfirmation":
        handleSubscriptionConfirmationNotification(circleNotification);
        return;

      case "Notification":
        handleTransferNotification(circleNotification);
        return;

      default:
        Log.warn("Not handling notification of type " + type);
    }
  }

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

  public void handleTransferNotification(CircleNotification circleNotification) {
    TransferNotificationBody transferNotification = gson.fromJson(circleNotification.getMessage(), TransferNotificationBody.class);

    CircleTransfer circleTransfer = transferNotification.getTransfer();
    if (circleTransfer == null) {
      Log.error("Missing \"transfer\" value in notification of type \"transfers\".");
      return;
    }

    if (!circleTransfer.getStatus().equals(CirclePaymentStatus.COMPLETE)) {
      Log.info("Incomplete transfer:\n"+gson.toJson(circleTransfer));
      return;
    }

    Log.info("Completed transfer:\n"+gson.toJson(circleTransfer));
    // TODO: handle transfer event
  }
}
