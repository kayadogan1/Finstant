package com.dogankaya.FinanStream.abscraction;

import rate.RateDto;
import rate.RateStatus;

public interface ICoordinatorCallback {
    void onConnect(String platformName, Boolean status);
    void onDisConnect(String platformName, Boolean status);
    void onRateAvailable(String platformName, String rateName, RateDto rateDto);
    void onRateUpdate(String platformName, String rateName, RateDto rateDto);
    void onRateStatus(String platformName, String rateName, RateStatus rateStatus);
}
