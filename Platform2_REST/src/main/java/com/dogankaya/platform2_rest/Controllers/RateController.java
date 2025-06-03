package com.dogankaya.platform2_rest.Controllers;

import Rate.RateDto;
import com.dogankaya.platform2_rest.Services.RateService;
import enums.TickerType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RateController {

    private final RateService rateService;

    public RateController(RateService rateService) {
        this.rateService = rateService;
    }

    @GetMapping("/rates/{tickerType}")
    public RateDto getRateByTickerType(TickerType tickerType) {
        return rateService.getRateByTickerType(tickerType);
    }
}
