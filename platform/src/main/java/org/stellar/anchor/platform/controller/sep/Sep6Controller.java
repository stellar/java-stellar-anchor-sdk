package org.stellar.anchor.platform.controller.sep;

import static org.stellar.anchor.platform.controller.sep.Sep10Helper.getSep10Token;
import static org.stellar.anchor.util.Log.debugF;

import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.api.sep.sep6.*;
import org.stellar.anchor.auth.Sep10Jwt;
import org.stellar.anchor.platform.condition.ConditionalOnAllSepsEnabled;
import org.stellar.anchor.sep6.Sep6Service;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("sep6")
@ConditionalOnAllSepsEnabled(seps = {"sep6"})
public class Sep6Controller {
  private final Sep6Service sep6Service;

  public Sep6Controller(Sep6Service sep6Service) {
    this.sep6Service = sep6Service;
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/info",
      method = {RequestMethod.GET})
  public InfoResponse getInfo() {
    debugF("GET /info");
    return sep6Service.getInfo();
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/deposit",
      method = {RequestMethod.GET})
  public GetDepositResponse deposit(
      HttpServletRequest request,
      @RequestParam(value = "asset_code") String assetCode,
      @RequestParam(value = "account") String account,
      @RequestParam(value = "memo_type", required = false) String memoType,
      @RequestParam(value = "memo", required = false) String memo,
      @RequestParam(value = "email_address", required = false) String emailAddress,
      @RequestParam(value = "type") String type,
      @RequestParam(value = "wallet_name", required = false) String walletName,
      @RequestParam(value = "wallet_url", required = false) String walletUrl,
      @RequestParam(value = "lang", required = false) String lang,
      @RequestParam(value = "amount") String amount,
      @RequestParam(value = "country_code", required = false) String countryCode,
      @RequestParam(value = "claimable_balances_supported", required = false)
          Boolean claimableBalancesSupported)
      throws AnchorException {
    debugF("GET /deposit");
    Sep10Jwt token = getSep10Token(request);
    GetDepositRequest getDepositRequest =
        GetDepositRequest.builder()
            .assetCode(assetCode)
            .account(account)
            .memoType(memoType)
            .memo(memo)
            .emailAddress(emailAddress)
            .type(type)
            .walletName(walletName)
            .walletUrl(walletUrl)
            .lang(lang)
            .amount(amount)
            .countryCode(countryCode)
            .claimableBalancesSupported(claimableBalancesSupported)
            .build();
    return sep6Service.deposit(token, getDepositRequest);
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/withdraw",
      method = {RequestMethod.GET})
  public GetWithdrawResponse withdraw(
      HttpServletRequest request,
      @RequestParam(value = "asset_code") String assetCode,
      @RequestParam(value = "type") String type,
      @RequestParam(value = "amount") String amount,
      @RequestParam(value = "country_code", required = false) String countryCode,
      @RequestParam(value = "refundMemo", required = false) String refundMemo,
      @RequestParam(value = "refundMemoType", required = false) String refundMemoType)
      throws AnchorException {
    debugF("GET /withdraw");
    Sep10Jwt token = getSep10Token(request);
    GetWithdrawRequest getWithdrawRequest =
        GetWithdrawRequest.builder()
            .assetCode(assetCode)
            .type(type)
            .amount(amount)
            .countryCode(countryCode)
            .refundMemo(refundMemo)
            .refundMemoType(refundMemoType)
            .build();
    return sep6Service.withdraw(token, getWithdrawRequest);
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/transactions",
      method = {RequestMethod.GET})
  public GetTransactionsResponse getTransactions(
      HttpServletRequest request,
      @RequestParam(value = "asset_code") String assetCode,
      @RequestParam(required = false, value = "kind") String kind,
      @RequestParam(required = false, value = "limit") Integer limit,
      @RequestParam(required = false, value = "paging_id") String pagingId,
      @RequestParam(required = false, value = "no_older_than") String noOlderThan,
      @RequestParam(required = false, value = "lang") String lang)
      throws SepException {
    debugF(
        "/transactions asset_code={} kind={} limit={} paging_id={} no_older_than={} lang={}",
        assetCode,
        kind,
        limit,
        pagingId,
        noOlderThan,
        lang);
    Sep10Jwt token = getSep10Token(request);
    GetTransactionsRequest getTransactionsRequest =
        GetTransactionsRequest.builder()
            .assetCode(assetCode)
            .kind(kind)
            .limit(limit)
            .pagingId(pagingId)
            .noOlderThan(noOlderThan)
            .lang(lang)
            .build();
    return sep6Service.findTransactions(token, getTransactionsRequest);
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/transaction",
      method = {RequestMethod.GET})
  public GetTransactionResponse getTransaction(
      HttpServletRequest request,
      @RequestParam(required = false, value = "id") String id,
      @RequestParam(required = false, value = "stellar_transaction_id") String stellarTransactionId,
      @RequestParam(required = false, value = "external_transaction_id")
          String externalTransactionId,
      @RequestParam(required = false, value = "lang") String lang)
      throws AnchorException {
    debugF(
        "/transaction id={} stellar_transaction_id={} external_transaction_id={} lang={}",
        id,
        stellarTransactionId,
        externalTransactionId,
        lang);
    Sep10Jwt token = getSep10Token(request);
    GetTransactionRequest getTransactionRequest =
        GetTransactionRequest.builder()
            .id(id)
            .stellarTransactionId(stellarTransactionId)
            .externalTransactionId(externalTransactionId)
            .build();
    return sep6Service.findTransaction(token, getTransactionRequest);
  }
}
