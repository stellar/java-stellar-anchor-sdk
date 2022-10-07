package org.stellar.anchor.platform.controller;

import static org.stellar.anchor.platform.controller.Sep10Helper.getSep10Token;
import static org.stellar.anchor.util.Log.*;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.api.sep.sep12.*;
import org.stellar.anchor.auth.JwtToken;
import org.stellar.anchor.platform.condition.ConditionalOnAllSepsEnabled;
import org.stellar.anchor.sep12.Sep12Service;
import org.stellar.anchor.util.GsonUtils;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/sep12")
@ConditionalOnAllSepsEnabled(seps = {"sep12"})
@Profile("default")
public class Sep12Controller {
  private final Sep12Service sep12Service;

  Sep12Controller(Sep12Service sep12Service) {
    this.sep12Service = sep12Service;
  }

  @SneakyThrows
  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/customer",
      method = {RequestMethod.GET})
  public Sep12GetCustomerResponse getCustomer(
      HttpServletRequest request,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String id,
      @RequestParam(required = false) String account,
      @RequestParam(required = false) String memo,
      @RequestParam(required = false, name = "memo_type") String memoType,
      @RequestParam(required = false) String lang) {
    debugF(
        "GET /customer type={} id={} account={} memo={}, memoType={}, lang={}",
        type,
        id,
        account,
        memo,
        memoType,
        lang);
    JwtToken jwtToken = getSep10Token(request);
    Sep12GetCustomerRequest getCustomerRequest =
        Sep12GetCustomerRequest.builder()
            .type(type)
            .id(id)
            .account(account)
            .memo(memo)
            .memoType(memoType)
            .lang(lang)
            .build();

    return sep12Service.getCustomer(jwtToken, getCustomerRequest);
  }

  @SneakyThrows
  @CrossOrigin(origins = "*")
  @ResponseStatus(code = HttpStatus.ACCEPTED)
  @RequestMapping(
      value = "/customer",
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      method = {RequestMethod.PUT})
  public Sep12PutCustomerResponse putCustomer(
      HttpServletRequest request, @RequestBody Sep12PutCustomerRequest putCustomerRequest) {
    debug("PUT /customer details:", putCustomerRequest);
    JwtToken jwtToken = getSep10Token(request);
    return sep12Service.putCustomer(jwtToken, putCustomerRequest);
  }

  @SneakyThrows
  @CrossOrigin(origins = "*")
  @ResponseStatus(code = HttpStatus.ACCEPTED)
  @RequestMapping(
      value = "/customer",
      method = {RequestMethod.POST, RequestMethod.PUT},
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  public Sep12PutCustomerResponse putCustomerMultipart(HttpServletRequest request) {
    debug("PUT /customer multipart body:", request.getParameterMap());
    Gson gson = GsonUtils.getInstance();
    Map<String, String> requestData = new HashMap<>();
    for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
      requestData.put(entry.getKey(), entry.getValue()[0]);
    }
    Sep12PutCustomerRequest putCustomerRequest =
        gson.fromJson(gson.toJson(requestData), Sep12PutCustomerRequest.class);
    JwtToken jwtToken = getSep10Token(request);
    return sep12Service.putCustomer(jwtToken, putCustomerRequest);
  }

  @SneakyThrows
  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/customer/{account}",
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      method = {RequestMethod.DELETE})
  @ResponseStatus(code = HttpStatus.OK)
  public void deleteCustomer(
      HttpServletRequest request,
      @PathVariable String account,
      @RequestBody(required = false) Sep12DeleteCustomerRequest body) {
    JwtToken jwtToken = getSep10Token(request);
    String memo = body != null ? body.getMemo() : null;
    String memoType = body != null ? body.getMemoType() : null;
    debugF(
        "DELETE /customer requestURI={} account={} body={}",
        request.getRequestURI(),
        account,
        body);
    sep12Service.deleteCustomer(jwtToken, account, memo, memoType);
  }

  @SneakyThrows
  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/customer/{account}",
      consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE},
      method = {RequestMethod.DELETE})
  @ResponseStatus(code = HttpStatus.OK)
  public void deleteCustomer(
      HttpServletRequest request,
      @PathVariable String account,
      @RequestParam(required = false) String memo,
      @RequestParam(required = false, name = "memo_type") String memoType) {
    JwtToken jwtToken = getSep10Token(request);
    debugF("DELETE /customer requestURI={} account={}", request.getRequestURI(), account);
    sep12Service.deleteCustomer(jwtToken, account, memo, memoType);
  }
}
