package org.stellar.anchor.server.controller;


import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.dto.sep12.GetCustomerRequest;
import org.stellar.anchor.dto.sep12.GetCustomerResponse;
import org.stellar.anchor.dto.sep12.PutCustomerRequest;
import org.stellar.anchor.dto.sep12.PutCustomerResponse;
import org.stellar.anchor.dto.sep24.InfoResponse;
import org.stellar.anchor.exception.SepValidationException;
import org.stellar.anchor.sep10.JwtToken;
import org.stellar.anchor.sep12.Sep12Service;
import org.stellar.anchor.sep24.Sep24Service;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;

import static org.stellar.anchor.filter.BaseTokenFilter.JWT_TOKEN;
import static org.stellar.anchor.server.controller.Sep10Helper.getSep10Token;
import static org.stellar.anchor.util.Log.debug;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/sep24")
public class Sep12Controller {
    private final Sep12Service sep12Service;

    Sep12Controller(Sep12Service sep12Service) {
        this.sep12Service = sep12Service;
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/customer",
            method = {RequestMethod.GET})
    public GetCustomerResponse getCustomer(HttpServletRequest request,
                                           @RequestParam(required = false) String type,
                                           @RequestParam(required = false) String id,
                                           @RequestParam(required = false) String account,
                                           @RequestParam(required = false) String memo,
                                           @RequestParam(required = false, name="memo_type") String memoType,
                                           @RequestParam(required = false) String lang) throws SepValidationException {
        JwtToken jwtToken = getSep10Token(request);
        GetCustomerRequest getCustomerRequest = GetCustomerRequest.builder()
                .type(type)
                .id(id)
                .account(account)
                .memo(memo)
                .memoType(memoType)
                .lang(lang)
                .build();

        return sep12Service.getCustomer(jwtToken, getCustomerRequest).block();
    }

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/customer", consumes = {MediaType.APPLICATION_JSON_VALUE},
      method = {RequestMethod.PUT})
  public PutCustomerResponse putCustomer(
      HttpServletRequest request,
      @RequestBody PutCustomerRequest putCustomerRequest)
      throws SepValidationException {
        JwtToken jwtToken = getSep10Token(request);
        return sep12Service.putCustomer(jwtToken, putCustomerRequest).block();
  }
}
