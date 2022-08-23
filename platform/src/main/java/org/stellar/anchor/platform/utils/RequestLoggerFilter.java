package org.stellar.anchor.platform.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Objects;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.stellar.anchor.util.Log;

/**
 * Log each request and response. The request body is never logged and the response body is only
 * logged when it's an error
 *
 * @see <a href="https://stackoverflow.com/a/42023374/875657">StackOverflow Answer</a>
 */
public class RequestLoggerFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    long startTime = System.currentTimeMillis();
    StringBuffer logMessage =
        new StringBuffer("\n\tREST Request:\n")
            .append("\t\t[METHOD: " + request.getMethod() + "]\n")
            .append("\t\t[PATH: " + request.getRequestURI());

    if (request.getQueryString() != null) {
      logMessage.append("?").append(request.getQueryString());
    }
    logMessage.append("]\n");
    if (request.getAuthType() != null) {
      logMessage.append("\t\t[AUTH_TYPE: " + request.getAuthType() + "]\n");
    }
    if (request.getUserPrincipal() != null) {
      logMessage.append("\t\t[PRINCIPAL_NAME: " + request.getUserPrincipal().getName() + "]\n");
    }
    logMessage.append("\t\t[CLIENT IP: " + request.getRemoteAddr() + "]\n");

    // ========= Log request and response payload ("body") ========
    // We CANNOT simply read the request payload here, because then the InputStream would be
    // consumed and cannot be read again by the actual processing/server.
    // So we need to apply some stronger magic here :-)
    ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
    ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

    // IMPORTANT: This performs the actual request!
    filterChain.doFilter(wrappedRequest, wrappedResponse);
    long duration = System.currentTimeMillis() - startTime;

    logMessage
        .append("\tREST Response:\n")
        .append("\t\t[RESPONSE STATUS: " + response.getStatus() + "]\n")
        .append("\t\t[RESPONSE BODY: ");

    getBody(wrappedResponse)
        .lines()
        .forEach(
            line -> {
              logMessage.append("\n\t\t\t").append(line);
            });
    logMessage.append("\n\t\t]\n");

    logMessage.append("\tREST Duration (milliseconds): " + duration);

    Log.info(logMessage.toString());

    // IMPORTANT: copy content of response back into original response:
    wrappedResponse.copyBodyToResponse();
  }

  /**
   * getBody will get the response body (if it's an error) or omit it if it's not an error.
   *
   * @param wrappedResponse the wrapped response
   * @return the response body, if it's an error, or "[hidden]" if it's not.
   */
  private String getBody(ContentCachingResponseWrapper wrappedResponse) {
    byte[] buf = wrappedResponse.getContentAsByteArray();

    if (Objects.toString(buf, "").isEmpty()) {
      return "";
    }

    int length = Math.min(buf.length, 2048);
    try {
      String body = new String(buf, 0, length, wrappedResponse.getCharacterEncoding());
      return body.contains("\"error\"") ? body : "[hidden]";
    } catch (UnsupportedEncodingException ex) {
      return "Unsupported Encoding";
    }
  }
}
