package org.stellar.anchor.platform.custody.service;

import static org.stellar.anchor.platform.utils.SecurityUtil.RSA_ALGORITHM;
import static org.stellar.anchor.platform.utils.SecurityUtil.SHA512_WITH_RSA_ALGORITHM;
import static org.stellar.anchor.platform.utils.SecurityUtil.generatePublicKey;
import static org.stellar.anchor.platform.utils.SecurityUtil.isValidSignature;
import static org.stellar.anchor.util.Log.debugF;
import static org.stellar.anchor.util.Log.info;
import static org.stellar.anchor.util.StringHelper.isEmpty;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.stellar.anchor.api.custody.fireblocks.FireblocksEventObject;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.util.GsonUtils;

@AllArgsConstructor
public class FireblocksEventsService {
  public static final String FIREBLOCKS_SIGNATURE_HEADER = "fireblocks-signature";

  private final PublicKey publicKey;

  public FireblocksEventsService(String fireblocksPublicKey) {
    publicKey = getFireblocksPublicKey(fireblocksPublicKey);
  }

  /**
   * Process request sent by Fireblocks to webhook endpoint
   *
   * @param eventObject Request body
   * @param headers HTTP headers
   * @throws BadRequestException when fireblocks-signature is missing, empty or contains invalid
   *     signature
   */
  public void handleFireblocksEvent(String eventObject, Map<String, String> headers)
      throws BadRequestException {
    String signature = headers.get(FIREBLOCKS_SIGNATURE_HEADER);
    if (signature == null) {
      throw new BadRequestException("'" + FIREBLOCKS_SIGNATURE_HEADER + "' header missed");
    }

    if (isEmpty(signature)) {
      throw new BadRequestException("'" + FIREBLOCKS_SIGNATURE_HEADER + "' is empty");
    }

    debugF("/webhook endpoint called with signature '{}'", signature);
    debugF("/webhook endpoint called with data '{}'", eventObject);

    try {
      if (isValidSignature(signature, eventObject, publicKey, SHA512_WITH_RSA_ALGORITHM)) {
        handleTransactionStatusChange(eventObject);
      } else {
        throw new BadRequestException("Signature validation failed");
      }
    } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
      throw new BadRequestException("Signature validation failed", e);
    }
  }

  /**
   * Get Fireblocks public key
   *
   * @return public key
   */
  private PublicKey getFireblocksPublicKey(String publicKey) {
    try {
      return generatePublicKey(publicKey, RSA_ALGORITHM);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      debugF("Failed to generate Fireblocks public key. Exception: {}", e);
      return null;
    }
  }

  /**
   * Handle and notify transaction status change
   *
   * @param eventObject event object
   */
  private void handleTransactionStatusChange(String eventObject) {
    FireblocksEventObject fireblocksEventObject =
        GsonUtils.getInstance().fromJson(eventObject, FireblocksEventObject.class);

    // TODO: handle transaction state change
    info(fireblocksEventObject);
  }
}
