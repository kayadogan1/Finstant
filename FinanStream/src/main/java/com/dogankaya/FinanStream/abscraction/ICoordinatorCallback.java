package com.dogankaya.FinanStream.abscraction;

import rate.RateDto;
import rate.RateStatus;

/**
 * Callback interface for coordinator events such as connection status and rate updates.
 */
public interface ICoordinatorCallback {

    /**
     * Called when a connection attempt is made.
     *
     * @param platformName name of the platform
     * @param status       true if connection succeeded, false otherwise
     */
    void onConnect(String platformName, Boolean status);

    /**
     * Called when a disconnection occurs.
     *
     * @param platformName name of the platform
     * @param status       true if disconnection succeeded, false otherwise
     */
    void onDisConnect(String platformName, Boolean status);

    /**
     * Called when a new rate is available.
     *
     * @param platformName name of the platform
     * @param rateName    the name of the rate
     * @param rateDto     rate details
     */
    void onRateAvailable(String platformName, String rateName, RateDto rateDto);

    /**
     * Called when an existing rate is updated.
     *
     * @param platformName name of the platform
     * @param rateName    the name of the rate
     * @param rateDto     updated rate details
     */
    void onRateUpdate(String platformName, String rateName, RateDto rateDto);

    /**
     * Called to report the status of a rate.
     *
     * @param platformName name of the platform
     * @param rateName    the name of the rate
     * @param rateStatus  current status of the rate
     */
    void onRateStatus(String platformName, String rateName, RateStatus rateStatus);
}
