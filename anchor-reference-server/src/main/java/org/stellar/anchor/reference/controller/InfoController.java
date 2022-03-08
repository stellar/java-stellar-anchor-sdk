package org.stellar.anchor.reference.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.stellar.anchor.reference.config.AppSettings;

@RestController
public class InfoController {

  private AppSettings appSettings;

  public InfoController(AppSettings appSettings) {
    this.appSettings = appSettings;
  }

  /**
   * Example endpoint.
   *
   * @return list of services available.
   */
  @RequestMapping(
      value = "/version",
      method = {RequestMethod.GET})
  public String getVersion() {
    return appSettings.getVersion();
  }
}
