package org.stellar.anchor.platform.controller.sep;

import static org.stellar.anchor.util.Log.debugF;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.api.sep.sep10c.ChallengeRequest;
import org.stellar.anchor.api.sep.sep10c.ChallengeResponse;
import org.stellar.anchor.api.sep.sep10c.ValidationRequest;
import org.stellar.anchor.api.sep.sep10c.ValidationResponse;
import org.stellar.anchor.platform.condition.ConditionalOnAllSepsEnabled;
import org.stellar.anchor.sep10.Sep10CService;
import org.stellar.anchor.util.GsonUtils;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/sep10c")
@ConditionalOnAllSepsEnabled(seps = {"sep10"})
public class Sep10CController {
  private final Sep10CService sep10CService;

  public Sep10CController(Sep10CService sep10CService) {
    this.sep10CService = sep10CService;
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/auth",
      produces = {MediaType.APPLICATION_JSON_VALUE},
      method = {RequestMethod.GET})
  public ChallengeResponse createChallenge(
      @RequestParam(name = "account") String account,
      @RequestParam(required = false, name = "memo") String memo,
      @RequestParam(required = false, name = "home_domain") String homeDomain,
      @RequestParam(required = false, name = "client_domain") String clientDomain,
      @RequestHeader(required = false, name = "Authorization") String authorization) {
    ChallengeRequest challengeRequest =
        ChallengeRequest.builder()
            .account(account)
            .memo(memo)
            .homeDomain(homeDomain)
            .clientDomain(clientDomain)
            .build();
    debugF(
        "GET /auth account={} memo={} home_domain={}, client_domain={}",
        account,
        memo,
        homeDomain,
        clientDomain);
    return sep10CService.createChallenge(challengeRequest);
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/auth",
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE},
      method = {RequestMethod.POST})
  public ValidationResponse validateChallenge(@RequestBody ValidationRequest validationRequest) {
    debugF("POST /auth validationRequest={}", GsonUtils.getInstance().toJson(validationRequest));
    return sep10CService.validateChallenge(validationRequest);
  }
}
