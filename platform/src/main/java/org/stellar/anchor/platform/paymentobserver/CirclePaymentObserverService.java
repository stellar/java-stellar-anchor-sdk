package org.stellar.anchor.platform.paymentobserver;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.stellar.anchor.config.CirclePaymentObserverConfig;
import org.stellar.anchor.paymentservice.circle.model.CircleTransfer;
import org.stellar.anchor.util.GsonUtils;
import org.stellar.anchor.util.Log;

public class CirclePaymentObserverService {
  private final OkHttpClient httpClient;
  private final CirclePaymentObserverConfig circlePaymentObserverConfig;

  private final Gson gson = GsonUtils.builder().setPrettyPrinting().create();

  public CirclePaymentObserverService(
      OkHttpClient httpClient, CirclePaymentObserverConfig circlePaymentObserverConfig) {
    this.httpClient = httpClient;
    this.circlePaymentObserverConfig = circlePaymentObserverConfig;
  }

  public void handleCircleNotification(Map<String, Object> requestBody) {
    String type = (String) requestBody.get("Type");
    if (type.equals("SubscriptionConfirmation")) {
      handleSubscriptionConfirmationNotification(requestBody);
      return;
    }

    if (type.equals("Notification")) {
      Map<String, Object> message = (HashMap<String, Object>) requestBody.get("Message");
    }

    // Handle transfer body
    boolean isTransferNotification = requestBody.get("NotificationType").equals("transfers");
    if (isTransferNotification) {
      handleTransferNotification(requestBody);
      return;
    }

    Log.warn("Not handling notification of type " + requestBody.getOrDefault("notificationType", ""));
  }

  public void handleSubscriptionConfirmationNotification(Map<String, Object> requestBody) {
    CircleSubscriptionNotification subscriptionNotification =
        gson.fromJson(gson.toJson(requestBody), CircleSubscriptionNotification.class);
    String subscribeUrl = subscriptionNotification.subscribeUrl;
    System.out.println("=====> subscriptionNotification.subscribeUrl: " + subscribeUrl);
    // TODO: handle subscription notification

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

  public void handleTransferNotification(Map<String, ?> requestBody) {
    Object transferBody = requestBody.get("transfer");
    if (transferBody == null) {
      Log.error("Missing \"transfer\" value in notification of type \"transfers\".");
      return;
    }

    CircleTransfer circleTransfer = gson.fromJson(gson.toJson(transferBody), CircleTransfer.class);
    System.out.println("=====> circleTransfer: " + circleTransfer);
    // TODO: handle transfer body
  }
}
