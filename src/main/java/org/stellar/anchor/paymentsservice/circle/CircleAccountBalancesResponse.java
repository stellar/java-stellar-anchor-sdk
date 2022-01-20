package org.stellar.anchor.paymentsservice.circle;

import lombok.Data;

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
