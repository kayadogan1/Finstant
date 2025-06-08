package com.dogankaya.platform2_rest.controllers;

import org.springframework.web.bind.annotation.RequestParam;
import rate.GetRateByTickerTypeRequest;
import rate.RateDto;
import com.dogankaya.platform2_rest.services.RateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller that exposes endpoints to retrieve rate data by ticker types.
 * Provides API endpoints under the "/api" path.
 */
@RestController
@RequestMapping("/api")
public class RateController {

    private final RateService rateService;

    /**
     * Constructs a new {@code RateController} with the given {@link RateService}.
     *
     * @param rateService the service responsible for retrieving rate data
     */
    public RateController(RateService rateService) {
        this.rateService = rateService;
    }

    /**
     * Retrieves a list of {@link RateDto} objects based on the list of ticker type requests.
     *
     * <p>Expected to receive a list of {@link GetRateByTickerTypeRequest} as a request parameter.</p>
     *
     * @param request a list of {@link GetRateByTickerTypeRequest} representing requested ticker types
     * @return a list of {@link RateDto} corresponding to the requested ticker types
     */
    @GetMapping("/rates/{tickerType}")
    public List<RateDto> getRatesByTickerTypeList(@RequestParam List<GetRateByTickerTypeRequest> request) {
        return rateService.getRatesByTickerTypeList(request);
    }
}
