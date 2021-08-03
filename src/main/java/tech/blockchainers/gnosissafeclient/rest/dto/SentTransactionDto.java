package tech.blockchainers.gnosissafeclient.rest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SentTransactionDto {

    private String transactionHash;
    private String signatures;

}
