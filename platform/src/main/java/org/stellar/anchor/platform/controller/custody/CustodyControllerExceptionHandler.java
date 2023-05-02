package org.stellar.anchor.platform.controller.custody;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.stellar.anchor.platform.controller.ControllerExceptionHandler;

/** The uncaught exception handler. */
@RestControllerAdvice
public class CustodyControllerExceptionHandler extends ControllerExceptionHandler {}
