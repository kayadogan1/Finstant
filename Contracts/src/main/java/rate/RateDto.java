package rate;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * Data transfer object (DTO) representing a rate with bid and ask prices
 * and the time when the rate was last updated.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RateDto {
    private String rateName;
    private BigDecimal bid;
    private BigDecimal ask;
    private LocalDateTime rateUpdateTime;
}
