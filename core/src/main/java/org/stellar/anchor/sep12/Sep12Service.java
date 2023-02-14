package org.stellar.anchor.sep12;

import static org.stellar.anchor.util.Log.infoF;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.stellar.anchor.api.callback.CustomerIntegration;
import org.stellar.anchor.api.exception.*;
import org.stellar.anchor.api.sep.sep12.*;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.auth.JwtToken;
import org.stellar.anchor.util.Log;
import org.stellar.anchor.util.MemoHelper;
import org.stellar.sdk.xdr.MemoType;

public class Sep12Service {
  private final CustomerIntegration customerIntegration;
  private final Set<String> knownTypes;

  public Sep12Service(CustomerIntegration customerIntegration, AssetService assetService) {
    this.customerIntegration = customerIntegration;
    Stream<String> receiverTypes =
        assetService.listAllAssets().stream()
            .filter(x -> x.getSep31() != null)
            .flatMap(x -> x.getSep31().getSep12().getReceiver().getTypes().keySet().stream());
    Stream<String> senderTypes =
        assetService.listAllAssets().stream()
            .filter(x -> x.getSep31() != null)
            .flatMap(x -> x.getSep31().getSep12().getSender().getTypes().keySet().stream());
    knownTypes = Stream.concat(receiverTypes, senderTypes).collect(Collectors.toSet());

    Log.info("Sep12Service initialized.");
  }

  public Sep12GetCustomerResponse getCustomer(JwtToken token, Sep12GetCustomerRequest request)
      throws AnchorException {
    validateGetOrPutRequest(request, token);
    if (request.getId() == null && request.getAccount() == null && token.getAccount() != null) {
      request.setAccount(token.getAccount());
    }

    return customerIntegration.getCustomer(request);
  }

  public Sep12PutCustomerResponse putCustomer(JwtToken token, Sep12PutCustomerRequest request)
      throws AnchorException {
    validateGetOrPutRequest(request, token);

    if (request.getAccount() == null && token.getAccount() != null) {
      request.setAccount(token.getAccount());
    }

    return customerIntegration.putCustomer(request);
  }

  public void deleteCustomer(JwtToken jwtToken, String account, String memo, String memoType)
      throws AnchorException {
    boolean isAccountAuthenticated =
        Stream.of(jwtToken.getAccount(), jwtToken.getMuxedAccount())
            .filter(Objects::nonNull)
            .anyMatch(tokenAccount -> Objects.equals(tokenAccount, account));

    boolean isMemoMissingAuthentication = false;
    String muxedAccountId = Objects.toString(jwtToken.getMuxedAccountId(), null);
    if (muxedAccountId != null) {
      if (!Objects.equals(jwtToken.getMuxedAccount(), account)) {
        isMemoMissingAuthentication = !Objects.equals(muxedAccountId, memo);
      }
    } else if (jwtToken.getAccountMemo() != null) {
      isMemoMissingAuthentication = !Objects.equals(jwtToken.getAccountMemo(), memo);
    }

    if (!isAccountAuthenticated || isMemoMissingAuthentication) {
      infoF("Requester ({}) not authorized to delete account ({})", jwtToken.getAccount(), account);
      throw new SepNotAuthorizedException(
          String.format("Not authorized to delete account [%s] with memo [%s]", account, memo));
    }

    boolean existingCustomerMatch = false;
    for (String customerType : knownTypes) {
      Sep12GetCustomerResponse existingCustomer =
          customerIntegration.getCustomer(
              Sep12GetCustomerRequest.builder()
                  .account(account)
                  .memo(memo)
                  .memoType(memoType)
                  .type(customerType)
                  .build());
      if (existingCustomer.getId() != null) {
        existingCustomerMatch = true;
        customerIntegration.deleteCustomer(existingCustomer.getId());
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
