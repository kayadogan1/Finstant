package com.dogankaya.platform2_rest.controllers;

import rate.GetRateByTickerTypeRequest;
import rate.RateDto;
import com.dogankaya.platform2_rest.services.RateService;
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
    public RateDto getRateByTickerType(GetRateByTickerTypeRequest request) {
        return rateService.getRateByTickerType(request);
    }
}
