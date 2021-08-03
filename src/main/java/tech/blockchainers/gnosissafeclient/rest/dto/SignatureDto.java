package tech.blockchainers.gnosissafeclient.rest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SignatureDto {

    private String dataHash;
    private String signature;

}
