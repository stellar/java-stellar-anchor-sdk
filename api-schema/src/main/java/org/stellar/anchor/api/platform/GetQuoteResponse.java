package org.stellar.anchor.api.platform;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.stellar.anchor.api.shared.Quote;

@EqualsAndHashCode(callSuper = false)
@Data
public class GetQuoteResponse extends Quote {}
