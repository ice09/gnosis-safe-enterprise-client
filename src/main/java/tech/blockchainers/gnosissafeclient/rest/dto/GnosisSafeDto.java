package tech.blockchainers.gnosissafeclient.rest.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GnosisSafeDto {

    private String address;
    private Integer threshold;
    private List<String> owners;

}
