import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rate{
    private int id;
    private String rateName;
    private BigDecimal bid;
    private BigDecimal ask;
    private LocalDateTime rateUpdateTime;
    private LocalDateTime dbUpdateTime;
}