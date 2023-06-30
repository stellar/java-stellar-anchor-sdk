package org.stellar.anchor.platform.action.handlers;

import static org.stellar.anchor.api.platform.PlatformTransactionData.Kind.WITHDRAWAL;
import static org.stellar.anchor.api.sep.SepTransactionStatus.INCOMPLETE;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_RECEIVER;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_SENDER;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_USR_TRANSFER_START;
import static org.stellar.anchor.platform.action.dto.ActionMethod.REQUEST_ONCHAIN_FUNDS;

import java.util.HashSet;
import java.util.Set;
import javax.validation.Validator;
import org.springframework.stereotype.Service;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.api.shared.SepDepositInfo;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.CustodyConfig;
import org.stellar.anchor.custody.CustodyService;
import org.stellar.anchor.platform.action.dto.ActionMethod;
import org.stellar.anchor.platform.action.dto.RequestOnchainFundsRequest;
import org.stellar.anchor.platform.config.PropertySep24Config;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSep31Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.service.Sep24DepositInfoNoneGenerator;
import org.stellar.anchor.sep24.Sep24DepositInfoGenerator;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

@Service
public class RequestOnchainFundsHandler extends ActionHandler<RequestOnchainFundsRequest> {

  private final CustodyConfig custodyConfig;
  private final CustodyService custodyService;
  private final Sep24DepositInfoGenerator sep24DepositInfoGenerator;

  public RequestOnchainFundsHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      AssetService assetService,
      CustodyConfig custodyConfig,
      CustodyService custodyService,
      Sep24DepositInfoGenerator sep24DepositInfoGenerator,
      PropertySep24Config propertySep24Config) {
    super(txn24Store, txn31Store, validator, assetService);
    this.custodyConfig = custodyConfig;
    this.custodyService = custodyService;
    this.sep24DepositInfoGenerator = sep24DepositInfoGenerator;
  }

  @Override
  public ActionMethod getActionType() {
    return REQUEST_ONCHAIN_FUNDS;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, RequestOnchainFundsRequest request) {
    switch (txn.getProtocol()) {
      case "24":
        return PENDING_USR_TRANSFER_START;
      case "31":
        return PENDING_SENDER;
      default:
        throw new IllegalArgumentException(
            String.format(
                "Invalid protocol[%s] for action[%s]", txn.getProtocol(), getActionType()));
    }
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    Set<SepTransactionStatus> supportedStatuses = new HashSet<>();

    if (txn.getTransferReceivedAt() == null) {
      switch (txn.getProtocol()) {
        case "24":
          JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
          switch (Kind.from(txn24.getKind())) {
            case WITHDRAWAL:
              supportedStatuses.add(INCOMPLETE);
              if (txn24.getTransferReceivedAt() == null) {
                supportedStatuses.add(PENDING_ANCHOR);
              }
          }
          break;
        case "31":
          supportedStatuses.add(INCOMPLETE);
          if (txn.getTransferReceivedAt() == null) {
            supportedStatuses.add(PENDING_RECEIVER);
          }
          break;
      }
    }

    return supportedStatuses;
  }

  @Override
  protected Set<String> getSupportedProtocols() {
    return Set.of("24", "31");
  }

  @Override
  protected boolean isMessageRequired() {
    return false;
  }

  @Override
  protected void updateTransactionWithAction(
      JdbcSepTransaction txn, RequestOnchainFundsRequest request) throws AnchorException {
    validateAsset("amount_in", request.getAmountIn());
    validateAsset("amount_out", request.getAmountOut());
    validateAsset("amount_fee", request.getAmountFee(), true);
    validateAsset("amount_expected", request.getAmountOut());

    if (txn.getAmountIn() == null) {
      if (request.getAmountIn() != null) {
        txn.setAmountIn(request.getAmountIn().getAmount());
        txn.setAmountInAsset(request.getAmountIn().getAsset());
      } else if (txn.getAmountIn() == null) {
        throw new BadRequestException("amount_in is required");
      }
    }

    if (txn.getAmountOut() == null) {
      if (request.getAmountOut() != null) {
        txn.setAmountIn(request.getAmountOut().getAmount());
        txn.setAmountInAsset(request.getAmountOut().getAsset());
      } else if (txn.getAmountOut() == null) {
        throw new BadRequestException("amount_out is required");
      }
    }

    if (txn.getAmountFee() == null) {
      if (request.getAmountFee() != null) {
        txn.setAmountIn(request.getAmountFee().getAmount());
        txn.setAmountInAsset(request.getAmountFee().getAsset());
      } else if (txn.getAmountFee() == null) {
        throw new BadRequestException("amount_fee is required");
      }
    }

    if ("24".equals(txn.getProtocol())) {
      JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;

      if (txn24.getAmountExpected() == null) {
        if (request.getAmountExpected() != null) {
          txn24.setAmountExpected(request.getAmountExpected().getAmount());
        } else if (txn24.getAmountFee() == null) {
          txn24.setAmountExpected(txn.getAmountIn());
        }
      }

      if (request.getDestinationAccount() != null) {
        txn24.setWithdrawAnchorAccount(request.getDestinationAccount());
        txn24.setToAccount(request.getDestinationAccount());
      }

      if (sep24DepositInfoGenerator instanceof Sep24DepositInfoNoneGenerator) {
        if (request.getMemo() != null) {
          txn24.setMemo(request.getMemo());
        } else {
          throw new BadRequestException("memo is required");
        }

        if (request.getMemoType() != null) {
          txn24.setMemo(request.getMemoType());
        } else {
          throw new BadRequestException("memo_type is required");
        }

        if (request.getDestinationAccount() != null) {
          txn24.setWithdrawAnchorAccount(request.getDestinationAccount());
        } else {
          throw new BadRequestException("destination_account is required");
        }
      } else {
        SepDepositInfo sep24DepositInfo = sep24DepositInfoGenerator.generate(txn24);
        txn24.setToAccount(sep24DepositInfo.getStellarAddress());
        txn24.setWithdrawAnchorAccount(sep24DepositInfo.getStellarAddress());
        txn24.setMemo(sep24DepositInfo.getMemo());
        txn24.setMemoType(sep24DepositInfo.getMemoType());
      }

      if (custodyConfig.isCustodyIntegrationEnabled()) {
        custodyService.createTransaction(txn24);
      }
    } else if ("31".equals(txn.getProtocol())) {
      JdbcSep31Transaction txn31 = (JdbcSep31Transaction) txn;

      if (txn31.getAmountExpected() == null) {
        if (request.getAmountExpected() != null) {
          txn31.setAmountExpected(request.getAmountExpected().getAmount());
        } else if (txn31.getAmountFee() == null) {
          txn31.setAmountExpected(txn.getAmountIn());
        }
      }
    }
  }
}
