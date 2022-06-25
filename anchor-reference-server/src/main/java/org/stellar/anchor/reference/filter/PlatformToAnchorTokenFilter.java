package org.stellar.anchor.reference.filter;

import org.stellar.anchor.api.exception.SepValidationException;
import org.stellar.anchor.filter.BaseTokenFilter;
import org.stellar.anchor.sep10.JwtService;
import org.stellar.anchor.sep10.JwtToken;

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
