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

public class FinancialDataGenerator {
    private static final Logger logger = LogManager.getLogger(FinancialDataGenerator.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Random random = new Random();
    private final TickerType[] supportedTickers;
    private final BigDecimal FIXED_DELTA = new BigDecimal("0.003");
    private final Map<TickerType, BigDecimal> lastBidValues;
    private final Map<TickerType, BigDecimal> lastAskValues;

    public FinancialDataGenerator(TickerType[] supportedTickers) {
        this.supportedTickers = supportedTickers;
        this.lastBidValues = new ConcurrentHashMap<>();
        this.lastAskValues = new ConcurrentHashMap<>();
        initializeLastValues();
    }

    private BigDecimal getRandomDelta() {
        BigDecimal maxChange = new BigDecimal("0.003");
        double randomChange = random.nextDouble() * maxChange.doubleValue();
        BigDecimal delta = BigDecimal.valueOf(randomChange).setScale(4, RoundingMode.HALF_UP);
        return random.nextBoolean() ? delta : delta.negate();
    }

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

    private RateDto generateMarketData(TickerType ticker) {
        BigDecimal lastBid = lastBidValues.get(ticker);
        BigDecimal delta = getRandomDelta();
        BigDecimal bid = lastBid.add(delta);
        BigDecimal ask = bid.add(FIXED_DELTA.abs());
        lastBidValues.put(ticker, bid);
        lastAskValues.put(ticker, ask);
        return RateDto.builder()
                .rateName(ticker.getValue())
                .ask(ask)
                .bid(bid)
                .rateUpdateTime(LocalDateTime.now(ZoneOffset.UTC))
                .build();
    }

    public void stopGenerating() {
        scheduler.shutdownNow();
    }

    private void initializeLastValues() {
        for (TickerType ticker : supportedTickers) {
            lastBidValues.put(ticker, ConfigurationHelper.getTickerInitialValueFromTickerType(ticker));
            lastAskValues.put(ticker, ConfigurationHelper.getTickerInitialValueFromTickerType(ticker));
        }
    }
}