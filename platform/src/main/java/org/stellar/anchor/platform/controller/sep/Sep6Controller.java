package org.stellar.anchor.platform.controller.sep;

import static org.stellar.anchor.util.Log.debugF;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.stellar.anchor.api.sep.sep6.InfoResponse;
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
}
