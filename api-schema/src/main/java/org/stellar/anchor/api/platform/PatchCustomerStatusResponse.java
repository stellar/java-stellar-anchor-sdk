package org.stellar.anchor.api.platform;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.stellar.anchor.api.shared.CustomerStatus;

@EqualsAndHashCode(callSuper = false)
@Data
public class PatchCustomerStatusResponse extends CustomerStatus {}
