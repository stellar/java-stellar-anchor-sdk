package org.stellar.anchor.platform;

import java.io.IOException;
import org.apache.commons.cli.*;

public class ServiceRunner {
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
      HelpFormatter helper = new HelpFormatter();
      helper.setOptionComparator(null);
      helper.printHelp("java -jar anchor-platform.jar", options);
    } catch (ParseException e) {
      HelpFormatter helper = new HelpFormatter();
      helper.printHelp("Usage", options);
    }
  }
}
