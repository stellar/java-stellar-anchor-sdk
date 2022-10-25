package org.stellar.anchor.platform.payment.observer.circle;

import static org.stellar.anchor.util.MathHelper.*;

import com.google.gson.Gson;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.stellar.anchor.api.exception.*;
import org.stellar.anchor.config.CirclePaymentObserverConfig;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.platform.payment.observer.PaymentListener;
import org.stellar.anchor.platform.payment.observer.circle.model.CircleNotification;
import org.stellar.anchor.platform.payment.observer.circle.model.CirclePaymentStatus;
import org.stellar.anchor.platform.payment.observer.circle.model.CircleTransactionParty;
import org.stellar.anchor.platform.payment.observer.circle.model.CircleTransfer;
import org.stellar.anchor.platform.payment.observer.circle.model.TransferNotificationBody;
import org.stellar.anchor.platform.payment.observer.circle.util.CircleAsset;
import org.stellar.anchor.util.GsonUtils;
import org.stellar.anchor.util.Log;
import org.stellar.anchor.util.StellarNetworkHelper;
import org.stellar.sdk.Network;
import org.stellar.sdk.Server;
import org.stellar.sdk.responses.Page;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PathPaymentBaseOperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;

public class CirclePaymentObserverService {
  private final OkHttpClient httpClient;
  private final String usdcIssuer;
  private final Server horizonServer;
  private final String trackedWallet;
  private final List<PaymentListener> observers;

  private final Gson gson =
      GsonUtils.builder()
          .registerTypeAdapter(CircleTransfer.class, new CircleTransfer.Serialization())
          .setPrettyPrinting()
          .create();

  public CirclePaymentObserverService(
      OkHttpClient httpClient,
      CirclePaymentObserverConfig circlePaymentObserverConfig,
      Horizon horizon,
      List<PaymentListener> observers) {
    this.httpClient = httpClient;
    Network stellarNetwork =
        StellarNetworkHelper.toStellarNetwork(horizon.getStellarNetworkPassphrase());
    String[] assetIdPieces = CircleAsset.stellarUSDC(stellarNetwork).split(":");
    this.usdcIssuer = assetIdPieces[assetIdPieces.length - 1];
    this.horizonServer = horizon.getServer();
    this.trackedWallet = circlePaymentObserverConfig.getTrackedWallet();
    this.observers = observers;
  }

  public void handleCircleNotification(CircleNotification circleNotification)
      throws UnprocessableEntityException, BadRequestException, ServerErrorException,
          EventPublishException {
    String type = Objects.toString(circleNotification.getType(), "");

    switch (type) {
      case "SubscriptionConfirmation":
        handleSubscriptionConfirmationNotification(circleNotification);
        return;

      case "Notification":
        handleTransferNotification(circleNotification);
        return;

      default:
        throw new UnprocessableEntityException(
            "Not handling notification of unsupported type \"" + type + "\".");
    }
  }

  /**
   * This will auto-subscribe to Circle when we receive a subscription available notification.
   *
   * @param circleNotification is the circle notification object.
   * @throws BadRequestException when the incoming circle subscription notification does not contain
   *     a SubscribeURL, or it can't be reached.
   */
  public void handleSubscriptionConfirmationNotification(CircleNotification circleNotification)
      throws BadRequestException {
    String subscribeUrl = circleNotification.getSubscribeUrl();
    if (subscribeUrl == null) {
      throw new BadRequestException(
          "Notification body of type SubscriptionConfirmation is missing subscription URL.");
    }

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
      throw new BadRequestException("Failed to call \"SubscribeURL\" endpoint.");
    }

    if (!response.isSuccessful()) {
      try (ResponseBody responseBody = response.body()) {
        if (responseBody != null) {
          Log.error(responseBody.string());
        }
      } catch (IOException e) {
        Log.errorEx(e);
      }
      throw new BadRequestException("Calling the \"SubscribeURL\" endpoint didn't succeed.");
    }

    Log.info("Successfully called subscribeUrl and got status code ", response.code());
  }

  /**
   * Handle incoming circle notifications of type "transfers". A transfer notification can contain
   * circle<>circle or circle<>stellar events.
   *
   * @param circleNotification is the circle notification object.
   * @throws BadRequestException when the incoming notification format is inconsistent with Circle
   *     documentation. This will return an error to Circle and Circle will try to submit the
   *     notification again.
   * @throws UnprocessableEntityException when the incoming notification doesn't match the
   *     characteristics we want to watch. This should not return any error to Circle.
   * @throws ServerErrorException when there's an error trying to fetch the Stellar network.
   */
  public void handleTransferNotification(CircleNotification circleNotification)
      throws BadRequestException, UnprocessableEntityException, ServerErrorException,
          EventPublishException {
    if (circleNotification.getMessage() == null) {
      throw new BadRequestException("Notification body of type Notification is missing a message.");
    }

    TransferNotificationBody transferNotification =
        gson.fromJson(circleNotification.getMessage(), TransferNotificationBody.class);

    String notificationType = transferNotification.getNotificationType();
    if (!Objects.equals("transfers", notificationType)) {
      throw new UnprocessableEntityException(
          String.format("Won't handle notification of type \"%s\".", notificationType));
    }

    CircleTransfer circleTransfer = transferNotification.getTransfer();
    if (circleTransfer == null) {
      throw new BadRequestException(
          "Missing \"transfer\" value in notification of type \"transfers\".");
    }

    if (!CirclePaymentStatus.COMPLETE.equals(circleTransfer.getStatus())) {
      throw new UnprocessableEntityException("Not a complete transfer.");
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
      throw new UnprocessableEntityException(
          "Neither source nor destination are Stellar accounts.");
    }

    if (!isWalletTracked(source) && !isWalletTracked(destination)) {
      throw new UnprocessableEntityException("None of the transfer wallets is being tracked.");
    }

    if (!circleTransfer.getAmount().getCurrency().equals("USD")) {
      throw new UnprocessableEntityException("The only supported Circle currency is USDC.");
    }

    ObservedPayment observedPayment;
    try {
      observedPayment = fetchCircleTransferOnStellar(circleTransfer);
    } catch (IOException ex) {
      throw new ServerErrorException(
          "Something went wrong when trying to fetch the Stellar network", ex);
    } catch (SepException ex) {
      String exMessage =
          String.format(
              "Payment from transaction %s contains an unsupported memo.",
              circleTransfer.getTransactionHash());
      throw new UnprocessableEntityException(exMessage, ex);
    }

    if (observedPayment == null) {
      throw new UnprocessableEntityException("Observed payment could not be fetched.");
    }

    if (isWalletTracked(destination)) {
      for (PaymentListener listener : observers) {
        listener.onReceived(observedPayment);
      }
    } else {
      final ObservedPayment finalObservedPayment1 = observedPayment;
      observers.forEach(observer -> observer.onSent(finalObservedPayment1));
    }
  }

  public boolean isWalletTracked(CircleTransactionParty party) {
    if (party.getType() != CircleTransactionParty.Type.WALLET) {
      return false;
    }

    if (Objects.equals(trackedWallet, "all")) {
      return true;
    }

    return Objects.equals(trackedWallet, party.getId());
  }

  /**
   * This will fetch the Stellar payment (or path payment) that originated the Circle transfer.
   *
   * @param circleTransfer the Circle transfer
   * @return an ObservedPayment or null if unable to convert
   * @throws IOException if an error happens fetching data from Stellar
   */
  public ObservedPayment fetchCircleTransferOnStellar(CircleTransfer circleTransfer)
      throws IOException, SepException {
    String txHash = circleTransfer.getTransactionHash();
    Page<OperationResponse> responsePage =
        horizonServer
            .payments()
            .forTransaction(txHash)
            .limit(200)
            .includeTransactions(true)
            .execute();

    for (OperationResponse opResponse : responsePage.getRecords()) {
      if (!opResponse.isTransactionSuccessful()) continue;

      if (!List.of("payment", "path_payment_strict_send", "path_payment_strict_receive")
          .contains(opResponse.getType())) continue;

      ObservedPayment observedPayment;
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

      BigDecimal transferAmount = decimal(circleTransfer.getAmount().getAmount());
      BigDecimal paymentAmount = decimal(observedPayment.amount);
      if (transferAmount.compareTo(paymentAmount) != 0) {
        continue;
      }

      observedPayment.setExternalTransactionId(circleTransfer.getId());
      observedPayment.setType(ObservedPayment.Type.CIRCLE_TRANSFER);
      return observedPayment;
    }

    return null;
  }
}
