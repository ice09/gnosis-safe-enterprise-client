package tech.blockchainers.gnosissafeclient.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GnosisSafeSetupDto {

    String safeAddress;
    String[] owners;
    int threshold;

}
