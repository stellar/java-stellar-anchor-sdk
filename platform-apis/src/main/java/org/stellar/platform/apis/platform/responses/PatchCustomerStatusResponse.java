package org.stellar.platform.apis.platform.responses;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.stellar.platform.apis.shared.CustomerStatus;

@EqualsAndHashCode(callSuper = false)
@Data
public class PatchCustomerStatusResponse extends CustomerStatus {}
