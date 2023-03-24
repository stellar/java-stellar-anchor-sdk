package org.stellar.anchor.platform;

import java.util.Map;
import org.apache.commons.cli.*;
import org.springframework.context.ConfigurableApplicationContext;
import org.stellar.anchor.reference.AnchorReferenceServer;
import org.stellar.reference.RefenreceServerStartKt;

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
    options.addOption("k", "kotlin-reference-server", false, "Start Kotlin reference server.");

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
        startAnchorReferenceServer();
        anyServerStarted = true;
      }

      if (cmd.hasOption("kotlin-reference-server") || cmd.hasOption("all")) {
        startKotlinReferenceServer(null, true);
        anyServerStarted = true;
      }

      if (!anyServerStarted) {
        printUsage(options);
      }
    } catch (ParseException e) {
      printUsage(options);
    }
  }

  public static ConfigurableApplicationContext startSepServer(Map<String, String> env) {
    return new AnchorPlatformServer().start(env);
  }

  public static ConfigurableApplicationContext startStellarObserver(Map<String, String> env) {
    return new StellarObservingServer().start(env);
  }

  public static ConfigurableApplicationContext startEventProcessor(Map<String, String> env) {
    return new EventProcessingServer().start(env);
  }

  public static ConfigurableApplicationContext startAnchorReferenceServer() {
    String strPort = System.getProperty("ANCHOR_REFERENCE_SERVER_PORT");

    int port = DEFAULT_ANCHOR_REFERENCE_SERVER_PORT;

    if (strPort != null) {
      port = Integer.parseInt(strPort);
    }

    return AnchorReferenceServer.start(port, "/");
  }

  public static void startKotlinReferenceServer(Map<String, String> envMap, boolean wait) {
    RefenreceServerStartKt.start(envMap, wait);
  }

  static void printUsage(Options options) {
    HelpFormatter helper = new HelpFormatter();
    helper.setOptionComparator(null);
    helper.printHelp("java -jar anchor-platform.jar", options);
  }
}
