package com.dogankaya.FinanStream.abscraction;

public interface IPlatformHandler {
    void connect(String platformName, String userid, String password);
    void disConnect(String platformName, String userid, String password);
    void subscribe(String platformName, String rateName);
    void unSubscribe(String platformName, String rateName);
}
