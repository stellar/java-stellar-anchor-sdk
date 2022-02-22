package org.stellar.anchor.dto.sep12;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Refer to SEP-12.
 *
 * https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0012.md#request-1
 */
@Data
public class PutCustomerRequest {
    String id;
    String account;
    String memo;
    @SerializedName("memo_type")
    String memoType;
    String type;
    Map<String, String> sep9Fields = new HashMap<>();
}
