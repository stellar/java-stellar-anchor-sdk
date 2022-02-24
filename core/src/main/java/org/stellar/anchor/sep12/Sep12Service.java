package org.stellar.anchor.sep12;

import java.util.stream.Stream;
import org.apache.http.HttpStatus;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.Sep12Config;
import org.stellar.anchor.dto.sep12.GetCustomerRequest;
import org.stellar.anchor.dto.sep12.GetCustomerResponse;
import org.stellar.anchor.dto.sep12.PutCustomerRequest;
import org.stellar.anchor.dto.sep12.PutCustomerResponse;
import org.stellar.anchor.exception.SepException;
import org.stellar.anchor.exception.SepValidationException;
import org.stellar.anchor.integration.customer.CustomerIntegration;
import org.stellar.anchor.sep10.JwtService;
import org.stellar.anchor.sep10.JwtToken;
import org.stellar.anchor.util.MemoHelper;
import org.stellar.sdk.xdr.MemoType;
import reactor.core.publisher.Mono;

public class Sep12Service {
  private final AppConfig appConfig;
  private Sep12Config sep12Config;
  private JwtService jwtService;
  private CustomerIntegration customerIntegration;

  public Sep12Service(
      AppConfig appConfig,
      Sep12Config sep12Config,
      JwtService jwtService,
      CustomerIntegration customerIntegration) {
    this.appConfig = appConfig;
    this.sep12Config = sep12Config;
    this.jwtService = jwtService;
    this.customerIntegration = customerIntegration;
  }

  public Mono<GetCustomerResponse> getCustomer(JwtToken token, GetCustomerRequest request)
      throws SepValidationException {
    if (request.getAccount() != null
        && Stream.of(token.getAccount(), token.getMuxedAccount())
            .noneMatch(it -> request.getAccount().equals(it))) {
      throw new SepValidationException(
          HttpStatus.SC_FORBIDDEN, "The account specified does not match authorization token");
    }

    if (token.getAccountMemo() != null && request.getMemo() != null) {
      if (!token.getAccountMemo().equals(request.getMemo())
          || !request.getMemoType().equals(MemoType.MEMO_ID.name())) {
        throw new SepValidationException(
            HttpStatus.SC_FORBIDDEN,
            "The memo specified does not match the memo ID authorized via SEP-10");
      }
    }

    if (request.getId() != null) {
      if (request.getAccount() != null
          || request.getMemo() != null
          || request.getMemoType() != null) {
        throw new SepValidationException(
            HttpStatus.SC_BAD_REQUEST,
            "A requests with 'id' cannot also have 'account', 'memo', or 'memo_type'");
      }
    }

    try {
      MemoHelper.makeMemo(request.getMemo(), request.getMemoType());
    } catch (SepException e) {
      throw new SepValidationException(HttpStatus.SC_BAD_REQUEST, "Invalid 'memo' for 'memo_type'");
    }

    String memo = (request.getMemo() != null) ? request.getMemo() : token.getAccountMemo();
    String memoType = null;
    if (memo != null) {
      memoType = (request.getMemoType() != null) ? request.getMemoType() : MemoType.MEMO_ID.name();
    }

    request.setMemo(memo);
    request.setMemoType(memoType);

    return customerIntegration.getCustomer(request);
  }

  public Mono<PutCustomerResponse> putCustomer(JwtToken token, PutCustomerRequest request)
      throws SepValidationException {
    if (request.getId() != null) {
      if (request.getAccount() != null
          || request.getMemo() != null
          || request.getMemoType() != null) {
        throw new SepValidationException(
            HttpStatus.SC_BAD_REQUEST,
            "A requests with 'id' cannot also have 'account', 'memo', or 'memo_type'");
      }
    }

    if (request.getAccount() != null
        && Stream.of(token.getAccount(), token.getMuxedAccount())
            .noneMatch(it -> request.getAccount().equals(it))) {
      throw new SepValidationException(
          HttpStatus.SC_FORBIDDEN, "The account specified does not match authorization token");
    }

    if (token.getAccountMemo() != null && request.getMemo() != null) {
      if (!token.getAccountMemo().equals(request.getMemo())
          || !request.getMemoType().equals(MemoType.MEMO_ID.name())) {
        throw new SepValidationException(
            HttpStatus.SC_FORBIDDEN,
            "The memo specified does not match the memo ID authorized via SEP-10");
      }
    }

    try {
      MemoHelper.makeMemo(request.getMemo(), request.getMemoType());
    } catch (SepException e) {
      throw new SepValidationException(HttpStatus.SC_BAD_REQUEST, "Invalid 'memo' for 'memo_type'");
    }

    String memo = (request.getMemo() != null) ? request.getMemo() : token.getAccountMemo();
    String memoType = null;
    if (memo != null) {
      memoType = (request.getMemoType() != null) ? request.getMemoType() : MemoType.MEMO_ID.name();
    }

    request.setMemo(memo);
    request.setMemoType(memoType);

    return customerIntegration.putCustomer(request);
  }
}
