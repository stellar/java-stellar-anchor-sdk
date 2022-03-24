package org.stellar.anchor.platform.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.stellar.anchor.dto.sep31.Sep31InfoResponse;
import org.stellar.anchor.sep31.Sep31Service;

@RestController
@RequestMapping("sep31")
public class Sep31Controller {
  private final Sep31Service sep31Service;

  public Sep31Controller(Sep31Service sep31Service) {
    this.sep31Service = sep31Service;
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/info",
      method = {RequestMethod.GET})
  public Sep31InfoResponse getInfo() {
    return sep31Service.getInfo();
  }
}
