package org.stellar.platform.apis.api.responses;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.stellar.platform.apis.shared.Quote;

@EqualsAndHashCode(callSuper = false)
@Data
public class GetQuoteResponse extends Quote {}
