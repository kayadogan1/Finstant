package com.dogankaya.FinanStream.abscraction;

import Rate.Rate;
import Rate.RateDto;
import Rate.RateStatus;

public interface ICoordinatorCallback {
    void onConnect(String platformName, Boolean status);
    void onDisConnect(String platformName, Boolean status);
    void onRateAvailable(String platformName, String rateName, Rate rate);
    void onRateUpdate(String platformName, String rateName, RateDto rateDto);
    void onRateStatus(String platformName, String rateName, RateStatus rateStatus);
}
