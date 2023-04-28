package org.stellar.anchor.platform.custody.fireblocks;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.stellar.anchor.platform.config.FireblocksConfig;
import org.stellar.anchor.platform.exception.FireblocksException;

public class FireblocksApiClient {

  private static final String API_KEY_HEADER_NAME = "X-API-Key";
  private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
  private static final String TOKEN_PREFIX = "Bearer ";
  private static final String JSON_UTF8_MEDIA_TYPE = "application/json; charset=utf-8";
  private static final String SHA_256_ALGORITHM = "SHA-256";
  private static final String RSA_ALGORITHM = "RSA";
  private static final int TOKEN_EXPIRATION_SECONDS = 55;

  private final OkHttpClient client;
  private final String baseUrl;
  private final String apiKey;

  private final PrivateKey privateKey;

  public FireblocksApiClient(OkHttpClient httpClient, FireblocksConfig fireblocksConfig) {
    this.client = httpClient;
    this.baseUrl = fireblocksConfig.getBaseUrl();
    this.apiKey = fireblocksConfig.getSecretConfig().getFireblocksApiKey();

    try {
      byte[] keyBytes =
          Base64.getDecoder()
              .decode(
                  fireblocksConfig
                      .getSecretConfig()
                      .getFireblocksSecretKey()
                      .replace("-----BEGIN PRIVATE KEY-----", StringUtils.EMPTY)
                      .replace("-----END PRIVATE KEY-----", StringUtils.EMPTY)
                      .replace(StringUtils.LF, StringUtils.EMPTY));

      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
      KeyFactory factory = KeyFactory.getInstance(RSA_ALGORITHM);
      this.privateKey = factory.generatePrivate(keySpec);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RuntimeException("Invalid Fireblocks secret key", e);
    }
  }

  public String get(String path) throws FireblocksException {
    return doRequest(
        new Request.Builder()
            .url(baseUrl + path)
            .addHeader(API_KEY_HEADER_NAME, apiKey)
            .addHeader(AUTHORIZATION_HEADER_NAME, TOKEN_PREFIX + signJwt(path, StringUtils.EMPTY))
            .build());
  }

  public String post(String path, String data) throws FireblocksException {
    return doRequest(
        new Request.Builder()
            .url(baseUrl + path)
            .addHeader(API_KEY_HEADER_NAME, apiKey)
            .addHeader(AUTHORIZATION_HEADER_NAME, TOKEN_PREFIX + signJwt(path, data))
            .post(RequestBody.create(data.getBytes(), MediaType.parse(JSON_UTF8_MEDIA_TYPE)))
            .build());
  }

  private String doRequest(Request request) throws FireblocksException {
    try (Response response = client.newCall(request).execute()) {
      ResponseBody responseBody = response.body();
      String responseBodyJson = null;

      if (responseBody != null) {
        responseBodyJson = responseBody.string();
      }

      if (HttpStatus.valueOf(response.code()).is2xxSuccessful()) {
        return responseBodyJson;
      } else {
        throw new FireblocksException(
            String.format(
                "Fireblocks API returned an error. HTTP status[%d], response[%s]",
                response.code(), responseBodyJson));
      }
    } catch (IOException e) {
      throw new FireblocksException("Exception occurred during request to Fireblocks API", e);
    }
  }

  private String signJwt(String path, String dataJSONString) {
    String bodyHash;

    try {
      MessageDigest digest = MessageDigest.getInstance(SHA_256_ALGORITHM);
      digest.update(dataJSONString.getBytes());
      BigInteger number = new BigInteger(1, digest.digest());
      StringBuilder hexString = new StringBuilder(number.toString(16));

      while (hexString.length() < 64) {
        hexString.insert(0, '0');
      }

      bodyHash = hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Unable to generate body hash for Fireblocks JWT token", e);
    }

    Instant now = Instant.now();
    Instant expirationTime = now.plusSeconds(TOKEN_EXPIRATION_SECONDS);

    return Jwts.builder()
        .setSubject(apiKey)
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(expirationTime))
        .claim("nonce", UUID.randomUUID().toString())
        .claim("uri", path)
        .claim("bodyHash", bodyHash)
        .signWith(SignatureAlgorithm.RS256, privateKey)
        .compact();
  }
}
