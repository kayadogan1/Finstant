package com.dogankaya.platform2_rest.Services;

import Rate.RateDto;
import enums.TickerType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class RateService {

    @Value("${supported.tickers}")
    private TickerType[] supportedTickers;
    private Map<TickerType, BigDecimal> lastBidvalues;

    public RateDto getRateByTickerType(TickerType tickerType) {
        return null;
    }
}
