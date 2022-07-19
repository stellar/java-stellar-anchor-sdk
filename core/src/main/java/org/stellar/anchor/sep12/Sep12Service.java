package org.stellar.anchor.sep12;

import static org.stellar.anchor.util.Log.infoF;

import java.util.Objects;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.stellar.anchor.api.callback.CustomerIntegration;
import org.stellar.anchor.api.exception.*;
import org.stellar.anchor.api.sep.sep12.*;
import org.stellar.anchor.auth.JwtToken;
import org.stellar.anchor.util.Log;
import org.stellar.anchor.util.MemoHelper;
import org.stellar.sdk.xdr.MemoType;

public class Sep12Service {
  private final CustomerIntegration customerIntegration;
  private final Sep12CustomerStore sep12CustomerStore;

  public Sep12Service(
      CustomerIntegration customerIntegration, Sep12CustomerStore sep12CustomerStore) {
    this.customerIntegration = customerIntegration;
    this.sep12CustomerStore = sep12CustomerStore;
    Log.info("Sep12Service initialized.");
  }

  public Sep12GetCustomerResponse getCustomer(JwtToken token, Sep12GetCustomerRequest request)
      throws AnchorException {
    validateGetOrPutRequest(request, token);
    if (request.getId() == null && request.getAccount() == null && token.getAccount() != null) {
      request.setAccount(token.getAccount());
    }

    Sep12GetCustomerResponse customerResponse = customerIntegration.getCustomer(request);

    if (customerResponse.getId() != null) {
      Sep12CustomerId customer = sep12CustomerStore.findById(customerResponse.getId());
      boolean shouldSave = false;
      if (customer == null) {
        customer =
            new Sep12CustomerBuilder(sep12CustomerStore)
                .id(customerResponse.getId())
                .account(request.getAccount())
                .memo(request.getMemo())
                .memoType(request.getMemoType())
                .build();
        sep12CustomerStore.save(customer);
      }
    }

    return customerResponse;
  }

  public Sep12PutCustomerResponse putCustomer(JwtToken token, Sep12PutCustomerRequest request)
      throws AnchorException {
    validateGetOrPutRequest(request, token);

    if (request.getAccount() == null && token.getAccount() != null) {
      request.setAccount(token.getAccount());
    }

    Sep12PutCustomerResponse putCustomerResponse = customerIntegration.putCustomer(request);

    if (putCustomerResponse.getId() != null) {
      Sep12CustomerId customer = sep12CustomerStore.findById(putCustomerResponse.getId());
      if (customer == null) {
        customer =
            new Sep12CustomerBuilder(sep12CustomerStore)
                .id(putCustomerResponse.getId())
                .account(request.getAccount())
                .memo(request.getMemo())
                .memoType(request.getMemoType())
                .build();
        sep12CustomerStore.save(customer);
      }
    }

    return putCustomerResponse;
  }

  public void deleteCustomer(JwtToken jwtToken, String account, String memo, String memoType)
      throws AnchorException {
    boolean isAccountAuthenticated =
        Stream.of(jwtToken.getAccount(), jwtToken.getMuxedAccount())
            .filter(Objects::nonNull)
            .anyMatch(tokenAccount -> Objects.equals(tokenAccount, account));

    boolean isMemoAuthenticated = memo == null;
    String muxedAccountId = Objects.toString(jwtToken.getMuxedAccountId(), null);
    if (muxedAccountId != null) {
      if (!Objects.equals(jwtToken.getMuxedAccount(), account)) {
        isMemoAuthenticated = Objects.equals(muxedAccountId, memo);
      }
    } else if (jwtToken.getAccountMemo() != null) {
      isMemoAuthenticated = Objects.equals(jwtToken.getAccountMemo(), memo);
    }

    if (!isAccountAuthenticated || !isMemoAuthenticated) {
      infoF("Requester ({}) not authorized to delete account ({})", jwtToken.getAccount(), account);
      throw new SepNotAuthorizedException(
          String.format("Not authorized to delete account [%s] with memo [%s]", account, memo));
    }

    // TODO: Move this into configuration instead of hardcoding customer type values.
    boolean existingCustomerMatch = false;
    String[] customerTypes = {"sending_user", "receiving_user"};
    for (String customerType : customerTypes) {
      Sep12GetCustomerResponse existingCustomer =
          customerIntegration.getCustomer(
              Sep12GetCustomerRequest.builder()
                  .account(account)
                  .memo(memo)
                  .memoType(memoType)
                  .type(customerType)
                  .build());
      String customerId = existingCustomer.getId();
      if (customerId != null) {
        existingCustomerMatch = true;
        customerIntegration.deleteCustomer(customerId);
        sep12CustomerStore.delete(customerId);
      }
    }
    if (!existingCustomerMatch) {
      infoF(
          "No existing customer found for account={} memo={} memoType={}", account, memo, memoType);
      throw new SepNotFoundException("User not found.");
    }
  }

  void validateGetOrPutRequest(Sep12CustomerRequestBase requestBase, JwtToken token)
      throws SepException {
    validateRequestAndTokenAccounts(requestBase, token);
    validateRequestAndTokenMemos(requestBase, token);
    updateRequestMemoAndMemoType(requestBase, token);
  }

  void validateRequestAndTokenAccounts(
      @NotNull Sep12CustomerRequestBase requestBase, @NotNull JwtToken token) throws SepException {
    // Validate request.account - SEP-12 says: This field should match the `sub` value of the
    // decoded SEP-10 JWT.
    String tokenAccount = token.getAccount();
    String tokenMuxedAccount = token.getMuxedAccount();
    String customerAccount = requestBase.getAccount();
    if (customerAccount != null
        && Stream.of(tokenAccount, tokenMuxedAccount).noneMatch(customerAccount::equals)) {
      infoF(
          "Neither tokenAccount ({}) nor tokenMuxedAccount ({}) match customerAccount ({})",
          tokenAccount,
          tokenMuxedAccount,
          customerAccount);
      throw new SepNotAuthorizedException(
          "The account specified does not match authorization token");
    }
  }

  void validateRequestAndTokenMemos(Sep12CustomerRequestBase requestBase, @NotNull JwtToken token)
      throws SepException {
    String tokenSubMemo = token.getAccountMemo();
    String tokenMuxedAccountId = Objects.toString(token.getMuxedAccountId(), null);
    String tokenMemo = tokenMuxedAccountId != null ? tokenMuxedAccountId : tokenSubMemo;
    // SEP-12 says: If the JWT's `sub` field does not contain a muxed account or memo then the memo
    // request parameters may contain any value.
    if (tokenMemo == null) {
      return;
    }

    // SEP-12 says: If a memo is present in the decoded SEP-10 JWT's `sub` value, it must match this
    // parameter value. If a muxed account is used as the JWT's `sub` value, memos sent in requests
    // must match the 64-bit integer subaccount ID of the muxed account. See the Shared Account's
    // section for more information.
    String requestMemo = requestBase.getMemo();
    if (Objects.equals(tokenMemo, requestMemo)) {
      return;
    }

    infoF(
        "request memo ({}) does not match token memo ID ({}) authorized via SEP-10",
        requestMemo,
        tokenMemo);
    throw new SepNotAuthorizedException(
        "The memo specified does not match the memo ID authorized via SEP-10");
  }

  void updateRequestMemoAndMemoType(@NotNull Sep12CustomerRequestBase requestBase, JwtToken token)
      throws SepException {
    String memo = requestBase.getMemo();
    if (memo == null) {
      requestBase.setMemoType(null);
      return;
    }
    String memoTypeId = MemoHelper.memoTypeAsString(MemoType.MEMO_ID);
    String memoType = Objects.toString(requestBase.getMemoType(), memoTypeId);
    // SEP-12 says: If a memo is present in the decoded SEP-10 JWT's `sub` value, this parameter
    // (memoType) can be ignored:
    if (token.getAccountMemo() != null || token.getMuxedAccountId() != null) {
      memoType = MemoHelper.memoTypeAsString(MemoType.MEMO_ID);
    }

    try {
      MemoHelper.makeMemo(memo, memoType);
    } catch (Exception e) {
      infoF("Invalid memo ({}) for memo_type ({})", memo, memoType);
      Log.warnEx(e);
      throw new SepValidationException("Invalid 'memo' for 'memo_type'");
    }

    requestBase.setMemo(memo);
    requestBase.setMemoType(memoType);
  }
}
