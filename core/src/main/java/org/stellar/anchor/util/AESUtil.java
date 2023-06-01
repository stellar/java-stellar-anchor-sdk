package org.stellar.anchor.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AESUtil {
  private final SecretKey secret;
  private final SecureRandom random = new SecureRandom();
  private final String algorithm = "AES/CBC/PKCS5Padding";

  public AESUtil(String password) throws Exception {

    byte[] salt = "salt".getBytes(StandardCharsets.UTF_8);

    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
    secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
  }

  public IvParameterSpec getIv() {
    byte[] iv = new byte[16];
    random.nextBytes(iv);
    return new IvParameterSpec(iv);
  }

  public String encrypt(String v, IvParameterSpec iv) throws Exception {
    Cipher cipherEncrypt = Cipher.getInstance(algorithm);
    cipherEncrypt.init(Cipher.ENCRYPT_MODE, secret, iv);
    byte[] bytes = cipherEncrypt.doFinal(v.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(bytes);
  }

  public String decrypt(String v, IvParameterSpec iv) throws Exception {
    Cipher cipherDecrypt = Cipher.getInstance(algorithm);
    cipherDecrypt.init(Cipher.DECRYPT_MODE, secret, iv);
    byte[] bytes = Base64.getDecoder().decode(v);
    return new String(cipherDecrypt.doFinal(bytes), StandardCharsets.UTF_8);
  }
}
