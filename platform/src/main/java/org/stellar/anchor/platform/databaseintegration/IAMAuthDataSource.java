package org.stellar.anchor.platform.databaseintegration;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import com.amazonaws.services.rds.auth.GetIamAuthTokenRequest;
import com.amazonaws.services.rds.auth.RdsIamAuthTokenGenerator;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.data.util.Pair;

// Used by the data-spring-jdbc-aws-aurora-postgres settings to allow authentication to the db via
// AWS IAM
public class IAMAuthDataSource extends HikariDataSource {
  @Override
  public String getPassword() {
    return getToken();
  }

  private String getToken() {
    var region = new DefaultAwsRegionProviderChain().getRegion();
    var hostnamePort = getHostnamePort();

    RdsIamAuthTokenGenerator generator =
        RdsIamAuthTokenGenerator.builder()
            .credentials(new DefaultAWSCredentialsProviderChain())
            .region(region)
            .build();

    GetIamAuthTokenRequest request =
        GetIamAuthTokenRequest.builder()
            .hostname(hostnamePort.getFirst())
            .port(hostnamePort.getSecond())
            .userName(getUsername())
            .build();

    return generator.getAuthToken(request);
  }

  // JDBC URL has a standard URL format, like: jdbc:postgresql://localhost:5432/test_database
  private Pair<String, Integer> getHostnamePort() {
    var slashing = getJdbcUrl().indexOf("//") + 2;
    var sub = getJdbcUrl().substring(slashing, getJdbcUrl().indexOf("/", slashing));
    var splitted = sub.split(":");
    return Pair.of(splitted[0], Integer.parseInt(splitted[1]));
  }
}
