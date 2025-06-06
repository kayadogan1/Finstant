package rate;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a financial rate document with bid and ask prices,timestamps and an identifier for elasticsearch
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "rate-index")
public class RateDocument{
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