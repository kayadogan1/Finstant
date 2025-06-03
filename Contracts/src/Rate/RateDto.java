package Rate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RateDto {
    private String rateName;
    private BigDecimal bid;
    private BigDecimal ask;
    private LocalDateTime rateUpdateTime;
}
