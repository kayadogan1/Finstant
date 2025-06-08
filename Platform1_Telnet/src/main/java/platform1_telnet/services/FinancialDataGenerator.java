package platform1_telnet.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rate.RateDto;
import enums.TickerType;
import platform1_telnet.helpers.ConfigurationHelper;
import platform1_telnet.handlers.TelnetServerHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * The {@code FinancialDataGenerator} class simulates financial market data for a set of supported tickers.
 * It periodically generates random bid and ask prices with slight variations and distributes
 * the generated market data to registered consumers and connected Telnet clients.
 *
 * <p>Each ticker's bid price fluctuates randomly within a small range,
 * and the ask price is calculated as the bid price plus a fixed delta spread.</p>
 *
 * <p>This class manages a scheduled executor service to generate data at a configurable interval.</p>
 */
public class FinancialDataGenerator {
    private static final Logger logger = LogManager.getLogger(FinancialDataGenerator.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Random random = new Random();
    private final TickerType[] supportedTickers;
    private final BigDecimal FIXED_DELTA = new BigDecimal("0.003");
    private final Map<TickerType, BigDecimal> lastBidValues;
    private final Map<TickerType, BigDecimal> lastAskValues;

    /**
     * Constructs a new FinancialDataGenerator with the given supported tickers.
     * Initializes the last bid and ask values for each ticker using configuration defaults.
     *
     * @param supportedTickers an array of {@link TickerType} representing the tickers to generate data for
     */
    public FinancialDataGenerator(TickerType[] supportedTickers) {
        this.supportedTickers = supportedTickers;
        this.lastBidValues = new ConcurrentHashMap<>();
        this.lastAskValues = new ConcurrentHashMap<>();
        initializeLastValues();
    }

    /**
     * Generates a random delta value for price fluctuation.
     * The delta is a random decimal value between -0.003 and +0.003, rounded to 4 decimal places.
     *
     * @return a {@link BigDecimal} representing the random price delta
     */
    private BigDecimal getRandomDelta() {
        BigDecimal maxChange = new BigDecimal("0.003");
        double randomChange = random.nextDouble() * maxChange.doubleValue();
        BigDecimal delta = BigDecimal.valueOf(randomChange).setScale(4, RoundingMode.HALF_UP);
        return random.nextBoolean() ? delta : delta.negate();
    }

    /**
     * Starts generating market data for all supported tickers at fixed intervals.
     * Each generated {@link RateDto} is passed to the provided {@code dataConsumer} and
     * also distributed to connected Telnet clients via {@link TelnetServerHandler}.
     *
     * @param dataConsumer a {@link Consumer} that accepts generated {@link RateDto} objects
     */
    public void startGenerating(Consumer<RateDto> dataConsumer) {
        long interval = ConfigurationHelper.getDataGeneratorIntervalMs();
        scheduler.scheduleAtFixedRate(() -> {
            for (TickerType ticker : supportedTickers) {
                RateDto generatedData = generateMarketData(ticker);
                dataConsumer.accept(generatedData);
                TelnetServerHandler.distributeMarketData(generatedData);
                logger.info("Generated market data: {} {} {} {}", generatedData.getRateName(), generatedData.getAsk(), generatedData.getBid(), generatedData);
            }
        }, 0, interval, TimeUnit.MILLISECONDS);
    }

    /**
     * Generates new market data for the specified ticker.
     * Bid price is calculated by applying a random delta to the last bid value,
     * and ask price is bid plus a fixed delta spread.
     *
     * @param ticker the {@link TickerType} to generate market data for
     * @return a new {@link RateDto} containing the generated market data
     */
    private RateDto generateMarketData(TickerType ticker) {
        BigDecimal lastBid = lastBidValues.get(ticker);
        BigDecimal delta = getRandomDelta();
        BigDecimal bid = lastBid.add(delta).setScale(4, RoundingMode.HALF_UP);
        BigDecimal ask = bid.add(FIXED_DELTA.abs()).setScale(4, RoundingMode.HALF_UP);
        lastBidValues.put(ticker, bid);
        lastAskValues.put(ticker, ask);
        return RateDto.builder()
                .rateName(ticker.getValue())
                .ask(ask)
                .bid(bid)
                .rateUpdateTime(LocalDateTime.now(ZoneOffset.UTC))
                .build();
    }

    /**
     * Stops the market data generation by shutting down the scheduled executor service.
     * This attempts to stop all running tasks immediately.
     */
    public void stopGenerating() {
        scheduler.shutdownNow();
    }

    /**
     * Initializes the last bid and ask values for each supported ticker
     * with initial values obtained from the configuration helper.
     */
    private void initializeLastValues() {
        for (TickerType ticker : supportedTickers) {
            lastBidValues.put(ticker, ConfigurationHelper.getTickerInitialValueFromTickerType(ticker));
            lastAskValues.put(ticker, ConfigurationHelper.getTickerInitialValueFromTickerType(ticker));
        }
    }
}
