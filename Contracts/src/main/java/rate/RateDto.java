package rate;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
