package platform1_telnet.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rate.RateDto;
import enums.TickerType;
import platform1_telnet.helpers.ConfigurationHelper;
import platform1_telnet.handlers.TelnetServerHandler;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * The {@code FinancialDataGenerator} class is responsible for generating simulated financial market data
 * for a predefined set of supported tickers. It uses a scheduled executor service to periodically
 * generate and distribute {@link RateDto} objects to registered consumers and Telnet subscribers.
 */
public class FinancialDataGenerator {
    private static final Logger logger = LogManager.getLogger(FinancialDataGenerator.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Random random = new Random();
    private final TickerType[] supportedTickers;
    private final Map<TickerType, BigDecimal> lastBidValues;
    /**
     * Constructs a new {@code FinancialDataGenerator}.
     * Initializes the generator with a list of supported tickers and sets up their initial bid values.
     *
     * @param supportedTickers An array of {@link TickerType} representing the financial instruments
     * for which data will be generated.
     */
    public FinancialDataGenerator(TickerType[] supportedTickers) {
        this.supportedTickers = supportedTickers;
        this.lastBidValues = new ConcurrentHashMap<>();
        initializeLastValues();
    }
    /**
     * Starts the data generation process.
     * Market data for each supported ticker will be generated and distributed at a fixed rate,
     * as configured by {@link ConfigurationHelper#getDataGeneratorIntervalMs()}.
     * The generated data is passed to the provided {@code dataConsumer} and also distributed
     * to active Telnet subscribers via {@link TelnetServerHandler#distributeMarketData(RateDto)}.
     *
     * @param dataConsumer A {@link Consumer} functional interface to process the generated {@link RateDto}.
     * This is typically used for internal processing of the data.
     */
    public void startGenerating(Consumer<RateDto> dataConsumer) {
        long interval = ConfigurationHelper.getDataGeneratorIntervalMs();
        scheduler.scheduleAtFixedRate(() -> {
            for (TickerType ticker : supportedTickers) {
                RateDto generatedData = generateMarketData(ticker);
                dataConsumer.accept(generatedData);
                TelnetServerHandler.distributeMarketData(generatedData);
                logger.info("Generated market data: {}", generatedData);
            }
        }, 0, interval, TimeUnit.MILLISECONDS);
    }
    /**
     * Generates a single {@link RateDto} for a given ticker.
     * This method calculates new bid and ask prices based on the last recorded bid value
     * and a random delta. The last bid value for the ticker is then updated.
     *
     * @param ticker The {@link TickerType} for which to generate market data.
     * @return A {@link RateDto} object containing the generated bid, ask, and update time.
     */
    private RateDto generateMarketData(TickerType ticker) {
        BigDecimal lastBid = lastBidValues.get(ticker);

        BigDecimal bid   = lastBid.add(getRandomDelta(2.0));
        BigDecimal ask   = bid.add(getRandomDelta(0.5).abs());
        lastBidValues.put(ticker, bid);
        return RateDto.builder()
                .rateName(ticker.getValue())
                .ask(ask)
                .bid(bid)
                .rateUpdateTime(LocalDateTime.now()).build();
    }
    /**
     * Generates a random {@link BigDecimal} delta (change) within a specified maximum.
     * The delta can be either positive or negative.
     *
     * @param maxChange The maximum absolute value of the change.
     * @return A {@link BigDecimal} representing a random positive or negative delta.
     */
    private BigDecimal getRandomDelta(double maxChange) {
        double change = random.nextDouble() * maxChange;
        return BigDecimal.valueOf(random.nextBoolean() ?  change : -change);
    }
    /**
     * Stops the data generation process.
     * This method shuts down the scheduled executor service, preventing further data generation.
     */
    public void stopGenerating() {
        scheduler.shutdownNow();
    }
    /**
     * Initializes the {@code lastBidValues} map with initial bid values for all supported tickers.
     * The initial values are retrieved from the {@link ConfigurationHelper}.
     */
    private void initializeLastValues() {
        for (TickerType ticker : supportedTickers) {
            lastBidValues.put(ticker, ConfigurationHelper.getTickerInitialValueFromTickerType(ticker));
        }
    }
}
