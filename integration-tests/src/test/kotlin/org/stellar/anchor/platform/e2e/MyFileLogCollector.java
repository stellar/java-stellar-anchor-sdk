package org.stellar.anchor.platform.e2e;

import static com.google.common.base.Preconditions.checkArgument;

import com.palantir.docker.compose.execution.DockerCompose;
import com.palantir.docker.compose.logging.LogCollector;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MyFileLogCollector implements LogCollector {

  private static final Logger log =
      LoggerFactory.getLogger(com.palantir.docker.compose.logging.FileLogCollector.class);

  private final File logDirectory;

  public MyFileLogCollector(File logDirectory) {
    checkArgument(!logDirectory.isFile(), "Log directory cannot be a file");
    if (!logDirectory.exists()) {
      Validate.isTrue(
          logDirectory.mkdirs(), "Error making log directory: " + logDirectory.getAbsolutePath());
    }
    this.logDirectory = logDirectory;
  }

  public static LogCollector fromPath(String path) {
    return new com.palantir.docker.compose.logging.FileLogCollector(new File(path));
  }

  @Override
  public void collectLogs(DockerCompose dockerCompose) throws IOException, InterruptedException {
    for (String service : dockerCompose.services()) {
      try {
        collectLogs(service, dockerCompose);
      } catch (RuntimeException e) {
        log.error("Failed to collect logs for '{}'", service);
      }
    }
  }

  private void collectLogs(String container, DockerCompose dockerCompose) {
    File outputFile = new File(logDirectory, container + ".log");
    try {
      Files.createFile(outputFile.toPath());
    } catch (final FileAlreadyExistsException e) {
      // ignore
    } catch (final IOException e) {
      throw new RuntimeException("Error creating log file", e);
    }
    log.info("Writing logs for container '{}' to '{}'", container, outputFile.getAbsolutePath());
    try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
      if (!dockerCompose.writeLogs(container, outputStream)) {
        log.error("Timed out while collecting logs for '{}'", container);
      }
      outputStream.flush();
    } catch (IOException e) {
      throw new RuntimeException("Error reading log", e);
    } catch (Exception ex) {
      System.out.println("Exception in collectLogs");
      ex.printStackTrace();
    } finally {
      System.out.println("leaving collectLogs");
    }
  }
}
