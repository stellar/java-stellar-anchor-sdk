package org.stellar.anchor.model;

import org.stellar.anchor.sep24.Sep24TransactionStore;

@SuppressWarnings("unused")
public class Sep24TransactionBuilder {
  final Sep24Transaction txn;

  public Sep24TransactionBuilder(Sep24TransactionStore factory) {
    txn = factory.newInstance();
  }

  public Sep24TransactionBuilder transactionId(String txnId) {
    txn.setTransactionId(txnId);
    return this;
  }

  public Sep24TransactionBuilder status(String status) {
    txn.setStatus(status);
    return this;
  }

  public Sep24TransactionBuilder kind(String kind) {
    txn.setKind(kind);
    return this;
  }

  public Sep24TransactionBuilder assetCode(String assetCode) {
    txn.setAssetCode(assetCode);
    return this;
  }

  public Sep24TransactionBuilder assetIssuer(String assetIssuer) {
    txn.setAssetIssuer(assetIssuer);
    return this;
  }

  public Sep24TransactionBuilder startedAt(long time) {
    txn.setStartedAt(time);
    return this;
  }

  public Sep24TransactionBuilder completedAt(long time) {
    txn.setCompletedAt(time);
    return this;
  }

  public Sep24TransactionBuilder stellarAccount(String stellarAccount) {
    txn.setStellarAccount(stellarAccount);
    return this;
  }

  public Sep24TransactionBuilder receivingAnchorAccount(String receivingAnchorAccount) {
    txn.setReceivingAnchorAccount(receivingAnchorAccount);
    return this;
  }

  public Sep24TransactionBuilder fromAccount(String fromAccount) {
    txn.setFromAccount(fromAccount);
    return this;
  }

  public Sep24TransactionBuilder toAccount(String toAccount) {
    txn.setToAccount(toAccount);
    return this;
  }

  public Sep24TransactionBuilder memoType(String memoType) {
    txn.setMemoType(memoType);
    return this;
  }

  public Sep24TransactionBuilder memo(String memo) {
    txn.setMemo(memo);
    return this;
  }

  public Sep24TransactionBuilder protocol(String protocol) {
    txn.setProtocol(protocol);
    return this;
  }

  public Sep24TransactionBuilder domainClient(String domainClient) {
    txn.setDomainClient(domainClient);
    return this;
  }

  public Sep24TransactionBuilder claimableBalanceSupported(Boolean claimableBalanceSupported) {
    txn.setClaimableBalanceSupported(claimableBalanceSupported);
    return this;
  }

  public Sep24TransactionBuilder amountIn(String amountIn) {
    txn.setAmountIn(amountIn);
    return this;
  }

  public Sep24TransactionBuilder amountOut(String amountOut) {
    txn.setAmountOut(amountOut);
    return this;
  }

  public Sep24TransactionBuilder amountFee(String amountFee) {
    txn.setAmountFee(amountFee);
    return this;
  }

  public Sep24TransactionBuilder amountInAsset(String amountInAsset) {
    txn.setAmountInAsset(amountInAsset);
    return this;
  }

  public Sep24TransactionBuilder amountOutAsset(String amountOutAsset) {
    txn.setAmountOutAsset(amountOutAsset);
    return this;
  }

  public Sep24TransactionBuilder amountFeeAsset(String amountFeeAsset) {
    txn.setAmountFeeAsset(amountFeeAsset);
    return this;
  }

  public Sep24Transaction build() {
    return txn;
  }
}
