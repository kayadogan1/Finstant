package com.dogankaya.platform2_rest.services;

import org.springframework.scheduling.annotation.EnableScheduling;
import rate.GetRateByTickerTypeRequest;
import rate.RateDto;
import com.dogankaya.platform2_rest.helpers.ConfigurationHelper;
import enums.TickerType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Service
@EnableScheduling
public class RateService {

    private static final Logger logger = LogManager.getLogger(RateService.class);

    private final Map<TickerType, BigDecimal> lastBidValues;
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

    public List<RateDto> getRatesByTickerTypeList(List<GetRateByTickerTypeRequest> request) {
        List<RateDto> rateDtos = new ArrayList<>();
        request.forEach(req ->
        {
            TickerType t = TickerType.fromString(req.getValue());
            assert t != null;
            rateDtos.add(RateDto.builder()
                    .rateName(t.getValue())
                    .ask(lastBidValues.get(t))
                    .bid(lastBidValues.get(t))
                    .build());
        });
        return rateDtos;
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

            logger.info("Generated {} | Bid: {} | Ask: {} | Timestamp: {}",
                    ticker.getValue(),
                    bid,
                    ask,
                    timestamp);
        }
    }
}
