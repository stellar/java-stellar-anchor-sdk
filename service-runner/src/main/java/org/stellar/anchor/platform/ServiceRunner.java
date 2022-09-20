package org.stellar.anchor.platform;

import java.util.Map;
import org.apache.commons.cli.*;
import org.springframework.context.ConfigurableApplicationContext;
import org.stellar.anchor.reference.AnchorReferenceServer;

public class ServiceRunner {
  public static final int DEFAULT_SEP_SERVER_PORT = 8080;
  public static final int DEFAULT_ANCHOR_REFERENCE_SERVER_PORT = 8081;
  public static final String DEFAULT_CONTEXTPATH = "/";

  public static void main(String[] args) {
    Options options = new Options();
    options.addOption("h", "help", false, "Print this message.");
    options.addOption("a", "all", false, "Start all servers.");
    options.addOption("s", "sep-server", false, "Start SEP endpoint server.");
    options.addOption(
        "o", "stellar-observer", false, "Start Observer that streams from the Stellar blockchain.");
    options.addOption("r", "anchor-reference-server", false, "Start anchor reference server.");

    CommandLineParser parser = new DefaultParser();

    try {
      CommandLine cmd = parser.parse(options, args);
      boolean anyServerStarted = false;
      if (cmd.hasOption("sep-server") || cmd.hasOption("all")) {
        startSepServer(DEFAULT_SEP_SERVER_PORT, DEFAULT_CONTEXTPATH, null);
        anyServerStarted = true;
      }

      if (cmd.hasOption("stellar-observer") || cmd.hasOption("all")) {
        startStellarObserver();
        anyServerStarted = true;
      }

      if (cmd.hasOption("anchor-reference-server") || cmd.hasOption("all")) {
        startAnchorReferenceServer();
        anyServerStarted = true;
      }

      if (!anyServerStarted) {
        printUsage(options);
      }
    } catch (ParseException e) {
      printUsage(options);
    }
  }

  static ConfigurableApplicationContext startSepServer(
      int port, String contextPath, Map<String, Object> env) {
    return AnchorPlatformServer.start(port, contextPath, env, true);
  }

  static ConfigurableApplicationContext startStellarObserver() {
    return StellarObservingService.start();
  }

  static void startAnchorReferenceServer() {
    String strPort = System.getProperty("ANCHOR_REFERENCE_SERVER_PORT");
    int port = DEFAULT_ANCHOR_REFERENCE_SERVER_PORT;
    if (strPort != null) {
      port = Integer.parseInt(strPort);
    }
    String contextPath = System.getProperty("ANCHOR_REFERENCE_CONTEXTPATH");
    if (contextPath == null) {
      contextPath = DEFAULT_CONTEXTPATH;
    }
    AnchorReferenceServer.start(port, contextPath);
  }

  static void printUsage(Options options) {
    HelpFormatter helper = new HelpFormatter();
    helper.setOptionComparator(null);
    helper.printHelp("java -jar anchor-platform.jar", options);
  }
}
