package finstant.platform1_telnet.helpers;

import enums.TickerType;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

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

    public static int getServerPort() {
        return config.getInt("server.port", 8081);
    }

    public static long getDataGeneratorIntervalMs() {
        return config.getLong("data.generator.interval.ms", 2000);
    }

    public static TickerType[] getSupportedTickers() {
        List<Object> tickerTypes = config.getList("supported.tickers");

        return tickerTypes.stream()
                .map(Object::toString)
                .map(TickerType::fromString)
                .filter(java.util.Objects::nonNull)
                .toArray(TickerType[]::new);
    }

    public static BigDecimal getTickerInitialValueFromTickerType(TickerType tickerType) {
        return config.getBigDecimal(String.format("data.%s", tickerType.toString()));
    }
}