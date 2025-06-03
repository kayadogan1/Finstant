package enums;

public enum TickerType {
    PF1_USDTRY("PF1_USDTRY"),
    PF1_EURUSD("PF1_EURUSD"),
    PF1_GBPUSD("PF1_GBPUSD"),
    PF2_USDTRY("PF2_USDTRY"),
    PF2_EURUSD("PF2_EURUSD"),
    PF2_GBPUSD("PF2_GBPUSD"),
    USDTRY("USDTRY"),
    EURTRY("EURTRY"),
    GBPTRY("GBPTRY");

    private final String value;

    TickerType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TickerType fromString(String text) {
        for (TickerType b : TickerType.values()) {
            if (b.value.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }

}
