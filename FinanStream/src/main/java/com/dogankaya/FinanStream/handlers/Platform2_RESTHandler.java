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

public class Platform2_RESTHandler implements IPlatformHandler {
    private static final Logger logger = LogManager.getLogger(Platform2_RESTHandler.class);

    private final String platformName;
    private final String API_REQUEST_URL;
    private String queryParams = "";

    private volatile boolean running = false;
    private Thread restThread;

    private final ICoordinatorCallback callback;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public Platform2_RESTHandler(ICoordinatorCallback callback, FinanStreamProperties finanStreamProperties) {
        FinanStreamProperties.PlatformProperties platformProperties = finanStreamProperties.getPlatformProperties("platform2");
        this.platformName = platformProperties.getName();
        // DÜZELTME: Port ve Host yer değiştirmişti, doğru sıraya koyuldu!
        this.API_REQUEST_URL = "http://" + platformProperties.getHost() + ":" + platformProperties.getPort() + "/api/rates/%7BtickerType%7D?";
        this.callback = callback;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(5))
                .build();
    }

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
                    if (queryParams.isEmpty()) {
                        Thread.sleep(1000); // boşsa CPU’yu yormasın
                        continue;
                    }

                    String url = API_REQUEST_URL + queryParams;
                    logger.debug("Sending GET request to: {}", url);

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(java.time.Duration.ofSeconds(5))
                            .GET()
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    int responseCode = response.statusCode();
                    if (responseCode == 200) {
                        String responseBody = response.body();
                        RateDto[] rateDtos = objectMapper.readValue(responseBody, RateDto[].class);

                        for (RateDto dto : rateDtos) {
                            callback.onRateUpdate(platformName, dto.getRateName(), dto);
                        }
                    } else {
                        logger.warn("REST response code: {}", responseCode);
                    }

                    Thread.sleep(1000); // istekler arasında bekleme
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

    @Override
    public void disConnect(String platformName, String userid, String password) {
        running = false;
        if (restThread != null) {
            restThread.interrupt();
            try {
                restThread.join(1000);
            } catch (InterruptedException e) {
                logger.warn("Interrupted while stopping the REST thread.", e);
            }
        }
        queryParams = "";
        callback.onDisConnect(platformName, true);
    }

    @Override
    public void subscribe(String platformName, String rateName) {
        if (queryParams.contains(rateName)) {
            logger.info("Already Subscribed {}", rateName);
            return;
        }
        queryParams += "request=" + rateName + "&";
        logger.info("Subscribed to {}", rateName);
    }

    @Override
    public void unSubscribe(String platformName, String rateName) {
        if (!queryParams.contains(rateName)) {
            logger.info("Already Unsubscribed {}", rateName);
            return;
        }
        queryParams = queryParams.replace("request=" + rateName + "&", "");
        callback.onRateStatus(platformName, rateName, RateStatus.NOT_AVAILABLE);
        logger.info("Unsubscribed from {}", rateName);
    }

    @Override
    public String getPlatformName() {
        return platformName;
    }
}
