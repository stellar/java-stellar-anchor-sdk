package org.stellar.anchor.filter;

import org.stellar.anchor.api.exception.SepValidationException;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.auth.JwtToken;
import org.stellar.anchor.config.Sep10Config;

public class Sep10TokenFilter extends BaseTokenFilter {
  final Sep10Config sep10Config;

  public Sep10TokenFilter(Sep10Config sep10Config, JwtService jwtService) {
    super(jwtService);
    this.sep10Config = sep10Config;
  }

  @Override
  protected boolean isEnabled() {
    return sep10Config.getEnabled();
  }

  protected void validate(JwtToken token) throws SepValidationException {
    if (token == null) {
      throw new SepValidationException("JwtToken should not be null");
    }
    // TODO: Validate later.
  }
}
