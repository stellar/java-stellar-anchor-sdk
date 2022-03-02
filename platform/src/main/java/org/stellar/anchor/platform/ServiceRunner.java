package org.stellar.anchor.platform;

import org.apache.commons.cli.*;
import org.stellar.anchor.reference.AnchorReferenceServer;

import java.io.IOException;

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
