package com.dogankaya.FinanStream.abscraction;

import enums.PlatformName;
import enums.TickerType;

/**
 * Defines coordinator actions for subscribing, connecting, unsubscribing,
 * and disconnecting from different platforms and ticker types.
 */
public interface ICoordinatorActions {
    /**
     * Subscribe to updates of a specific ticker
     * @param tickerType the ticker type to subscribe to
     */
    void subscribe(TickerType tickerType);

    /**
     * connect to a given platform
     * @param platformName the platform to connect
     */
    void connect(PlatformName platformName);
    /**
     * Unsubscribe from updates of a specific ticker.
     *
     * @param tickerType the ticker type to unsubscribe from
     */
    void unsubscribe(TickerType tickerType);
    /**
     * Disconnect from a given platform.
     *
     * @param platformName the platform to disconnect from
     */
    void disconnect(PlatformName platformName);
}
