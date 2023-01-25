package org.stellar.anchor.platform;

import com.example.RefenreceServerStartKt;
import java.util.Map;
import org.apache.commons.cli.*;
import org.springframework.context.ConfigurableApplicationContext;
import org.stellar.anchor.reference.AnchorReferenceServer;

public class ServiceRunner {
  public static final int DEFAULT_ANCHOR_REFERENCE_SERVER_PORT = 8081;

  public static void main(String[] args) {
    Options options = new Options();
    options.addOption("h", "help", false, "Print this message.");
    options.addOption("a", "all", false, "Start all servers.");
    options.addOption("s", "sep-server", false, "Start SEP endpoint server.");
    options.addOption(
        "o", "stellar-observer", false, "Start Observer that streams from the Stellar blockchain.");
    options.addOption("e", "event-processor", false, "Start the event processor.");
    options.addOption("r", "anchor-reference-server", false, "Start anchor reference server.");

    CommandLineParser parser = new DefaultParser();

    try {
      CommandLine cmd = parser.parse(options, args);
      boolean anyServerStarted = false;
      if (cmd.hasOption("sep-server") || cmd.hasOption("all")) {
        startSepServer(null);
        anyServerStarted = true;
      }

      if (cmd.hasOption("stellar-observer") || cmd.hasOption("all")) {
        startStellarObserver(null);
        anyServerStarted = true;
      }

      if (cmd.hasOption("event-processor") || cmd.hasOption("all")) {
        startEventProcessor(null);
        anyServerStarted = true;
      }

      if (cmd.hasOption("anchor-reference-server") || cmd.hasOption("all")) {
        startAnchorReferenceServer(true);
        anyServerStarted = true;
      }

      if (!anyServerStarted) {
        printUsage(options);
      }
    } catch (ParseException e) {
      printUsage(options);
    }
  }

  static ConfigurableApplicationContext startSepServer(Map<String, Object> env) {
    return AnchorPlatformServer.start(env);
  }

  static ConfigurableApplicationContext startStellarObserver(Map<String, Object> env) {
    return StellarObservingServer.start(env);
  }

  static ConfigurableApplicationContext startEventProcessor(Map<String, Object> env) {
    return EventProcessingServer.start(env);
  }

  static void startAnchorReferenceServer(boolean waitKotlinServer) {
    String strPort = System.getProperty("ANCHOR_REFERENCE_SERVER_PORT");

    int port = DEFAULT_ANCHOR_REFERENCE_SERVER_PORT;

    if (strPort != null) {
      port = Integer.parseInt(strPort);
    }

    AnchorReferenceServer.start(port, "/");
    RefenreceServerStartKt.start(waitKotlinServer);
  }

  static void printUsage(Options options) {
    HelpFormatter helper = new HelpFormatter();
    helper.setOptionComparator(null);
    helper.printHelp("java -jar anchor-platform.jar", options);
  }
}
