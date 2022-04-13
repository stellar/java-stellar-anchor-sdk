package org.stellar.anchor.reference;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;
import org.stellar.anchor.event.models.AnchorEvent;
import org.stellar.anchor.event.models.TransactionEvent;
import org.stellar.anchor.exception.AnchorException;
import org.stellar.anchor.reference.client.PlatformApiClient;
import org.stellar.anchor.util.Log;
import org.stellar.platform.apis.platform.requests.PatchTransactionRequest;
import org.stellar.platform.apis.platform.requests.PatchTransactionsRequest;
import org.stellar.platform.apis.shared.Amount;

@Component
public class AnchorEventConsumerService implements DisposableBean, Runnable {
  private Thread thread;
  private Consumer consumer;
  private volatile boolean shutdown = false;

  AnchorEventConsumerService() {
    this.thread = new Thread(this);
    this.thread.start();
  }

  @Override
  public void run() {
    Log.info("queue consumer server started ");
    Properties props = new Properties();

    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "group_one1");
    // props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

    Consumer<String, AnchorEvent> consumer = new KafkaConsumer<String, AnchorEvent>(props);
    consumer.subscribe(
        Arrays.asList(
            "ap_event_quote_created",
            "ap_event_transaction_created",
            "ap_event_transaction_payment_received")); // TODO make this configurable
    this.consumer = consumer;

    while (!shutdown) {
      try {
        ConsumerRecords<String, AnchorEvent> consumerRecords =
            consumer.poll(Duration.ofSeconds(10));
        System.out.println(
            "Anchor Reference Server - Messages received - " + consumerRecords.count());
        consumerRecords.forEach(
            record -> {
              for (Header header : record.headers()) {
                if (Objects.equals(header.key(), "type")) {
                  String eventType = new String(header.value(), StandardCharsets.UTF_8);
                  switch (eventType) {
                    case "quote_created":
                      Log.debug("anchor_platform_event - quote_created " + record.value());
                      break;
                    case "transaction_created":
                      Log.debug(
                          "anchor_platform_event - " + "transaction_created " + record.value());
                      break;
                    case "transaction_payment_received":
                      Log.debug(
                          "anchor_platform_event - "
                              + "transaction_payment_received "
                              + record.value());
                      if (record.value() instanceof TransactionEvent) {
                        TransactionEvent transactionEvent = (TransactionEvent) record.value();
                        PatchTransactionsRequest txnRequest =
                            PatchTransactionsRequest.builder()
                                .records(
                                    List.of(
                                        PatchTransactionRequest.builder()
                                            .id(transactionEvent.getId())
                                            .status(TransactionEvent.Status.COMPLETED.status)
                                            .amountFee(
                                                new Amount(
                                                    transactionEvent.getAmountFee().getAmount(),
                                                    transactionEvent.getAmountFee().getAsset()))
                                            .amountOut(
                                                new Amount(
                                                    transactionEvent.getAmountOut().getAmount(),
                                                    transactionEvent.getAmountOut().getAsset()))
                                            .build()))
                                .build();
                        PlatformApiClient platformClient =
                            new PlatformApiClient("http://localhost:8080");
                        try {
                          platformClient.patchTransaction(txnRequest);
                        } catch (IOException e) {
                          e.printStackTrace();
                        } catch (AnchorException e) {
                          e.printStackTrace();
                        }
                      }

                      break;
                    case "transaction_error":
                      Log.debug("anchor_platform_event - " + "transaction_error " + record.value());
                      break;
                    default:
                      Log.debug(
                          "error: anchor_platform_event - invalid " + "message type '%s'%n",
                          eventType);
                  }
                  break;
                }
              }
            });
      } catch (Exception ex) {
        Log.errorEx(ex);
        try {
          // TODO cleanup/skip messages that are unreadable
          Thread.sleep(1 * 1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  @Override
  public void destroy() {
    this.consumer.close();
  }
}
