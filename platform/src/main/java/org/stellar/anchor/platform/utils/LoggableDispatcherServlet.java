package org.stellar.anchor.platform.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;
import org.stellar.anchor.util.Log;

public class LoggableDispatcherServlet extends DispatcherServlet {
  @Override
  protected void doDispatch(HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    if (!(request instanceof ContentCachingRequestWrapper)) {
      request = new ContentCachingRequestWrapper(request);
    }
    if (!(response instanceof ContentCachingResponseWrapper)) {
      response = new ContentCachingResponseWrapper(response);
    }
    HandlerExecutionChain handler = getHandler(request);

    try {
      super.doDispatch(request, response);
    } finally {
      logRequestAndResponse(request, response, handler);
      updateResponse(response);
    }
  }

  private void logRequestAndResponse(
      HttpServletRequest requestToCache,
      HttpServletResponse responseToCache,
      HandlerExecutionChain handler) {
    StringBuilder logMessage =
        new StringBuilder("\n\tREST Request:\n")
            .append("\t\t[METHOD: " + requestToCache.getMethod() + "]\n")
            .append("\t\t[PATH: " + requestToCache.getRequestURI() + "]\n")
            .append("\t\t[CLIENT IP: " + requestToCache.getRemoteAddr() + "]\n")
            .append("\t\t[JAVA METHOD: " + handler.toString() + "]\n")
            .append("\tREST Response:\n")
            .append("\t\t[RESPONSE STATUS: " + responseToCache.getStatus() + "]\n")
            .append("\t\t[RESPONSE BODY: " + getResponsePayload(responseToCache) + "]");
    Log.info(logMessage.toString());
  }

  private String getResponsePayload(HttpServletResponse response) {
    ContentCachingResponseWrapper wrapper =
        WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
    if (wrapper != null) {

      byte[] buf = wrapper.getContentAsByteArray();
      if (buf.length > 0) {
        int length = Math.min(buf.length, 5120);
        try {
          String body = new String(buf, 0, length, wrapper.getCharacterEncoding());
          return body.contains("\"error\"") ? body : "[hidden]";
        } catch (UnsupportedEncodingException ex) {
          // NOOP
        }
      }
    }
    return "[unknown]";
  }

  private void updateResponse(HttpServletResponse response) throws IOException {
    ContentCachingResponseWrapper responseWrapper =
        WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
    responseWrapper.copyBodyToResponse();
  }
}
