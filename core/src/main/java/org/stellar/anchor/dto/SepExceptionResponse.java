package org.stellar.anchor.dto;

import lombok.Data;

@Data
public class SepExceptionResponse {
    String error;

    public SepExceptionResponse(String error) {
        this.error = error;
    }

}
