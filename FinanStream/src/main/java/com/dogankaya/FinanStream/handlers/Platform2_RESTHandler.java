package com.dogankaya.FinanStream.handlers;

import com.dogankaya.FinanStream.abscraction.ICoordinatorCallback;
import com.dogankaya.FinanStream.abscraction.IPlatformHandler;
import com.dogankaya.FinanStream.helpers.FinanStreamProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rate.RateDto;
import rate.RateStatus;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 * The {@code Platform2_RESTHandler} class implements REST-based data retrieval for Platform2.
 * <p>
 * This handler periodically polls a REST API endpoint to fetch rate updates for active subscriptions
 * and notifies the higher-level coordinator via callback.
 * <p>
 * Active subscriptions are managed in a thread-safe manner and persisted in Redis to maintain state across restarts.
 * <p>
 * HTTP requests are sent using Java's {@link java.net.http.HttpClient}, and JSON responses are
 * deserialized into {@code RateDto} objects using Jackson's {@link com.fasterxml.jackson.databind.ObjectMapper}.
 * <p>
 * Implements the {@link IPlatformHandler} interface.
 *
 * @see IPlatformHandler
 * @see ICoordinatorCallback
 */
public class Platform2_RESTHandler implements IPlatformHandler {
    private static final Logger logger = LogManager.getLogger(Platform2_RESTHandler.class);

    private final String platformName;
    private final String API_REQUEST_URL;
    private final Map<String,Boolean> activeSubscriptions = new ConcurrentHashMap<>();
    private volatile boolean running = false;
    private Thread restThread;

    private final ICoordinatorCallback callback;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    /**
     * Constructs a new Platform2_RESTHandler instance.
     *
     * @param callback          The callback interface for rate updates and connection events.
     * @param finanStreamProperties Configuration properties containing platform details.
     */
    public Platform2_RESTHandler(ICoordinatorCallback callback, FinanStreamProperties finanStreamProperties) {
        FinanStreamProperties.PlatformProperties platformProperties = finanStreamProperties.getPlatformProperties("platform2");
        this.platformName = platformProperties.getName();
        this.API_REQUEST_URL = "http://" + platformProperties.getHost() + ":" + platformProperties.getPort() + "/api/rates/%7BtickerType%7D";
        this.callback = callback;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(5))
                .build();
    }
    /**
     * Establishes connection to the platform and starts the polling thread.
     * <p>
     * If already connected, this method will not start a new thread.
     *
     * @param platformName The name of the platform to connect to.
     * @param userid      The user ID (currently unused).
     * @param password    The password (currently unused).
     */
    @Override
    public void connect(String platformName, String userid, String password) {
        if (restThread != null && restThread.isAlive()) {
            logger.warn("Already connected and running.");
            return;
        }

        running = true;
        restThread = new Thread(() -> {
            logger.info("REST polling thread started for platform: {}", platformName);

            while (running) {
                try {
                    if (activeSubscriptions.isEmpty()) {
                        Thread.sleep(1000);
                        continue;
                    }
                    StringBuilder url = new StringBuilder(API_REQUEST_URL + "?");
                    for(String rateName : activeSubscriptions.keySet()) {
                        url
                                .append("request=")
                                .append(rateName)
                                .append("&");
                    }

                    logger.info("Sending GET request to {}", url.toString());
                    HttpRequest request = HttpRequest.newBuilder(URI.create(url.toString())).GET().build();
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    int responseCode = response.statusCode();
                    if (responseCode != 200) {
                        logger.warn("REST status code for {}, url:{}", response.body(), url.toString());
                    }else{
                        String responseBody = response.body();
                        RateDto[] rateDtos = objectMapper.readValue(responseBody, RateDto[].class);
                        for (RateDto rateDto : rateDtos) {
                            callback.onRateUpdate(platformName, rateDto.getRateName(), rateDto);
                        }
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.info("REST polling thread interrupted.");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error in REST polling thread: {}", e.getMessage(), e);
                }
            }

            logger.info("REST polling thread stopped for platform: {}", platformName);
        });

        restThread.start();
        callback.onConnect(platformName, true);
    }
    /**
     * Disconnects from the platform and stops the polling thread.
     * <p>
     * Clears active subscriptions from Redis and notifies callback.
     *
     * @param platformName The name of the platform to disconnect from.
     * @param userid      The user ID (currently unused).
     * @param password    The password (currently unused).
     */
    @Override
    public void disConnect(String platformName, String userid, String password) {
        running = false;
        if(restThread != null && restThread.isAlive()) {
            restThread.interrupt();
            try{
                restThread.join(1000);
            }catch(InterruptedException e){
                logger.warn("Interrupted while stopping the REST thread.", e);
            }
        }
        activeSubscriptions.clear();
        callback.onDisConnect(platformName, true);
    }

    /**
     * Subscribe to updates for the specified rate
     * if already subscribed this method does nothing
     * Add the rate name to the Redis persistent subscription set.
     * @param platformName the platform name
     * @param rateName     the name of the rate to subscribe to
     */
    @Override
    public void subscribe(String platformName, String rateName) {
        if(activeSubscriptions.containsKey(rateName)) {
            logger.info("Already Subscribed {}", rateName);
            return;
        }
        activeSubscriptions.put(rateName, true);
        logger.info("Subscribed to {}", rateName);
    }

    /**
     * Unsubscribe from updates fot he specified rate
     * @param platformName the platform name
     * @param rateName     the name of the rate to unsubscribe from
     */
    @Override
    public void unSubscribe(String platformName, String rateName) {

        if (!activeSubscriptions.containsKey(rateName)) {
            logger.info("Already Unsubscribed from {}", rateName);
            return;
        }
        activeSubscriptions.remove(rateName);
        callback.onRateStatus(platformName, rateName, RateStatus.NOT_AVAILABLE);
        logger.info("Unsubscribed from {}", rateName);

    }

    /**
     * returns the platform name
     * @return value of platform
     */
    @Override
    public String getPlatformName() {
        return platformName;
    }
}
