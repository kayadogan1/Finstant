package rate;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "rate-index")
public class RateDocument {
    @Id
    private int id;
    private String rateName;
    private BigDecimal bid;
    private BigDecimal ask;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private String rateUpdateTime;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private String dbUpdateTime;
}
