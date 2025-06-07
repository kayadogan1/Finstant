package com.dogankaya.platform2_rest.services;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import rate.GetRateByTickerTypeRequest;
import rate.RateDto;
import enums.TickerType;
import com.dogankaya.platform2_rest.helpers.ConfigurationHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Service
@EnableScheduling
public class RateService {

    private static final Logger logger = LogManager.getLogger(RateService.class);

    /** Last bid values per ticker */
    private final Map<TickerType, BigDecimal> lastBidValues;

    /** Last ask values per ticker */
    private final Map<TickerType, BigDecimal> lastAskValues;

    /** Supported ticker types */
    private final TickerType[] supportedTickers;

    /** Random instance for delta generation */
    private final Random random;

    public RateService() {
        this.lastBidValues = new ConcurrentHashMap<>();
        this.lastAskValues = new ConcurrentHashMap<>();
        this.supportedTickers = ConfigurationHelper.getSupportedTickers();
        this.random = new Random();

        initializeLastValues();
    }

    /**
     * Initializes the last bid and ask values for all supported tickers.
     * Bid is initialized from configuration.
     * Ask is initialized as bid * 1.10.
     *
     * @throws IllegalStateException if supported tickers are not configured.
     */
    private void initializeLastValues() {
        if (supportedTickers == null || supportedTickers.length == 0) {
            throw new IllegalStateException("No supported tickers configured!");
        }
        for (TickerType ticker : supportedTickers) {
            BigDecimal initialBid = ConfigurationHelper.getTickerInitialValueFromTickerType(ticker)
                    .max(BigDecimal.valueOf(0.5)); // Minimum 0.5
            BigDecimal initialAsk = initialBid.multiply(BigDecimal.valueOf(1.10)).setScale(4, RoundingMode.HALF_UP);
            lastBidValues.put(ticker, initialBid.setScale(4, RoundingMode.HALF_UP));
            lastAskValues.put(ticker, initialAsk);
        }
    }

    /**
     * Returns a list of RateDto objects for the requested ticker types.
     * If a ticker is unsupported or null, it is skipped.
     *
     * @param requests List of GetRateByTickerTypeRequest containing requested ticker types.
     * @return List of RateDto with current bid, ask, and timestamp values.
     */
    public List<RateDto> getRatesByTickerTypeList(List<GetRateByTickerTypeRequest> requests) {
        List<RateDto> rates = new ArrayList<>();
        if (requests == null || requests.isEmpty()) {
            return rates;
        }

        for (GetRateByTickerTypeRequest req : requests) {
            TickerType ticker = TickerType.fromString(req.getValue());
            if (ticker == null || !lastBidValues.containsKey(ticker)) {
                logger.warn("Unsupported or null ticker in request: {}", req.getValue());
                continue;
            }

            BigDecimal bid = lastBidValues.get(ticker);
            BigDecimal ask = lastAskValues.get(ticker);

            RateDto rateDto = RateDto.builder()
                    .rateName(ticker.getValue())
                    .bid(bid)
                    .ask(ask)
                    .rateUpdateTime(LocalDateTime.now())
                    .build();

            rates.add(rateDto);
        }
        return rates;
    }

    /**
     * Generates a random BigDecimal delta value between -maxChange and +maxChange.
     *
     * @param maxChange Maximum magnitude of the delta.
     * @return Random BigDecimal delta.
     */
    private BigDecimal getRandomDelta(double maxChange) {
        double change = (random.nextDouble() * 2 - 1) * maxChange;
        return BigDecimal.valueOf(change).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Scheduled task to generate new bid and ask values for each supported ticker
     * and update the lastBidValues and lastAskValues maps.
     * Runs at a fixed rate specified by 'data.generator.interval.ms' configuration.
     */
    @Scheduled(fixedRateString = "${data.generator.interval.ms}")
    private void startGenerating() {
        LocalDateTime now = LocalDateTime.now();

        for (TickerType ticker : supportedTickers) {
            BigDecimal lastBid = lastBidValues.get(ticker);
            BigDecimal lastAsk = lastAskValues.get(ticker);

            if (lastBid == null || lastAsk == null) {
                logger.warn("Ticker {} has no last bid or ask value initialized!", ticker);
                continue;
            }

            BigDecimal newBid = lastBid.add(getRandomDelta(0.5))
                    .max(BigDecimal.valueOf(0.5))  // Minimum 0.5
                    .setScale(4, RoundingMode.HALF_UP);

            BigDecimal newAsk = newBid.add(getRandomDelta(0.2).abs())
                    .max(newBid.add(BigDecimal.valueOf(0.01)))  // Ask en az bid + 0.01
                    .setScale(4, RoundingMode.HALF_UP);

            lastBidValues.put(ticker, newBid);
            lastAskValues.put(ticker, newAsk);

            String timestamp = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            logger.info("Generated {} | Bid: {} | Ask: {} | Timestamp: {}",
                    ticker.getValue(), newBid, newAsk, timestamp);
        }
    }
}
