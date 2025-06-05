package rate;

import lombok.Getter;


/**
 * Enum representing ticker types that can be requested to get rates
 */
@Getter
public enum GetRateByTickerTypeRequest {
    PF2_USDTRY("PF2_USDTRY"),
    PF2_EURUSD("PF2_EURUSD"),
    PF2_GBPUSD("PF2_GBPUSD");

    private final String value;

    /**
     * Constructor for GetRateByTickerTypeRequest
     * @param value the string representation of the ticker type
     */
    GetRateByTickerTypeRequest(String value) {
        this.value = value;
    }
}
