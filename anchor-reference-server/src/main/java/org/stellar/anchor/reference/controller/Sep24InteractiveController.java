package org.stellar.anchor.reference.controller;

import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.stellar.anchor.api.exception.SepValidationException;
import org.stellar.anchor.util.Log;
import org.stellar.anchor.util.StringHelper;

/** The controller that implement the endpoints of the Sep24 interactive flow. */
@Controller
@CrossOrigin(origins = "*")
@RequestMapping("/")
public class Sep24InteractiveController {
  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/sep24/interactive",
      method = {RequestMethod.GET})
  @ResponseBody
  public String interactive(HttpServletRequest request) throws SepValidationException {
    String operation = request.getParameter("operation");
    if (StringHelper.isEmpty(operation)) {
      throw new SepValidationException("Missing [operation] parameter.");
    }

    switch (operation.toLowerCase()) {
      case "deposit":
        return "The sep24 interactive DEPOSIT starts here.";
      case "withdraw":
        return "The sep24 interactive WITHDRAW starts here.";
      default:
        Log.warnF("Unsupported operation {}", operation);
        return "The only supported operations are \"deposit\" or \"withdraw\"";
    }
  }
}
