package org.stellar.anchor.sep12;

import java.util.stream.Stream;
import org.stellar.anchor.dto.sep12.GetCustomerRequest;
import org.stellar.anchor.dto.sep12.GetCustomerResponse;
import org.stellar.anchor.dto.sep12.PutCustomerRequest;
import org.stellar.anchor.dto.sep12.PutCustomerResponse;
import org.stellar.anchor.exception.SepException;
import org.stellar.anchor.exception.SepNotAuthorizedException;
import org.stellar.anchor.exception.SepValidationException;
import org.stellar.anchor.integration.customer.CustomerIntegration;
import org.stellar.anchor.sep10.JwtToken;
import org.stellar.anchor.util.MemoHelper;
import org.stellar.sdk.xdr.MemoType;
import reactor.core.publisher.Mono;

public class Sep12Service {
  private final CustomerIntegration customerIntegration;

  public Sep12Service(CustomerIntegration customerIntegration) {
    this.customerIntegration = customerIntegration;
  }

  public Mono<GetCustomerResponse> getCustomer(JwtToken token, GetCustomerRequest request)
      throws SepException {
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
    return customerIntegration.getCustomer(request);
  }

  public Mono<PutCustomerResponse> putCustomer(JwtToken token, PutCustomerRequest request)
      throws SepException {
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
    return customerIntegration.putCustomer(request);
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
      throw new SepValidationException("Invalid 'memo' for 'memo_type'");
    }
  }

  void validateIdXorMemoIsPresent(String id, String account, String memo, String memoType)
      throws SepException {
    if (id != null) {
      if (account != null || memo != null || memoType != null) {
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
      throw new SepNotAuthorizedException(
          "The account specified does not match authorization token");
    }
    if (tokenMemo != null && requestMemo != null) {
      if (!tokenMemo.equals(requestMemo) || !requestMemoType.equals(MemoType.MEMO_ID.name())) {
        throw new SepNotAuthorizedException(
            "The memo specified does not match the memo ID authorized via SEP-10");
      }
    }
  }
}
