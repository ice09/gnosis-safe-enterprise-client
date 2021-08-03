package tech.blockchainers.gnosissafeclient.rest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddressSignatureDto {

    private String address;
    private String signature;
}
