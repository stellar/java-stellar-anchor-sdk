package org.stellar.anchor.platform.component.platform;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.CustodyConfig;
import org.stellar.anchor.custody.CustodyService;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.platform.action.ActionHandler;
import org.stellar.anchor.platform.action.DoStellarPaymentHandler;
import org.stellar.anchor.platform.action.DoStellarRefundHandler;
import org.stellar.anchor.platform.action.NotifyAmountsUpdatedHandler;
import org.stellar.anchor.platform.action.NotifyCustomerInfoUpdatedHandler;
import org.stellar.anchor.platform.action.NotifyInteractiveFlowCompletedHandler;
import org.stellar.anchor.platform.action.NotifyOffchainFundsAvailableHandler;
import org.stellar.anchor.platform.action.NotifyOffchainFundsPendingHandler;
import org.stellar.anchor.platform.action.NotifyOffchainFundsReceivedHandler;
import org.stellar.anchor.platform.action.NotifyOffchainFundsSentHandler;
import org.stellar.anchor.platform.action.NotifyOnchainFundsReceivedHandler;
import org.stellar.anchor.platform.action.NotifyOnchainFundsSentHandler;
import org.stellar.anchor.platform.action.NotifyRefundPendingHandler;
import org.stellar.anchor.platform.action.NotifyRefundSentHandler;
import org.stellar.anchor.platform.action.NotifyTransactionErrorHandler;
import org.stellar.anchor.platform.action.NotifyTransactionExpiredHandler;
import org.stellar.anchor.platform.action.NotifyTransactionRecoveryHandler;
import org.stellar.anchor.platform.action.NotifyTrustSetHandler;
import org.stellar.anchor.platform.action.RequestCustomerInfoUpdateHandler;
import org.stellar.anchor.platform.action.RequestOffchainFundsHandler;
import org.stellar.anchor.platform.action.RequestOnchainFundsHandler;
import org.stellar.anchor.platform.action.RequestTrustHandler;
import org.stellar.anchor.platform.config.PropertyCustodyConfig;
import org.stellar.anchor.platform.data.JdbcTransactionPendingTrustRepo;
import org.stellar.anchor.platform.service.ActionService;
import org.stellar.anchor.platform.validator.RequestValidator;
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
      RequestValidator requestValidator,
      CustodyConfig custodyConfig,
      Horizon horizon,
      AssetService assetService,
      CustodyService custodyService,
      EventService eventService,
      JdbcTransactionPendingTrustRepo transactionPendingTrustRepo) {
    return new DoStellarPaymentHandler(
        txn24Store,
        txn31Store,
        requestValidator,
        custodyConfig,
        horizon,
        assetService,
        custodyService,
        eventService,
        transactionPendingTrustRepo);
  }

  @Bean
  DoStellarRefundHandler doStellarRefundHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      CustodyConfig custodyConfig,
      AssetService assetService,
      CustodyService custodyService,
      EventService eventService) {
    return new DoStellarRefundHandler(
        txn24Store,
        txn31Store,
        requestValidator,
        custodyConfig,
        assetService,
        custodyService,
        eventService);
  }

  @Bean
  NotifyAmountsUpdatedHandler notifyAmountsUpdatedHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService) {
    return new NotifyAmountsUpdatedHandler(
        txn24Store, txn31Store, requestValidator, assetService, eventService);
  }

  @Bean
  NotifyInteractiveFlowCompletedHandler notifyInteractiveFlowCompletedHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService) {
    return new NotifyInteractiveFlowCompletedHandler(
        txn24Store, txn31Store, requestValidator, assetService, eventService);
  }

  @Bean
  NotifyOffchainFundsAvailableHandler notifyOffchainFundsAvailableHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService) {
    return new NotifyOffchainFundsAvailableHandler(
        txn24Store, txn31Store, requestValidator, assetService, eventService);
  }

  @Bean
  NotifyOffchainFundsPendingHandler notifyOffchainFundsPendingHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService) {
    return new NotifyOffchainFundsPendingHandler(
        txn24Store, txn31Store, requestValidator, assetService, eventService);
  }

  @Bean
  NotifyOffchainFundsReceivedHandler notifyOffchainFundsReceivedHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      CustodyService custodyService,
      CustodyConfig custodyConfig,
      EventService eventService) {
    return new NotifyOffchainFundsReceivedHandler(
        txn24Store,
        txn31Store,
        requestValidator,
        assetService,
        custodyService,
        custodyConfig,
        eventService);
  }

  @Bean
  NotifyOffchainFundsSentHandler notifyOffchainFundsSentHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService) {
    return new NotifyOffchainFundsSentHandler(
        txn24Store, txn31Store, requestValidator, assetService, eventService);
  }

  @Bean
  NotifyOnchainFundsReceivedHandler notifyOnchainFundsReceivedHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      Horizon horizon,
      AssetService assetService,
      EventService eventService) {
    return new NotifyOnchainFundsReceivedHandler(
        txn24Store, txn31Store, requestValidator, horizon, assetService, eventService);
  }

  @Bean
  NotifyOnchainFundsSentHandler notifyOnchainFundsSentHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      Horizon horizon,
      AssetService assetService,
      EventService eventService) {
    return new NotifyOnchainFundsSentHandler(
        txn24Store, txn31Store, requestValidator, horizon, assetService, eventService);
  }

  @Bean
  NotifyRefundPendingHandler notifyRefundPendingHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService) {
    return new NotifyRefundPendingHandler(
        txn24Store, txn31Store, requestValidator, assetService, eventService);
  }

  @Bean
  NotifyRefundSentHandler notifyRefundSentHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService) {
    return new NotifyRefundSentHandler(
        txn24Store, txn31Store, requestValidator, assetService, eventService);
  }

  @Bean
  NotifyTransactionErrorHandler notifyTransactionErrorHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService,
      JdbcTransactionPendingTrustRepo transactionPendingTrustRepo) {
    return new NotifyTransactionErrorHandler(
        txn24Store,
        txn31Store,
        requestValidator,
        assetService,
        eventService,
        transactionPendingTrustRepo);
  }

  @Bean
  NotifyTransactionExpiredHandler notifyTransactionExpiredHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService,
      JdbcTransactionPendingTrustRepo transactionPendingTrustRepo) {
    return new NotifyTransactionExpiredHandler(
        txn24Store,
        txn31Store,
        requestValidator,
        assetService,
        eventService,
        transactionPendingTrustRepo);
  }

  @Bean
  NotifyTransactionRecoveryHandler notifyTransactionRecoveryHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService) {
    return new NotifyTransactionRecoveryHandler(
        txn24Store, txn31Store, requestValidator, assetService, eventService);
  }

  @Bean
  NotifyTrustSetHandler notifyTrustSetHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService,
      PropertyCustodyConfig custodyConfig,
      CustodyService custodyService) {
    return new NotifyTrustSetHandler(
        txn24Store,
        txn31Store,
        requestValidator,
        assetService,
        eventService,
        custodyConfig,
        custodyService);
  }

  @Bean
  RequestCustomerInfoUpdateHandler requestCustomerInfoUpdateHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService) {
    return new RequestCustomerInfoUpdateHandler(
        txn24Store, txn31Store, requestValidator, assetService, eventService);
  }

  @Bean
  NotifyCustomerInfoUpdatedHandler notifyCustomerInfoUpdatedHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService) {
    return new NotifyCustomerInfoUpdatedHandler(
        txn24Store, txn31Store, requestValidator, assetService, eventService);
  }

  @Bean
  RequestOffchainFundsHandler requestOffchainFundsHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService) {
    return new RequestOffchainFundsHandler(
        txn24Store, txn31Store, requestValidator, assetService, eventService);
  }

  @Bean
  RequestOnchainFundsHandler requestOnchainFundsHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      CustodyService custodyService,
      CustodyConfig custodyConfig,
      Sep24DepositInfoGenerator sep24DepositInfoGenerator,
      EventService eventService) {
    return new RequestOnchainFundsHandler(
        txn24Store,
        txn31Store,
        requestValidator,
        assetService,
        custodyService,
        custodyConfig,
        sep24DepositInfoGenerator,
        eventService);
  }

  @Bean
  RequestTrustHandler requestTrustHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      CustodyConfig custodyConfig,
      EventService eventService) {
    return new RequestTrustHandler(
        txn24Store, txn31Store, requestValidator, assetService, custodyConfig, eventService);
  }
}
