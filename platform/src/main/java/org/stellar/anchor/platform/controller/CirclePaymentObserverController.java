package org.stellar.anchor.platform.controller;

import static org.stellar.anchor.util.Log.errorEx;
import static org.stellar.anchor.util.Log.warnEx;

import com.google.gson.Gson;
import java.lang.reflect.Type;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.ServerErrorException;
import org.stellar.anchor.api.exception.UnprocessableEntityException;
import org.stellar.anchor.api.sep.SepExceptionResponse;
import org.stellar.anchor.platform.payment.observer.circle.CirclePaymentObserverService;
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
      @RequestBody(required = false) Map<String, Object> requestBody)
      throws UnprocessableEntityException, BadRequestException, ServerErrorException {
    circlePaymentObserverService.handleCircleNotification(requestBody);
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "",
      method = {RequestMethod.POST, RequestMethod.GET, RequestMethod.HEAD},
      consumes = {MediaType.TEXT_PLAIN_VALUE})
  public void handleCircleNotificationTextPlain(@RequestBody(required = false) String jsonBodyStr)
      throws UnprocessableEntityException, BadRequestException, ServerErrorException {
    Type type = new TypeToken<Map<String, ?>>() {}.getType();
    Map<String, Object> requestBody = gson.fromJson(jsonBodyStr, type);
    circlePaymentObserverService.handleCircleNotification(requestBody);
  }

  @ExceptionHandler(UnprocessableEntityException.class)
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  public SepExceptionResponse handleUnhandledCaseException(RestClientException ex) {
    warnEx(ex);
    return new SepExceptionResponse(ex.getMessage());
  }

  @ExceptionHandler(BadRequestException.class)
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  public SepExceptionResponse handleBadRequestException(RestClientException ex) {
    errorEx(ex);
    return new SepExceptionResponse(ex.getMessage());
  }
}
