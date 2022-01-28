package org.stellar.anchor.paymentsservice.circle.model.response;

import lombok.Data;
import org.stellar.anchor.paymentsservice.circle.model.CircleBalance;

import java.util.List;

@Data
public class CircleAccountBalancesResponse {
    Data data;

    @lombok.Data
    public static class Data {
        public List<CircleBalance> available;
        public List<CircleBalance> unsettled;
    }
}
