package org.stellar.anchor.paymentservice.circle;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;
import org.stellar.anchor.exception.HttpException;
import org.stellar.anchor.paymentservice.circle.model.CircleTransactionParty;
import org.stellar.anchor.paymentservice.circle.model.CircleTransfer;
import org.stellar.sdk.responses.GsonSingleton;
import org.stellar.sdk.responses.Page;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PathPaymentBaseOperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import shadow.com.google.common.reflect.TypeToken;
import shadow.com.google.gson.Gson;

public interface StellarReconciliation extends CircleResponseErrorHandler {

  HttpClient getWebClient(boolean authenticated);

  String getHorizonUrl();

  default Mono<CircleTransfer> updatedStellarSenderAddress(CircleTransfer transfer)
      throws HttpException {
    CircleTransfer transferCopy = gson.fromJson(gson.toJson(transfer), CircleTransfer.class);

    if (transferCopy.getSource().getId() != null) return Mono.just(transferCopy);

    // Only Stellar->CircleWallet transfers should arrive here with id == null, and they should also
    // have
    // chain == "XLM" and type == BLOCKCHAIN. Let's validate if that's true:
    if (!transferCopy.getSource().getChain().equals("XLM")
        || transferCopy.getSource().getType() != CircleTransactionParty.Type.BLOCKCHAIN) {
      throw new HttpException(500, "invalid source account");
    }

    String txHash = transferCopy.getTransactionHash();
    return getWebClient(false)
        .baseUrl(getHorizonUrl())
        .get()
        .uri("/transactions/" + txHash + "/payments")
        .responseSingle(handleResponseSingle())
        .mapNotNull(
            body -> {
              Type type = new TypeToken<Page<OperationResponse>>() {}.getType();
              Gson stellarSdkGson = GsonSingleton.getInstance();
              Page<OperationResponse> responsePage = stellarSdkGson.fromJson(body, type);

              for (OperationResponse opResponse : responsePage.getRecords()) {
                if (!opResponse.isTransactionSuccessful()) continue;

                if (!List.of("payment", "path_payment_strict_send", "path_payment_strict_receive")
                    .contains(opResponse.getType())) continue;

                String amount, from;
                if ("payment".equals(opResponse.getType())) {
                  PaymentOperationResponse paymentResponse = (PaymentOperationResponse) opResponse;
                  amount = paymentResponse.getAmount();
                  from = paymentResponse.getFrom();
                } else {
                  PathPaymentBaseOperationResponse pathPaymentResponse =
                      (PathPaymentBaseOperationResponse) opResponse;
                  amount = pathPaymentResponse.getAmount();
                  from = pathPaymentResponse.getFrom();
                }

                BigDecimal wantAmount = new BigDecimal(transferCopy.getAmount().getAmount());
                BigDecimal gotAmount = new BigDecimal(amount);
                if (wantAmount.compareTo(gotAmount) != 0) continue;

                transferCopy.getSource().setAddress(from);
                return transferCopy;
              }

              return null;
            });
  }
}
