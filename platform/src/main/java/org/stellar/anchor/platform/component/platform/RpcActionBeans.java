package org.stellar.anchor.platform.component.platform;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.CustodyConfig;
import org.stellar.anchor.custody.CustodyService;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.metrics.MetricsService;
import org.stellar.anchor.platform.config.PropertyCustodyConfig;
import org.stellar.anchor.platform.config.RpcConfig;
import org.stellar.anchor.platform.data.JdbcTransactionPendingTrustRepo;
import org.stellar.anchor.platform.rpc.DoStellarPaymentHandler;
import org.stellar.anchor.platform.rpc.DoStellarRefundHandler;
import org.stellar.anchor.platform.rpc.NotifyAmountsUpdatedHandler;
import org.stellar.anchor.platform.rpc.NotifyCustomerInfoUpdatedHandler;
import org.stellar.anchor.platform.rpc.NotifyInteractiveFlowCompletedHandler;
import org.stellar.anchor.platform.rpc.NotifyOffchainFundsAvailableHandler;
import org.stellar.anchor.platform.rpc.NotifyOffchainFundsPendingHandler;
import org.stellar.anchor.platform.rpc.NotifyOffchainFundsReceivedHandler;
import org.stellar.anchor.platform.rpc.NotifyOffchainFundsSentHandler;
import org.stellar.anchor.platform.rpc.NotifyOnchainFundsReceivedHandler;
import org.stellar.anchor.platform.rpc.NotifyOnchainFundsSentHandler;
import org.stellar.anchor.platform.rpc.NotifyRefundPendingHandler;
import org.stellar.anchor.platform.rpc.NotifyRefundSentHandler;
import org.stellar.anchor.platform.rpc.NotifyTransactionErrorHandler;
import org.stellar.anchor.platform.rpc.NotifyTransactionExpiredHandler;
import org.stellar.anchor.platform.rpc.NotifyTransactionRecoveryHandler;
import org.stellar.anchor.platform.rpc.NotifyTrustSetHandler;
import org.stellar.anchor.platform.rpc.RequestCustomerInfoUpdateHandler;
import org.stellar.anchor.platform.rpc.RequestOffchainFundsHandler;
import org.stellar.anchor.platform.rpc.RequestOnchainFundsHandler;
import org.stellar.anchor.platform.rpc.RequestTrustlineHandler;
import org.stellar.anchor.platform.rpc.RpcMethodHandler;
import org.stellar.anchor.platform.service.RpcService;
import org.stellar.anchor.platform.validator.RequestValidator;
import org.stellar.anchor.sep24.Sep24DepositInfoGenerator;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

@Configuration
public class RpcActionBeans {

  @Bean
  RpcService rpcService(List<RpcMethodHandler<?>> rpcMethodHandlers, RpcConfig rpcConfig) {
    return new RpcService(rpcMethodHandlers, rpcConfig);
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
      MetricsService metricsService,
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
        metricsService,
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
      EventService eventService,
      MetricsService metricsService) {
    return new DoStellarRefundHandler(
        txn24Store,
        txn31Store,
        requestValidator,
        custodyConfig,
        assetService,
        custodyService,
        eventService,
        metricsService);
  }

  @Bean
  NotifyAmountsUpdatedHandler notifyAmountsUpdatedHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService,
      MetricsService metricsService) {
    return new NotifyAmountsUpdatedHandler(
        txn24Store, txn31Store, requestValidator, assetService, eventService, metricsService);
  }

  @Bean
  NotifyInteractiveFlowCompletedHandler notifyInteractiveFlowCompletedHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService,
      MetricsService metricsService) {
    return new NotifyInteractiveFlowCompletedHandler(
        txn24Store, txn31Store, requestValidator, assetService, eventService, metricsService);
  }

  @Bean
  NotifyOffchainFundsAvailableHandler notifyOffchainFundsAvailableHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService,
      MetricsService metricsService) {
    return new NotifyOffchainFundsAvailableHandler(
        txn24Store, txn31Store, requestValidator, assetService, eventService, metricsService);
  }

  @Bean
  NotifyOffchainFundsPendingHandler notifyOffchainFundsPendingHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService,
      MetricsService metricsService) {
    return new NotifyOffchainFundsPendingHandler(
        txn24Store, txn31Store, requestValidator, assetService, eventService, metricsService);
  }

  @Bean
  NotifyOffchainFundsReceivedHandler notifyOffchainFundsReceivedHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      CustodyService custodyService,
      CustodyConfig custodyConfig,
      EventService eventService,
      MetricsService metricsService) {
    return new NotifyOffchainFundsReceivedHandler(
        txn24Store,
        txn31Store,
        requestValidator,
        assetService,
        custodyService,
        custodyConfig,
        eventService,
        metricsService);
  }

  @Bean
  NotifyOffchainFundsSentHandler notifyOffchainFundsSentHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService,
      MetricsService metricsService) {
    return new NotifyOffchainFundsSentHandler(
        txn24Store, txn31Store, requestValidator, assetService, eventService, metricsService);
  }

  @Bean
  NotifyOnchainFundsReceivedHandler notifyOnchainFundsReceivedHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      Horizon horizon,
      AssetService assetService,
      EventService eventService,
      MetricsService metricsService) {
    return new NotifyOnchainFundsReceivedHandler(
        txn24Store,
        txn31Store,
        requestValidator,
        horizon,
        assetService,
        eventService,
        metricsService);
  }

  @Bean
  NotifyOnchainFundsSentHandler notifyOnchainFundsSentHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      Horizon horizon,
      AssetService assetService,
      EventService eventService,
      MetricsService metricsService) {
    return new NotifyOnchainFundsSentHandler(
        txn24Store,
        txn31Store,
        requestValidator,
        horizon,
        assetService,
        eventService,
        metricsService);
  }

  @Bean
  NotifyRefundPendingHandler notifyRefundPendingHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService,
      MetricsService metricsService) {
    return new NotifyRefundPendingHandler(
        txn24Store, txn31Store, requestValidator, assetService, eventService, metricsService);
  }

  @Bean
  NotifyRefundSentHandler notifyRefundSentHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService,
      MetricsService metricsService) {
    return new NotifyRefundSentHandler(
        txn24Store, txn31Store, requestValidator, assetService, eventService, metricsService);
  }

  @Bean
  NotifyTransactionErrorHandler notifyTransactionErrorHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService,
      MetricsService metricsService,
      JdbcTransactionPendingTrustRepo transactionPendingTrustRepo) {
    return new NotifyTransactionErrorHandler(
        txn24Store,
        txn31Store,
        requestValidator,
        assetService,
        eventService,
        metricsService,
        transactionPendingTrustRepo);
  }

  @Bean
  NotifyTransactionExpiredHandler notifyTransactionExpiredHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService,
      MetricsService metricsService,
      JdbcTransactionPendingTrustRepo transactionPendingTrustRepo) {
    return new NotifyTransactionExpiredHandler(
        txn24Store,
        txn31Store,
        requestValidator,
        assetService,
        eventService,
        metricsService,
        transactionPendingTrustRepo);
  }

  @Bean
  NotifyTransactionRecoveryHandler notifyTransactionRecoveryHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService,
      MetricsService metricsService) {
    return new NotifyTransactionRecoveryHandler(
        txn24Store, txn31Store, requestValidator, assetService, eventService, metricsService);
  }

  @Bean
  NotifyTrustSetHandler notifyTrustSetHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService,
      MetricsService metricsService,
      PropertyCustodyConfig custodyConfig,
      CustodyService custodyService) {
    return new NotifyTrustSetHandler(
        txn24Store,
        txn31Store,
        requestValidator,
        assetService,
        eventService,
        metricsService,
        custodyConfig,
        custodyService);
  }

  @Bean
  RequestCustomerInfoUpdateHandler requestCustomerInfoUpdateHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService,
      MetricsService metricsService) {
    return new RequestCustomerInfoUpdateHandler(
        txn24Store, txn31Store, requestValidator, assetService, eventService, metricsService);
  }

  @Bean
  NotifyCustomerInfoUpdatedHandler notifyCustomerInfoUpdatedHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService,
      MetricsService metricsService) {
    return new NotifyCustomerInfoUpdatedHandler(
        txn24Store, txn31Store, requestValidator, assetService, eventService, metricsService);
  }

  @Bean
  RequestOffchainFundsHandler requestOffchainFundsHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService,
      MetricsService metricsService) {
    return new RequestOffchainFundsHandler(
        txn24Store, txn31Store, requestValidator, assetService, eventService, metricsService);
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
      EventService eventService,
      MetricsService metricsService) {
    return new RequestOnchainFundsHandler(
        txn24Store,
        txn31Store,
        requestValidator,
        assetService,
        custodyService,
        custodyConfig,
        sep24DepositInfoGenerator,
        eventService,
        metricsService);
  }

  @Bean
  RequestTrustlineHandler requestTrustHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      CustodyConfig custodyConfig,
      EventService eventService,
      MetricsService metricsService) {
    return new RequestTrustlineHandler(
        txn24Store,
        txn31Store,
        requestValidator,
        assetService,
        custodyConfig,
        eventService,
        metricsService);
  }
}
