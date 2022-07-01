package org.stellar.anchor.filter;

import org.stellar.anchor.api.exception.SepValidationException;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.auth.JwtToken;

public class PlatformToAnchorTokenFilter extends BaseTokenFilter {
  public PlatformToAnchorTokenFilter(JwtService jwtService) {
    super(jwtService);
  }

  @Override
  protected boolean isEnabled() {
    return true;
  }

  protected void validate(JwtToken token) throws SepValidationException {
    if (token == null) {
      throw new SepValidationException("JwtToken should not be null");
    }
    // TODO: Validate later.
  }
}
