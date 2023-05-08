package org.stellar.anchor.platform.config;

import static org.stellar.anchor.platform.utils.RSAUtil.RSA_ALGORITHM;
import static org.stellar.anchor.platform.utils.RSAUtil.generatePrivateKey;
import static org.stellar.anchor.platform.utils.RSAUtil.generatePublicKey;
import static org.stellar.anchor.util.StringHelper.isEmpty;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.platform.utils.RSAUtil;
import org.stellar.anchor.util.NetUtil;

@Data
public class FireblocksConfig implements Validator {

  private String baseUrl;
  private String vaultAccountId;
  private CustodySecretConfig secretConfig;
  private String transactionsReconciliationCron;
  private String publicKey;
  private int maxAttempts;
  private int delay;

  public FireblocksConfig(CustodySecretConfig secretConfig) {
    this.secretConfig = secretConfig;
  }

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return FireblocksConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    validateBaseUrl(errors);
    validateVaultAccountId(errors);
    validateApiKey(errors);
    validateSecretKey(errors);
    validateTransactionsReconciliationCron(errors);
    validatePublicKey(errors);
    validateMaxAttempts(errors);
    validateDelay(errors);
  }

  private void validateBaseUrl(Errors errors) {
    if (isEmpty(baseUrl)) {
      errors.rejectValue(
          "baseUrl",
          "custody-fireblocks-base-url-empty",
          "The custody.fireblocks.base_url cannot be empty and must be defined");
    }
    if (!NetUtil.isUrlValid(baseUrl)) {
      errors.rejectValue(
          "baseUrl",
          "custody-fireblocks-base-url-invalid",
          "The custody.fireblocks.base_url is not a valid URL");
    }
  }

  private void validateVaultAccountId(Errors errors) {
    if (isEmpty(vaultAccountId)) {
      errors.rejectValue(
          "vaultAccountId",
          "custody-fireblocks-vault-account-id-empty",
          "The custody.fireblocks.vault_account_id cannot be empty and must be defined");
    }
  }

  private void validateApiKey(Errors errors) {
    if (isEmpty(secretConfig.getFireblocksApiKey())) {
      errors.reject(
          "secret-custody-fireblocks-api-key-empty",
          "Please set environment variable secret.custody.fireblocks.api_key or SECRET_CUSTODY_FIREBLOCKS_API_KEY");
    }
  }

  private void validateSecretKey(Errors errors) {
    if (isEmpty(secretConfig.getFireblocksSecretKey())) {
      errors.reject(
          "secret-custody-fireblocks-secret-key-empty",
          "Please set environment variable secret.custody.fireblocks.secret_key or SECRET_CUSTODY_FIREBLOCKS_SECRET_KEY");
    }
    if (!RSAUtil.isValidPrivateKey(secretConfig.getFireblocksSecretKey(), RSAUtil.RSA_ALGORITHM)) {
      errors.reject(
          "secret-custody-fireblocks-secret_key-invalid",
          "The secret-custody-fireblocks-secret_key is invalid");
    }
  }

  private void validateTransactionsReconciliationCron(Errors errors) {
    if (isEmpty(transactionsReconciliationCron)) {
      errors.reject(
          "custody-fireblocks-transactions-reconciliation-cron-empty",
          "The custody.fireblocks.transactions_reconciliation_cron is empty");
    }
    if (!CronExpression.isValidExpression(transactionsReconciliationCron)) {
      errors.reject(
          "custody-fireblocks-transactions-reconciliation-cron-invalid",
          "The custody.fireblocks.transactions_reconciliation_cron is invalid");
    }
  }

  public void validatePublicKey(Errors errors) {
    if (isEmpty(publicKey)) {
      errors.reject(
          "custody-fireblocks-public_key-empty", "The custody.fireblocks.public_key is empty");
    }
    if (!RSAUtil.isValidPublicKey(publicKey, RSAUtil.RSA_ALGORITHM)) {
      errors.reject(
          "custody-fireblocks-public_key-invalid", "The custody-fireblocks-public_key is invalid");
    }
  }

  public void validateMaxAttempts(Errors errors) {
    if (maxAttempts < 0) {
      errors.reject(
          "custody-fireblocks-max_attempts-invalid",
          "custody-fireblocks-max_attempts must be greater than 0");
    }
  }

  public void validateDelay(Errors errors) {
    if (maxAttempts < 0) {
      errors.reject(
          "custody-fireblocks-delay-invalid", "custody-fireblocks-delay must be greater than 0");
    }
  }

  /**
   * Get Fireblocks public key
   *
   * @return public key
   */
  public PublicKey getFireblocksPublicKey() throws InvalidConfigException {
    try {
      return generatePublicKey(publicKey, RSA_ALGORITHM);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new InvalidConfigException(List.of("Failed to generate Fireblocks public key"), e);
    }
  }

  /**
   * Get Fireblocks private key
   *
   * @return private key
   */
  public PrivateKey getFireblocksPrivateKey() throws InvalidConfigException {
    try {
      return generatePrivateKey(secretConfig.getFireblocksSecretKey(), RSA_ALGORITHM);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new InvalidConfigException(List.of("Failed to generate Fireblocks private key"), e);
    }
  }
}
