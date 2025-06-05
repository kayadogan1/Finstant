package enums;

import java.util.Arrays;
/**
 * Enum representing different ticker types and their associated platforms.
 * <p>
 * Each ticker type has a unique string value and is linked to a specific platform.
 * This enum also provides utility methods to query tickers by platform name,
 * convert from string values to enum constants, and retrieve hash names based on platform.
 * </p>
 */
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

    /**
     * Constructor for TicketType enum.
     * @param value the ticker string value
     * @param platformName the platform name associated with this ticker
     */
    TickerType(String value, String platformName) {
        this.value = value;
        this.platformName = platformName;
    }

    /**
     * Returns all ticker values for a given platform name.
     *
     * @param platformName the platform name to filter tickers by
     * @return an array of ticker string values belonging to the specified platform
     */
    public static String[] getTickersFromPlatformName(String platformName) {
        return Arrays
                .stream(TickerType.values())
                .filter(ticker -> ticker.platformName.equalsIgnoreCase(platformName))
                .map(TickerType::getValue)
                .toArray(String[]::new);
    }
    /**
     * Returns the string value of this ticker.
     *
     * @return the ticker string value
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the platform name associated with this ticker
     * @return the string value of this ticker
     */
    public String getPlatformName() {
        return platformName;
    }

    /**
     * Converts a string value to its corresponding TickerType enum constant
     * @param text the ticker string value to convert
     * @return  the corresponding TickerType if found, or {@code null} if no match exists
     */
    public static TickerType fromString(String text) {
        for (TickerType b : TickerType.values()) {
            if (b.value.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }
    /**
     * Returns the Redis hash name associated with a given platform name.
     *
     * @param platformName the platform name
     * @return the Redis hash name as a string
     * @throws IllegalArgumentException if the platform name is unknown
     */
    public static String getHashNameFromPlatformName(String platformName){
        return switch (platformName) {
            case "Telnet" -> "raw_rates";
            case "REST" -> "raw_rates";
            case "Coordinator" -> "calculated_rates";
            default -> throw new IllegalArgumentException("Unknown platform name: " + platformName);
        };
    }
}
