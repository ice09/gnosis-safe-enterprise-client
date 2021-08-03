package tech.blockchainers.gnosissafeclient.rest.dto;

import lombok.Data;

@Data
public class KeyPairDto {

    private String privateKeyHex;
    private String publicKeyHex;
    private String addressHex;

}
