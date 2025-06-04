package com.dogankaya.FinanStream.services;

import com.dogankaya.FinanStream.abscraction.ICoordinatorActions;
import enums.PlatformName;
import enums.TickerType;
import org.springframework.stereotype.Service;

@Service
public class CoordinatorService {

    private final ICoordinatorActions coordinator;
    public CoordinatorService(ICoordinatorActions coordinator) {
        this.coordinator = coordinator;
    }

    public void subscribe(TickerType tickerType) {
        coordinator.subscribe(tickerType);
    }

    public void connect(PlatformName platformName) {
        coordinator.connect(platformName);
    }

    public void unsubscribe(TickerType tickerType) {
        coordinator.unsubscribe(tickerType);
    }

    public void disconnect(PlatformName platformName) {
        coordinator.disconnect(platformName);
    }
}
