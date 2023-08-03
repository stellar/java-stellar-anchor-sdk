package org.stellar.anchor.platform;

import static org.stellar.anchor.util.Log.info;

import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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
    options.addOption("c", "custody-server", false, "Start Custody server.");
    options.addOption("p", "platform-server", false, "Start Platform API endpoint server.");
    options.addOption(
        "o", "stellar-observer", false, "Start Observer that streams from the Stellar blockchain.");
    options.addOption("e", "event-processor", false, "Start the event processor.");
    options.addOption("r", "anchor-reference-server", false, "Start anchor reference server.");
    options.addOption("k", "kotlin-reference-server", false, "Start Kotlin reference server.");
    options.addOption("t", "test-profile-runner", false, "Run the stack with test profile.");

    CommandLineParser parser = new DefaultParser();

    try {
      CommandLine cmd = parser.parse(options, args);
      boolean anyServerStarted = false;
      if (cmd.hasOption("sep-server") || cmd.hasOption("all")) {
        startSepServer(null);
        anyServerStarted = true;
      }

      if (cmd.hasOption("custody-server") || cmd.hasOption("all")) {
        startCustodyServer(null);
        anyServerStarted = true;
      }

      if (cmd.hasOption("platform-server") || cmd.hasOption("all")) {
        startPlatformServer(null);
        anyServerStarted = true;
      }

      if (cmd.hasOption("stellar-observer") || cmd.hasOption("all")) {
        startStellarObserver(null);
        anyServerStarted = true;
      }

      if (cmd.hasOption("event-processor") || cmd.hasOption("all")) {
        startEventProcessingServer(null);
        anyServerStarted = true;
      }

      if (cmd.hasOption("anchor-reference-server") || cmd.hasOption("all")) {
        startAnchorReferenceServer(null);
        anyServerStarted = true;
      }

      if (cmd.hasOption("kotlin-reference-server") || cmd.hasOption("all")) {
        startKotlinReferenceServer(null, true);
        anyServerStarted = true;
      }

      if (cmd.hasOption("test-profile-runner")) {
        startTestProfileRunner();
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
    return new SepServer().start(env);
  }

  public static ConfigurableApplicationContext startPlatformServer(Map<String, String> env) {
    return new PlatformServer().start(env);
  }

  public static ConfigurableApplicationContext startCustodyServer(Map<String, String> env) {
    return new CustodyServer().start(env);
  }

  public static ConfigurableApplicationContext startStellarObserver(Map<String, String> env) {
    return new StellarObservingServer().start(env);
  }

  public static ConfigurableApplicationContext startEventProcessingServer(Map<String, String> env) {
    return new EventProcessingServer().start(env);
  }

  public static ConfigurableApplicationContext startAnchorReferenceServer(Map<String, String> env) {
    String strPort = System.getProperty("ANCHOR_REFERENCE_SERVER_PORT");

    int port = DEFAULT_ANCHOR_REFERENCE_SERVER_PORT;

    if (strPort != null) {
      port = Integer.parseInt(strPort);
    }

    return AnchorReferenceServer.start(env, port, "/");
  }

  public static void startKotlinReferenceServer(Map<String, String> envMap, boolean wait) {
    RefenreceServerStartKt.start(envMap, wait);
  }

  public static void startTestProfileRunner() {
    info("Running test profile runner");
    TestProfileRunner.main();
  }

  static void printUsage(Options options) {
    HelpFormatter helper = new HelpFormatter();
    helper.setOptionComparator(null);
    helper.printHelp("java -jar anchor-platform.jar", options);
  }
}
