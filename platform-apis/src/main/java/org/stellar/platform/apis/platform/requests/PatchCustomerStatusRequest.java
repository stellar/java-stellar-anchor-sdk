package org.stellar.platform.apis.platform.requests;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.stellar.platform.apis.shared.CustomerStatus;

@EqualsAndHashCode(callSuper = false)
@Data
public class PatchCustomerStatusRequest extends CustomerStatus {}
