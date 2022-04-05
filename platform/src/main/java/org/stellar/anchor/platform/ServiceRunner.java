package org.stellar.anchor.platform;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import org.apache.commons.cli.*;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.stellar.anchor.event.models.AnchorEvent;
import org.stellar.anchor.event.models.QuoteEvent;
import org.stellar.anchor.reference.AnchorReferenceServer;

public class ServiceRunner {
  public static final int DEFAULT_SEP_SERVER_PORT = 8080;
  public static final int DEFAULT_ANCHOR_REFERENCE_SERVER_PORT = 8081;
  public static final String DEFAULT_CONTEXTPATH = "/";

  public static void main(String[] args) throws IOException {
    Options options = new Options();
    options.addOption("h", "help", false, "Print this message.");
    options.addOption("a", "all", false, "Start all servers.");
    options.addOption("s", "sep-server", false, "Start SEP endpoint server.");
    options.addOption("p", "payment-observer", false, "Start payment observation server.");
    options.addOption("r", "anchor-reference-server", false, "Start anchor reference server.");
    // TODO remove this
    options.addOption("e", "event-server", false, "Start anchor event server.");
    options.addOption("ec", "event-consumer", false, "Start anchor event server.");

    CommandLineParser parser = new DefaultParser();

    try {
      CommandLine cmd = parser.parse(options, args);
      boolean anyServerStarted = false;
      if (cmd.hasOption("sep-server") || cmd.hasOption("all")) {
        startSepServer();
        anyServerStarted = true;
      }

      if (cmd.hasOption("anchor-reference-server") || cmd.hasOption("all")) {
        startAnchorReferenceServer();
        anyServerStarted = true;
      }

      if (cmd.hasOption("payment-observer") || cmd.hasOption("all")) {
        startPaymentObserver();
        anyServerStarted = true;
      }

      if (cmd.hasOption("event-server") || cmd.hasOption("all")) {
        startEventsServer();
        anyServerStarted = true;
      }

      if (cmd.hasOption("event-consumer") || cmd.hasOption("all")) {
        startConsumerServer();
        anyServerStarted = true;
      }

      if (!anyServerStarted) {
        printUsage(options);
      }
    } catch (ParseException e) {
      printUsage(options);
    }
  }

  static void startSepServer() {
    String strPort = System.getProperty("SEP_SERVER_PORT");
    String contextPath = System.getProperty("SEP_CONTEXTPATH");
    int port = DEFAULT_SEP_SERVER_PORT;
    if (strPort != null) {
      port = Integer.parseInt(strPort);
    }
    if (contextPath == null) {
      contextPath = DEFAULT_CONTEXTPATH;
    }
    AnchorPlatformServer.start(port, contextPath);
  }

  static void startConsumerServer() {
    //    JsonDeserializer<QuoteEvent> deserializer = new JsonDeserializer<>(QuoteEvent.class);
    //    deserializer.setRemoveTypeHeaders(false);
    //    deserializer.addTrustedPackages("*");
    //    deserializer.setUseTypeMapperForKey(true);platform/src/main/java/org/stellar/anchor/platform/ServiceRunner.java

    Properties props = new Properties();

    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "group_one1");
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

    Consumer<String, QuoteEvent> consumer = new KafkaConsumer<String, QuoteEvent>(props);

    consumer.subscribe(Arrays.asList("ap_event_quote_created"));

    // ObjectMapper objectMapper = new ObjectMapper();
    // EventRequest event = objectMapper.readValue(consumerRecords, );
    // AtomicReference<EventRequest> msgCons = new AtomicReference<>();
    while (true) {
      ConsumerRecords<String, QuoteEvent> consumerRecords = consumer.poll(Duration.ofSeconds(10));
      System.out.println("Messages received - " + consumerRecords.count());
      consumerRecords.forEach(
          record -> {
            // msgCons.set(record.value());
            System.out.println("Message received " + record.value());
          });
    }
    // consumer.close();
  }

  static void startEventsServer() {
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    Producer<String, AnchorEvent> producer = new KafkaProducer<String, AnchorEvent>(props);

    String topic = "quote_created1";
    // String key = "testkey";
    //    EventRequest event = new EventRequest("1234", "quote_created1");
    //    event.setData(
    //        new QuoteEvent(
    //            "id",
    //            "sellAsset",
    //            "buyAsset",
    //            LocalDateTime.now(),
    //            "price",
    //            null,
    //            "transactionId",
    //            LocalDateTime.now(),
    //            "clientDomain"));
    QuoteEvent event =
        new QuoteEvent(
            "id",
            "quote_created",
            "sellAsset",
            "buyAsset",
            LocalDateTime.now(),
            "price",
            null,
            "transactionId",
            LocalDateTime.now(),
            "clientDomain");

    ProducerRecord<String, AnchorEvent> record =
        new ProducerRecord<String, AnchorEvent>(topic, event);
    try {
      producer.send(record).get();
    } catch (ExecutionException e) {
      ;
    } catch (InterruptedException e) {
      ;
    }

    producer.close();
  }

  static void startAnchorReferenceServer() {
    String strPort = System.getProperty("ANCHOR_REFERENCE_SERVER_PORT");
    String contextPath = System.getProperty("ANCHOR_REFERENCE_CONTEXTPATH");
    int port = DEFAULT_ANCHOR_REFERENCE_SERVER_PORT;
    if (strPort != null) {
      port = Integer.parseInt(strPort);
    }
    if (contextPath == null) {
      contextPath = DEFAULT_CONTEXTPATH;
    }
    AnchorReferenceServer.start(port, contextPath);
  }

  static void startPaymentObserver() {
    // TODO: implement.
    System.out.println("Not implemented yet.");
  }

  static void printUsage(Options options) {
    HelpFormatter helper = new HelpFormatter();
    helper.setOptionComparator(null);
    helper.printHelp("java -jar anchor-platform.jar", options);
  }
}
