package org.stellar.anchor.platform.payment.observer.circle;

import static org.stellar.anchor.util.MathHelper.decimal;

import java.math.BigDecimal;
import java.util.List;
import org.stellar.anchor.api.exception.HttpException;
import org.stellar.anchor.platform.payment.observer.circle.model.CircleTransactionParty;
import org.stellar.anchor.platform.payment.observer.circle.model.CircleTransfer;
import org.stellar.sdk.Server;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PathPaymentBaseOperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

public interface StellarReconciliation extends CircleResponseErrorHandler {

  HttpClient getWebClient(boolean authenticated);

  Server getHorizonServer();

  default Mono<CircleTransfer> updatedStellarSenderAddress(CircleTransfer transfer)
      throws HttpException {
    CircleTransfer transferCopy = gson.fromJson(gson.toJson(transfer), CircleTransfer.class);

    if (transferCopy.getSource().getId() != null) return Mono.just(transferCopy);

    // Only Stellar->CircleWallet transfers should arrive here with id == null, and they should also
    // have chain == "XLM" and type == BLOCKCHAIN. Let's validate if that's true:
    if (!transferCopy.getSource().getChain().equals("XLM")
        || transferCopy.getSource().getType() != CircleTransactionParty.Type.BLOCKCHAIN) {
      throw new HttpException(500, "invalid source account");
    }

    String txHash = transferCopy.getTransactionHash();
    return Mono.fromCallable(() -> getHorizonServer().payments().forTransaction(txHash).execute())
        .mapNotNull(
            responsePage -> {
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

                BigDecimal wantAmount = decimal(transferCopy.getAmount().getAmount());
                BigDecimal gotAmount = decimal(amount);
                if (wantAmount.compareTo(gotAmount) != 0) continue;

                transferCopy.getSource().setAddress(from);
                return transferCopy;
              }

              return null;
            });
  }
}
