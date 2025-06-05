package com.dogankaya.FinanStream.abscraction;

/**
 *Handler interface used in PlatformHandlers for platform-level operations such as connecting,
 * disconnecting and managing subscribers
 */
public interface IPlatformHandler {
    /**
     * Connect to the platform with given user credentials.
     *
     * @param platformName the platform name
     * @param userid       user identifier
     * @param password     user password
     */
    void connect(String platformName, String userid, String password);

    /**
     * Disconnect from the platform with given user credentials.
     *
     * @param platformName the platform name
     * @param userid       user identifier
     * @param password     user password
     */
    void disConnect(String platformName, String userid, String password);

    /**
     * Subscribe to a specific rate on the platform.
     *
     * @param platformName the platform name
     * @param rateName     the name of the rate to subscribe to
     */
    void subscribe(String platformName, String rateName);

    /**
     * Unsubscribe from a specific rate on the platform.
     *
     * @param platformName the platform name
     * @param rateName     the name of the rate to unsubscribe from
     */
    void unSubscribe(String platformName, String rateName);

    /**
     * Returns the name of the platform.
     *
     * @return platform name as a string
     */
    String getPlatformName();
}
