package com.dogankaya.FinanStream.controllers;

import com.dogankaya.FinanStream.services.CoordinatorService;
import enums.PlatformName;
import enums.TickerType;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing endpoints for connecting, subscribing, unsubscribing,
 * and disconnecting from various data platforms via CoordinatorService.
 */
@RestController
@RequestMapping("/coordinator")
public class CoordinatorController {

    private final CoordinatorService coordinatorService;

    public CoordinatorController(CoordinatorService coordinatorService) {
        this.coordinatorService = coordinatorService;
    }

    /**
     * Connect to the given platform.
     *
     * @param platformName platform to connect
     */
    @GetMapping("/connect/{platformName}")
    public void connect(@RequestParam PlatformName platformName){
        coordinatorService.connect(platformName);
    }

    /**
     * Subscribe to ticker updates for the given ticker type.
     *
     * @param tickerType ticker type to subscribe
     */
    @GetMapping("/subscribe/{tickerType}")
    public void subscribe(@RequestParam TickerType tickerType){
        coordinatorService.subscribe(tickerType);
    }

    /**
     * Unsubscribe from ticker updates for the given ticker type.
     *
     * @param tickerType ticker type to unsubscribe
     */
    @GetMapping("/unsubscribe/{tickerType}")
    public void unsubscribe(@RequestParam TickerType tickerType){
        coordinatorService.unsubscribe(tickerType);
    }

    /**
     * Disconnect from the given platform.
     *
     * @param platformName platform to disconnect
     */
    @GetMapping("/disconnect/{platformName}")
    public void disconnect(@RequestParam PlatformName platformName){
        coordinatorService.disconnect(platformName);
    }
}
