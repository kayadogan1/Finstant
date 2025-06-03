package platform1_telnet.services;

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

public class FinancialDataGenerator {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Random random = new Random();
    private final TickerType[] supportedTickers;
    private final Map<TickerType, BigDecimal> lastBidValues;

    public FinancialDataGenerator(TickerType[] supportedTickers) {
        this.supportedTickers = supportedTickers;
        this.lastBidValues = new ConcurrentHashMap<>();
        initializeLastValues();
    }

    public void startGenerating(Consumer<RateDto> dataConsumer) {
        long interval = ConfigurationHelper.getDataGeneratorIntervalMs();
        scheduler.scheduleAtFixedRate(() -> {
            for (TickerType ticker : supportedTickers) {
                RateDto generatedData = generateMarketData(ticker);

                dataConsumer.accept(generatedData);
                TelnetServerHandler.distributeMarketData(generatedData);
                System.out.println("DEBUG: Generated " + generatedData);
            }
        }, 0, interval, TimeUnit.MILLISECONDS);
    }

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

    private BigDecimal getRandomDelta(double maxChange) {
        double change = random.nextDouble() * maxChange;
        return BigDecimal.valueOf(random.nextBoolean() ?  change : -change);
    }

    public void stopGenerating() {
        scheduler.shutdownNow();
    }

    private void initializeLastValues() {
        for (TickerType ticker : supportedTickers) {
            // ConfigurationHelper içinde başlangıç değerini getiriyoruz
            lastBidValues.put(ticker, ConfigurationHelper.getTickerInitialValueFromTickerType(ticker));
        }
    }
}
