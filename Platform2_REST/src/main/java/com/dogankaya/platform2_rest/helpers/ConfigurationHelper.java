package com.dogankaya.platform2_rest.helpers;

import enums.TickerType;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

/**
 * Helper class to load and provide configuration values from the application.properties file.
 * Uses Apache Commons Configuration to read properties.
 */
public class ConfigurationHelper {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationHelper.class);
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
     * Returns an array of supported {@link TickerType} values defined in the "supported.tickers" property.
     * The property should be a comma-separated list of ticker names matching {@link TickerType} values.
     *
     * @return an array of supported {@link TickerType} enums
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
     * Retrieves the initial BigDecimal value for a given {@link TickerType} from the properties.
     * The property key format is expected to be "data.{TICKER_TYPE}" where {TICKER_TYPE} is the string
     * representation of the {@link TickerType}.
     *
     * @param tickerType the ticker type for which to retrieve the initial value
     * @return the initial value as a {@link BigDecimal}, or null if the property is not found or invalid
     */
    public static BigDecimal getTickerInitialValueFromTickerType(TickerType tickerType) {
        return config.getBigDecimal(String.format("data.%s", tickerType.toString()));
    }
}
