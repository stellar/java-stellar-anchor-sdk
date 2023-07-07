package org.stellar.anchor.platform.component.platform;

import java.util.List;
import javax.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.CustodyConfig;
import org.stellar.anchor.custody.CustodyService;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.platform.action.ActionHandler;
import org.stellar.anchor.platform.action.DoStellarPaymentHandler;
import org.stellar.anchor.platform.action.DoStellarRefundHandler;
import org.stellar.anchor.platform.action.NotifyAmountsUpdatedHandler;
import org.stellar.anchor.platform.action.NotifyInteractiveFlowCompletedHandler;
import org.stellar.anchor.platform.action.NotifyOffchainFundsAvailableHandler;
import org.stellar.anchor.platform.action.NotifyOffchainFundsReceivedHandler;
import org.stellar.anchor.platform.action.NotifyOffchainFundsSentHandler;
import org.stellar.anchor.platform.action.NotifyOnchainFundsReceivedHandler;
import org.stellar.anchor.platform.action.NotifyOnchainFundsSentHandler;
import org.stellar.anchor.platform.action.NotifyRefundInitiatedHandler;
import org.stellar.anchor.platform.action.NotifyRefundSentHandler;
import org.stellar.anchor.platform.action.NotifyTransactionErrorHandler;
import org.stellar.anchor.platform.action.NotifyTransactionExpiredHandler;
import org.stellar.anchor.platform.action.NotifyTransactionRecoveryHandler;
import org.stellar.anchor.platform.action.NotifyTrustSetHandler;
import org.stellar.anchor.platform.action.RequestOffchainFundsHandler;
import org.stellar.anchor.platform.action.RequestOnchainFundsHandler;
import org.stellar.anchor.platform.action.RequestTrustHandler;
import org.stellar.anchor.platform.service.ActionService;
import org.stellar.anchor.sep24.Sep24DepositInfoGenerator;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

@Configuration
public class ActionBeans {

  @Bean
  ActionService actionService(List<ActionHandler<?>> actionHandlers) {
    return new ActionService(actionHandlers);
  }

  @Bean
  DoStellarPaymentHandler doStellarPaymentHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      CustodyConfig custodyConfig,
      Horizon horizon,
      AssetService assetService,
      CustodyService custodyService) {
    return new DoStellarPaymentHandler(
        txn24Store, txn31Store, validator, custodyConfig, horizon, assetService, custodyService);
  }

  @Bean
  DoStellarRefundHandler doStellarRefundHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      CustodyConfig custodyConfig,
      Horizon horizon,
      AssetService assetService,
      CustodyService custodyService) {
    return new DoStellarRefundHandler(
        txn24Store, txn31Store, validator, custodyConfig, horizon, assetService, custodyService);
  }

  @Bean
  NotifyAmountsUpdatedHandler notifyAmountsUpdatedHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      Horizon horizon,
      AssetService assetService) {
    return new NotifyAmountsUpdatedHandler(
        txn24Store, txn31Store, validator, horizon, assetService);
  }

  @Bean
  NotifyInteractiveFlowCompletedHandler notifyInteractiveFlowCompletedHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      Horizon horizon,
      AssetService assetService) {
    return new NotifyInteractiveFlowCompletedHandler(
        txn24Store, txn31Store, validator, horizon, assetService);
  }

  @Bean
  NotifyOffchainFundsAvailableHandler notifyOffchainFundsAvailableHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      Horizon horizon,
      AssetService assetService) {
    return new NotifyOffchainFundsAvailableHandler(
        txn24Store, txn31Store, validator, horizon, assetService);
  }

  @Bean
  NotifyOffchainFundsReceivedHandler notifyOffchainFundsReceivedHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      Horizon horizon,
      AssetService assetService,
      CustodyService custodyService,
      CustodyConfig custodyConfig) {
    return new NotifyOffchainFundsReceivedHandler(
        txn24Store, txn31Store, validator, horizon, assetService, custodyService, custodyConfig);
  }

  @Bean
  NotifyOffchainFundsSentHandler notifyOffchainFundsSentHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      Horizon horizon,
      AssetService assetService) {
    return new NotifyOffchainFundsSentHandler(
        txn24Store, txn31Store, validator, horizon, assetService);
  }

  @Bean
  NotifyOnchainFundsReceivedHandler notifyOnchainFundsReceivedHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      Horizon horizon,
      AssetService assetService) {
    return new NotifyOnchainFundsReceivedHandler(
        txn24Store, txn31Store, validator, horizon, assetService);
  }

  @Bean
  NotifyOnchainFundsSentHandler notifyOnchainFundsSentHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      Horizon horizon,
      AssetService assetService) {
    return new NotifyOnchainFundsSentHandler(
        txn24Store, txn31Store, validator, horizon, assetService);
  }

  @Bean
  NotifyRefundInitiatedHandler notifyRefundInitiatedHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      Horizon horizon,
      AssetService assetService) {
    return new NotifyRefundInitiatedHandler(
        txn24Store, txn31Store, validator, horizon, assetService);
  }

  @Bean
  NotifyRefundSentHandler notifyRefundSentHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      Horizon horizon,
      AssetService assetService) {
    return new NotifyRefundSentHandler(txn24Store, txn31Store, validator, horizon, assetService);
  }

  @Bean
  NotifyTransactionErrorHandler notifyTransactionErrorHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      Horizon horizon,
      AssetService assetService) {
    return new NotifyTransactionErrorHandler(
        txn24Store, txn31Store, validator, horizon, assetService);
  }

  @Bean
  NotifyTransactionExpiredHandler notifyTransactionExpiredHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      Horizon horizon,
      AssetService assetService) {
    return new NotifyTransactionExpiredHandler(
        txn24Store, txn31Store, validator, horizon, assetService);
  }

  @Bean
  NotifyTransactionRecoveryHandler notifyTransactionRecoveryHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      Horizon horizon,
      AssetService assetService) {
    return new NotifyTransactionRecoveryHandler(
        txn24Store, txn31Store, validator, horizon, assetService);
  }

  @Bean
  NotifyTrustSetHandler notifyTrustSetHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      Horizon horizon,
      AssetService assetService,
      CustodyConfig custodyConfig) {
    return new NotifyTrustSetHandler(
        txn24Store, txn31Store, validator, horizon, assetService, custodyConfig);
  }

  @Bean
  RequestOffchainFundsHandler requestOffchainFundsHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      Horizon horizon,
      AssetService assetService) {
    return new RequestOffchainFundsHandler(
        txn24Store, txn31Store, validator, horizon, assetService);
  }

  @Bean
  RequestOnchainFundsHandler requestOnchainFundsHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      Horizon horizon,
      AssetService assetService,
      CustodyService custodyService,
      CustodyConfig custodyConfig,
      Sep24DepositInfoGenerator sep24DepositInfoGenerator) {
    return new RequestOnchainFundsHandler(
        txn24Store,
        txn31Store,
        validator,
        horizon,
        assetService,
        custodyService,
        custodyConfig,
        sep24DepositInfoGenerator);
  }

  @Bean
  RequestTrustHandler requestTrustHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      Horizon horizon,
      AssetService assetService,
      CustodyConfig custodyConfig) {
    return new RequestTrustHandler(
        txn24Store, txn31Store, validator, horizon, assetService, custodyConfig);
  }
}