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
import org.stellar.anchor.platform.action.NotifyInteractiveFlowCompletedHandler;
import org.stellar.anchor.platform.action.NotifyOffchainFundsReceivedHandler;
import org.stellar.anchor.platform.action.NotifyOffchainFundsSentHandler;
import org.stellar.anchor.platform.action.NotifyOnchainFundsReceivedHandler;
import org.stellar.anchor.platform.action.RequestOffchainFundsHandler;
import org.stellar.anchor.platform.action.RequestOnchainFundsHandler;
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
}
