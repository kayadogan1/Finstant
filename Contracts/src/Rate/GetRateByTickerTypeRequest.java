package Rate;

import lombok.Getter;

@Getter
public enum GetRateByTickerTypeRequest {
    PF2_USDTRY("PF2_USDTRY"),
    PF2_EURUSD("PF2_EURUSD"),
    PF2_GBPUSD("PF2_GBPUSD");

    private final String value;

    GetRateByTickerTypeRequest(String value) {
        this.value = value;
    }
}
