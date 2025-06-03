package com.dogankaya.platform2_rest.services;

import rate.GetRateByTickerTypeRequest;
import rate.RateDto;
import com.dogankaya.platform2_rest.helpers.ConfigurationHelper;
import enums.TickerType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateService {

    private Map<TickerType, BigDecimal> lastBidValues;
    private final TickerType[] supportedTickers;
    private final Random random = new Random();

    public RateService() {
        this.lastBidValues = new ConcurrentHashMap<>();
        this.supportedTickers = ConfigurationHelper.getSupportedTickers();
        initializeLastValues();
    }

    private void initializeLastValues() {
        assert supportedTickers.length > 0;
        for (TickerType tickerType : supportedTickers) {
            lastBidValues.put(tickerType, ConfigurationHelper.getTickerInitialValueFromTickerType(tickerType));
        }
    }

    public RateDto getRateByTickerType(GetRateByTickerTypeRequest request) {
        TickerType tickerType = TickerType.fromString(request.getValue());
        assert tickerType != null;
        return RateDto.builder()
                .rateName(tickerType.getValue())
                .ask(lastBidValues.get(tickerType))
                .bid(lastBidValues.get(tickerType))
                .build();
    }

    private BigDecimal getRandomDelta(double maxChange) {
        double change = random.nextDouble() * maxChange;
        return BigDecimal.valueOf(random.nextBoolean() ?  change : -change);
    }

    @Scheduled(fixedRateString = "${data.generator.interval.ms}")
    private void startGenerating() {
        for (TickerType ticker : supportedTickers) {
            BigDecimal lastBid = lastBidValues.get(ticker);

            BigDecimal bid = lastBid.add(getRandomDelta(2.0));
            BigDecimal ask = bid.add(getRandomDelta(0.5).abs());
            lastBidValues.put(ticker, bid);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            System.out.println("DEBUG: Generated " + String.format("%s| Bid: %.5f| Ask: %.5f| Timestamp: %s",
                    ticker.getValue(),
                    bid,
                    ask,
                    timestamp));
        }
    }
}
