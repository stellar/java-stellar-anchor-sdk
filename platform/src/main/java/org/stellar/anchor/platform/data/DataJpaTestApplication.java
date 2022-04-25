package org.stellar.anchor.platform.data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * This application is solely for the purpose of Spring Boot Test used in JUnit5. Spring Tests
 * requires the Spring Boot Application to be included in the `main` folder instead of the `test`
 * folder.
 */
@SpringBootApplication
public class DataJpaTestApplication {
  public static void main(String[] args) {
    SpringApplication.run(DataJpaTestApplication.class, args);
  }
}
