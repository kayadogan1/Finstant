package platform1_telnet.helpers;

import enums.TickerType;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.List;
/**
 * The {@code ConfigurationHelper} class provides static utility methods to load and access
 * application-specific configuration properties from the {@code application.properties} file.
 * It handles properties such as server port, data generation interval, and supported financial tickers.
 */
public class ConfigurationHelper {
    private static final Logger logger = LogManager.getLogger(ConfigurationHelper.class);
    private static final PropertiesConfiguration config;

    static {
        try {
            config = new PropertiesConfiguration("application.properties");
        } catch (ConfigurationException e) {
            logger.error("Error loading application.properties", e);
            throw new RuntimeException("Failed to load application.properties", e);
        }
    }
    /**
     * Retrieves the server port from the configuration.
     *
     * @return The server port. Defaults to 8081 if not specified in the configuration.
     */
    public static int getServerPort() {
        return config.getInt("server.port", 8081);
    }
    /**
     * Retrieves the data generator interval in milliseconds from the configuration.
     * This interval determines how frequently new market data is generated.
     *
     * @return The data generator interval in milliseconds. Defaults to 2000ms if not specified.
     */
    public static long getDataGeneratorIntervalMs() {
        return config.getLong("data.generator.interval.ms", 2000);
    }
    /**
     * Retrieves the list of supported financial ticker types from the configuration.
     * The values are read as strings and then converted to {@link TickerType} enums.
     * Invalid or unrecognized ticker strings are filtered out.
     *
     * @return An array of {@link TickerType} representing the supported tickers.
     */
    public static TickerType[] getSupportedTickers() {
        List<Object> tickerTypes = config.getList("supported.tickers");

        return tickerTypes.stream()
                .map(Object::toString)
                .map(TickerType::fromString)
                .filter(java.util.Objects::nonNull)
                .toArray(TickerType[]::new);
    }
    /**
     * Retrieves the initial numerical value for a specific ticker type from the configuration.
     * The property key is dynamically formed using the ticker type's string representation
     * (e.g., "data.EURUSD").
     *
     * @param tickerType The {@link TickerType} for which to retrieve the initial value.
     * @return A {@link BigDecimal} representing the initial value for the specified ticker.
     */
    public static BigDecimal getTickerInitialValueFromTickerType(TickerType tickerType) {
        return config.getBigDecimal(String.format("data.%s", tickerType.toString()));
    }
}