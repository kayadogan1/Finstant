package com.dogankaya.FinanStream.handlers;

import com.dogankaya.FinanStream.helpers.FinanStreamProperties;
import rate.RateDto;
import rate.RateStatus;
import com.dogankaya.FinanStream.abscraction.ICoordinatorCallback;
import com.dogankaya.FinanStream.abscraction.IPlatformHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class Platform1_TelnetHandler implements IPlatformHandler {

    private static final Logger logger = LogManager.getLogger(Platform1_TelnetHandler.class);

    private final int telnetPort;
    private final String telnetHost;
    private final String platformName;

    private final ICoordinatorCallback callback;
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private final ObjectMapper objectMapper;

    public Platform1_TelnetHandler(ICoordinatorCallback callback, FinanStreamProperties finanStreamProperties) {
        FinanStreamProperties.PlatformProperties platformProperties = finanStreamProperties.getPlatformProperties("platform1");
        this.telnetPort = platformProperties.getPort();
        this.telnetHost = platformProperties.getHost();
        this.platformName = platformProperties.getName();
        this.callback = callback;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void connect(String platformName, String userid, String password) {
        try {
            this.socket = new Socket(telnetHost, telnetPort);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            new Thread(() -> {
                try {
                    callback.onConnect(platformName, true);
                    String line;
                    while ((line = reader.readLine()) != null) {
                        try {
                            RateDto dto = objectMapper.readValue(line, RateDto.class);
                            callback.onRateUpdate(platformName, dto.getRateName(), dto);
                        } catch (Exception e) {
                            logger.error("JSON parse error: {} | Line: {}", e.getMessage(), line, e);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error in client thread: {}", e.getMessage(), e);
                    callback.onConnect(platformName, false);
                }
            }).start();

        } catch (Exception e) {
            logger.error("Error connecting to Telnet server: {}", e.getMessage(), e);
            callback.onConnect(platformName, false);
        }
    }

    @Override
    public void disConnect(String platformName, String userid, String password) {
        try {
            if (socket != null) socket.close();
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            callback.onDisConnect(platformName, true);
        } catch (Exception e) {
            logger.error("Error during disconnect: {}", e.getMessage(), e);
            callback.onDisConnect(platformName, false);
        }
    }

    @Override
    public void subscribe(String platformName, String rateName) {
        try {
            if (writer != null) {
                writer.write("subscribe|" + rateName + "\n");
                writer.flush();
                callback.onRateAvailable(platformName, rateName, null);
            } else {
                logger.warn("Writer not initialized. Call connect() first.");
            }
        } catch (Exception e) {
            logger.error("Error subscribing to rate {}: {}", rateName, e.getMessage(), e);
        }
    }

    @Override
    public void unSubscribe(String platformName, String rateName) {
        try {
            if (writer != null) {
                writer.write("unsubscribe|" + rateName + "\n");
                writer.flush();
                callback.onRateStatus(platformName, rateName, RateStatus.NOT_AVAILABLE);
            } else {
                logger.warn("Writer not initialized. Call connect() first.");
            }
        } catch (Exception e) {
            logger.error("Error unsubscribing from rate {}: {}", rateName, e.getMessage(), e);
        }
    }

    @Override
    public String getPlatformName() {
        return this.platformName;
    }
}
