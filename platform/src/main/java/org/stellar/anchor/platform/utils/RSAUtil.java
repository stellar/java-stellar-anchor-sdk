package org.stellar.anchor.platform.utils;

import io.jsonwebtoken.SignatureAlgorithm;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.apache.commons.lang3.StringUtils;

public class RSAUtil {

  public static final String BEGIN_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----";
  public static final String END_PUBLIC_KEY = "-----END PUBLIC KEY-----";
  public static final String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
  public static final String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";

  public static final String RSA_ALGORITHM = SignatureAlgorithm.RS512.getFamilyName();
  public static final String SHA512_WITH_RSA_ALGORITHM = SignatureAlgorithm.RS512.getJcaName();

  /**
   * Generate a public key from a provided string.
   *
   * @param publicKey public key in a String format
   * @param keyFactoryAlgorithm the name of the requested key algorithm
   * @return the public key object
   * @throws NoSuchAlgorithmException – if no Provider supports a KeyFactorySpi implementation for
   *     the specified algorithm
   * @throws InvalidKeySpecException – if the given key specification is inappropriate for this key
   *     factory to produce a public key.
   */
  public static PublicKey generatePublicKey(String publicKey, String keyFactoryAlgorithm)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    publicKey =
        publicKey
            .replace(BEGIN_PUBLIC_KEY, StringUtils.EMPTY)
            .replaceAll(StringUtils.CR, StringUtils.EMPTY)
            .replaceAll(StringUtils.LF, StringUtils.EMPTY)
            .replace(END_PUBLIC_KEY, StringUtils.EMPTY);

    byte[] keyBytes = Base64.getDecoder().decode(publicKey);
    X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(keyBytes);
    KeyFactory kf = KeyFactory.getInstance(keyFactoryAlgorithm);

    return kf.generatePublic(X509publicKey);
  }

  /**
   * Generate a private key from a provided string.
   *
   * @param privateKey private key in a String format
   * @param keyFactoryAlgorithm the name of the requested key algorithm
   * @return the public key object
   * @throws NoSuchAlgorithmException – if no Provider supports a KeyFactorySpi implementation for
   *     the specified algorithm
   * @throws InvalidKeySpecException – if the given key specification is inappropriate for this key
   *     factory to produce a private key.
   */
  public static PrivateKey generatePrivateKey(String privateKey, String keyFactoryAlgorithm)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    privateKey =
        privateKey
            .replace(BEGIN_PRIVATE_KEY, StringUtils.EMPTY)
            .replaceAll(StringUtils.CR, StringUtils.EMPTY)
            .replaceAll(StringUtils.LF, StringUtils.EMPTY)
            .replace(END_PRIVATE_KEY, StringUtils.EMPTY);

    byte[] keyBytes = Base64.getDecoder().decode(privateKey);
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
    KeyFactory kf = KeyFactory.getInstance(keyFactoryAlgorithm);

    return kf.generatePrivate(keySpec);
  }

  public static boolean isValidPublicKey(String publicKey, String keyFactoryAlgorithm) {
    try {
      generatePublicKey(publicKey, keyFactoryAlgorithm);
    } catch (Exception e) {
      return false;
    }

    return true;
  }

  public static boolean isValidPrivateKey(String privateKey, String keyFactoryAlgorithm) {
    try {
      generatePrivateKey(privateKey, keyFactoryAlgorithm);
    } catch (Exception e) {
      return false;
    }

    return true;
  }

  /**
   * Verify signature
   *
   * @param signature signature to verify
   * @param dataString data to verify
   * @param publicKey public key
   * @param signatureAlgorithm the standard name of the algorithm requested
   * @return true if signature is valid; false otherwise
   */
  public static boolean isValidSignature(
      String signature, String dataString, PublicKey publicKey, String signatureAlgorithm)
      throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
    System.out.println(generate(dataString, signatureAlgorithm));
    if (publicKey == null) {
      throw new IllegalArgumentException("Public key is null");
    }

    Signature sign = Signature.getInstance(signatureAlgorithm);
    sign.initVerify(publicKey);
    sign.update(dataString.getBytes());

    return sign.verify(Base64.getDecoder().decode(signature));
  }

  private static String generate(String dataString, String signatureAlgorithm) {
    try {
      String key =
          "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAJPq9AU6f8GrkSccloRb+UJmbxilZ7iLkvp0FeR/yymQuJDMNXPXwt5MYR9VJoY0uMDHThaEwbQAc2M4L9wl0EtsESebODUTqnsWZ8/a6GsmvIM3kz02ZOUHLst0krSkECPUHLLUKuw/HWz3LWxWLeqVRIcsCwKJNaBPZJzFw/8PAgMBAAECgYBhWXSYLFQApmW1k/8LxWxa4wei9Nk6f8GPy+7Mn76Z8IFH6t4TC6FYpHQXJvdfxDsDxSgDcgP574IBfu0gulJHEIsoDy1xR8TPobfhwoS1NJn70a8KQA6Whu+K5pjJxGUC7bpJH4ZK0O+szB1mNC0WgUywIc0lXCFFnCrgbNFWQQJBAM7+zgHAyD6rqQem09JcabMkwxu0mYih1mvQ3uv/fay8BdgZAqMuacMAPQ/A1TLeuTbMQ0LojnK/QlotbhHVnSkCQQC27684POaRrVIsVo5uKvQsKSNlYPpvGrGapHKiVgIQYfZSEY9SrazqKbA5yux0OR8ZFFSiJCThRElpzhnWFYl3AkEArNsLnVsn3W3sUX93FAwoGHlylQhTzk2XiaF7BwjsIftBxhvcn/h6SWVBmI4ne7uSX7hj0tPxYNFmz3dwm2QPQQJBAKS8mPSy2wtqoiotVBvfcHzoGujrePpednuFBXosq7UnEpN7Hq7cmW9RVVHl7CMJYXjLNx/AHroBLX8rS1bflCcCQBclpUG1PybVy1jHXTdI0w6zB6AwjaeFN5x4+b7hRe29yLNF532uIatxif19LHb5jUC7EefpLWBxx/bB4JCIyug=";
      PrivateKey privateKey = RSAUtil.generatePrivateKey(key, RSAUtil.RSA_ALGORITHM);

      Signature sign = Signature.getInstance(signatureAlgorithm);
      sign.initSign(privateKey);
      sign.update(dataString.getBytes());

      byte[] signatureBytes = sign.sign();
      return Base64.getEncoder().encodeToString(signatureBytes);
    } catch (Exception e) {
      return null;
    }
  }
}
