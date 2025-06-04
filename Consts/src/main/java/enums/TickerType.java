package enums;

public enum TickerType {
    PF1_USDTRY("PF1_USDTRY", "Telnet"),
    PF1_EURUSD("PF1_EURUSD", "Telnet"),
    PF1_GBPUSD("PF1_GBPUSD", "Telnet"),
    PF2_USDTRY("PF2_USDTRY", "REST"),
    PF2_EURUSD("PF2_EURUSD", "REST"),
    PF2_GBPUSD("PF2_GBPUSD", "REST"),
    USDTRY("USDTRY", "Coordinator"),
    EURTRY("EURTRY", "Coordinator"),
    GBPTRY("GBPTRY", "Coordinator");

    private final String value;
    private final String platformName;

    TickerType(String value, String platformName) {
        this.value = value;
        this.platformName = platformName;
    }

    public String getValue() {
        return value;
    }

    public String getPlatformName() {
        return platformName;
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
