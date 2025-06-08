package rate;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a financial rate entity with bid and ask prices,timestamps and an identifier
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Rate{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String rateName;
    private BigDecimal bid;
    private BigDecimal ask;
    private LocalDateTime rateUpdateTime;
    private LocalDateTime dbUpdateTime;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        dbUpdateTime = LocalDateTime.now();
    }
}