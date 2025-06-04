package com.dogankaya.FinanStream.abscraction;

import enums.PlatformName;
import enums.TickerType;

public interface ICoordinatorActions {
    void subscribe(TickerType tickerType);
    void connect(PlatformName platformName);
    void unsubscribe(TickerType tickerType);
    void disconnect(PlatformName platformName);
}
