package org.stellar.anchor.platform.controller.observer;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.stellar.anchor.platform.controller.ControllerExceptionHandler;

/** The uncaught exception handler. */
@RestControllerAdvice
public class ObserverControllerExceptionHandler extends ControllerExceptionHandler {}
