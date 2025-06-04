package com.dogankaya.FinanStream.controllers;

import com.dogankaya.FinanStream.services.CoordinatorService;
import enums.PlatformName;
import enums.TickerType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/coordinator")
public class CoordinatorController {

    private final CoordinatorService coordinatorService;

    public CoordinatorController(CoordinatorService coordinatorService) {
        this.coordinatorService = coordinatorService;
    }

    @GetMapping("/connect/{platformName}")
    public void connect(@RequestParam PlatformName platformName){
        coordinatorService.connect(platformName);
    }

    @GetMapping("/subscribe/{tickerType}")
    public void subscribe(@RequestParam TickerType tickerType){
        coordinatorService.subscribe(tickerType);
    }

    @GetMapping("/unsubscribe/{tickerType}")
    public void unsubscribe(@RequestParam TickerType tickerType){
        coordinatorService.unsubscribe(tickerType);
    }

    @GetMapping("/disconnect/{platformName}")
    public void disconnect(@RequestParam PlatformName platformName){
        coordinatorService.disconnect(platformName);
    }
}
