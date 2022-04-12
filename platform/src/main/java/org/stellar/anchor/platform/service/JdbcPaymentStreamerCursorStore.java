package org.stellar.anchor.platform.service;

import java.util.Optional;
import org.springframework.stereotype.Component;
import org.stellar.anchor.platform.paymentobserver.PaymentStreamerCursorStore;
import org.stellar.anchor.server.data.StellarAccountPageToken;
import org.stellar.anchor.server.data.StellarAccountPageTokenRepo;

@Component
public class JdbcPaymentStreamerCursorStore implements PaymentStreamerCursorStore {
  private final StellarAccountPageTokenRepo repo;

  JdbcPaymentStreamerCursorStore(StellarAccountPageTokenRepo repo) {
    this.repo = repo;
  }

  @Override
  public void save(String account, String cursor) {
    StellarAccountPageToken pageToken = this.repo.findByAccountId(account).orElse(null);
    if (pageToken == null) {
      pageToken = new StellarAccountPageToken();
    }
    pageToken.setAccountId(account);
    pageToken.setToken(cursor);
    this.repo.save(pageToken);
  }

  @Override
  public String load(String account) {
    Optional<StellarAccountPageToken> pageToken = repo.findByAccountId(account);
    return pageToken.map(StellarAccountPageToken::getToken).orElse(null);
  }
}
