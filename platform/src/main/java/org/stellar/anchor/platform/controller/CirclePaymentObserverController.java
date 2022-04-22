package org.stellar.anchor.platform.controller;

import com.google.gson.Gson;
import java.lang.reflect.Type;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.platform.paymentobserver.CirclePaymentObserverService;
import shadow.com.google.common.reflect.TypeToken;

@RestController
@RequestMapping("/circle-observer")
public class CirclePaymentObserverController {
  private final Gson gson = new Gson();
  private final CirclePaymentObserverService circlePaymentObserverService;

  public CirclePaymentObserverController(
      CirclePaymentObserverService circlePaymentObserverService) {
    this.circlePaymentObserverService = circlePaymentObserverService;
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "",
      method = {RequestMethod.POST, RequestMethod.GET, RequestMethod.HEAD},
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  public void handleCircleNotificationJson(
      @RequestBody(required = false) Map<String, Object> requestBody) {
    circlePaymentObserverService.handleCircleNotification(requestBody);
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "",
      method = {RequestMethod.POST, RequestMethod.GET, RequestMethod.HEAD},
      consumes = {MediaType.TEXT_PLAIN_VALUE})
  public void handleCircleNotificationTextPlain(@RequestBody(required = false) String jsonBodyStr) {
    Type type = new TypeToken<Map<String, ?>>() {}.getType();
    Map<String, Object> requestBody = gson.fromJson(jsonBodyStr, type);
    circlePaymentObserverService.handleCircleNotification(requestBody);
  }
}
