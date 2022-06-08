package org.stellar.anchor.sep12;

import java.util.stream.Stream;
import org.stellar.anchor.api.callback.CustomerIntegration;
import org.stellar.anchor.api.exception.*;
import org.stellar.anchor.api.sep.sep12.Sep12GetCustomerRequest;
import org.stellar.anchor.api.sep.sep12.Sep12GetCustomerResponse;
import org.stellar.anchor.api.sep.sep12.Sep12PutCustomerRequest;
import org.stellar.anchor.api.sep.sep12.Sep12PutCustomerResponse;
import org.stellar.anchor.sep10.JwtToken;
import org.stellar.anchor.util.MemoHelper;
import org.stellar.sdk.xdr.MemoType;

import static org.stellar.anchor.util.Log.debugF;
import static org.stellar.anchor.util.Log.infoF;

public class Sep12Service {
  private final CustomerIntegration customerIntegration;

  public Sep12Service(CustomerIntegration customerIntegration) {
    this.customerIntegration = customerIntegration;
  }

  public Sep12GetCustomerResponse getCustomer(JwtToken token, Sep12GetCustomerRequest request)
      throws AnchorException {
    validateGetOrPutRequest(
        request.getId(),
        request.getAccount(),
        request.getMemo(),
        request.getMemoType(),
        token.getAccount(),
        token.getMuxedAccount(),
        token.getAccountMemo());
    request.setMemo(getMemoForCustomerIntegration(request.getMemo(), token.getAccountMemo()));
    request.setMemoType(
        getMemoTypeForCustomerIntegration(request.getMemo(), request.getMemoType()));
    if (request.getId() == null && request.getAccount() == null && token.getAccount() != null) {
      request.setAccount(token.getAccount());
    }
    return customerIntegration.getCustomer(request);
  }

  public Sep12PutCustomerResponse putCustomer(JwtToken token, Sep12PutCustomerRequest request)
      throws AnchorException {
    validateGetOrPutRequest(
        request.getId(),
        request.getAccount(),
        request.getMemo(),
        request.getMemoType(),
        token.getAccount(),
        token.getMuxedAccount(),
        token.getAccountMemo());
    request.setMemo(getMemoForCustomerIntegration(request.getMemo(), token.getAccountMemo()));
    request.setMemoType(
        getMemoTypeForCustomerIntegration(request.getMemo(), request.getMemoType()));
    if (request.getAccount() == null && token.getAccount() != null) {
      request.setAccount(token.getAccount());
    }
    return customerIntegration.putCustomer(request);
  }

  public void deleteCustomer(JwtToken jwtToken, String account, String memo, String memoType)
      throws AnchorException {
    if (!jwtToken.getAccount().equals(account)) {
      infoF("Requester ({}) not authorized to delete account ({})", jwtToken.getAccount(), account);
      throw new SepNotAuthorizedException(
          String.format("Not authorized to delete account [%s]", account));
    }

    Sep12GetCustomerResponse existingCustomer =
        customerIntegration.getCustomer(
            Sep12GetCustomerRequest.builder()
                .account(account)
                .memo(memo)
                .memoType(memoType)
                .build());
    if (existingCustomer.getId() == null) {
      infoF("No existing customer found for account={} memo={} memoType={}", account, memo, memoType);
      throw new SepNotFoundException("User not found.");
    }

    customerIntegration.deleteCustomer(existingCustomer.getId());
  }

  void validateGetOrPutRequest(
      String customerId,
      String requestAccount,
      String requestMemo,
      String requestMemoType,
      String tokenAccount,
      String tokenMuxedAccount,
      String tokenMemo)
      throws SepException {
    validateIdXorMemoIsPresent(customerId, requestAccount, requestMemo, requestMemoType);
    validateMemoRequestAndTokenValuesMatch(
        requestAccount, requestMemo, requestMemoType, tokenAccount, tokenMuxedAccount, tokenMemo);
    validateMemo(requestMemo, requestMemoType);
  }

  String getMemoTypeForCustomerIntegration(String memo, String requestMemoType) {
    String memoType = null;
    if (memo != null) {
      memoType = (requestMemoType != null) ? requestMemoType : MemoType.MEMO_ID.name();
    }
    return memoType;
  }

  String getMemoForCustomerIntegration(String requestMemo, String tokenMemo) {
    return requestMemo != null ? requestMemo : tokenMemo;
  }

  void validateMemo(String memo, String memoType) throws SepException {
    try {
      MemoHelper.makeMemo(memo, memoType);
    } catch (SepException e) {
      infoF("Invalid memo ({}) for memo_type ({})", memo, memoType);
      throw new SepValidationException("Invalid 'memo' for 'memo_type'");
    }
  }

  void validateIdXorMemoIsPresent(String id, String account, String memo, String memoType)
      throws SepException {
    if (id != null) {
      if (account != null || memo != null || memoType != null) {
        infoF("Request with id ({}) should not have 'account', 'memo', or 'memo_type'", id);
        throw new SepValidationException(
            "A requests with 'id' cannot also have 'account', 'memo', or 'memo_type'");
      }
    }
  }

  void validateMemoRequestAndTokenValuesMatch(
      String requestAccount,
      String requestMemo,
      String requestMemoType,
      String tokenAccount,
      String tokenMuxedAccount,
      String tokenMemo)
      throws SepException {
    if (requestAccount != null
        && Stream.of(tokenAccount, tokenMuxedAccount).noneMatch(requestAccount::equals)) {
      infoF("Neither tokenAccount ({}) nor tokenMuxedAccount ({}) match requestAccount ({})", tokenAccount,
              tokenMuxedAccount, requestAccount);
      throw new SepNotAuthorizedException(
          "The account specified does not match authorization token");
    }
    if (tokenMemo != null && requestMemo != null) {
      if (!tokenMemo.equals(requestMemo) || !requestMemoType.equals(MemoType.MEMO_ID.name())) {
        infoF("request memo ({}) does not match token memo ID ({}) authorized via SEP-10", requestMemo, tokenMemo);
        throw new SepNotAuthorizedException(
            "The memo specified does not match the memo ID authorized via SEP-10");
      }
    }
  }
}
