package org.stellar.anchor.platform.controller.custody;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.platform.custody.service.FireblocksEventsService;

@RestController
@AllArgsConstructor
@CrossOrigin(origins = "*")
@ConditionalOnProperty(value = "custody.type", havingValue = "fireblocks")
public class FireblocksWebhookController {

  private final FireblocksEventsService fireblocksEventsService;

  @SneakyThrows
  @CrossOrigin(origins = "*")
  @ResponseStatus(code = HttpStatus.OK)
  @RequestMapping(
      value = "/webhook",
      method = {RequestMethod.POST},
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<Void> handleFireblocksEvent(
      @RequestBody String eventObject, @RequestHeader Map<String, String> headers) {
    fireblocksEventsService.handleFireblocksEvent(eventObject, headers);
    return ResponseEntity.ok().build();
  }
}
