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
import java.net.SocketException;
/**
 * Implementation of IPlatformHandler for handling Telnet-based platform connection.
 * Connects to a Telnet server, listens for JSON-encoded rate updates, and sends subscribe/unsubscribe commands.
 */
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

    /**
     * Constructor for Platform1_TelnetHandler
     * @param callback callback interface to report events
     * @param finanStreamProperties application properties containing connection info
     */
    public Platform1_TelnetHandler(ICoordinatorCallback callback, FinanStreamProperties finanStreamProperties) {
        FinanStreamProperties.PlatformProperties platformProperties = finanStreamProperties.getPlatformProperties("platform1");
        this.telnetPort = platformProperties.getPort();
        this.telnetHost = platformProperties.getHost();
        this.platformName = platformProperties.getName();
        this.callback = callback;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    /**
     * Connects to the Telnet server and starts a listener thread for incoming rate updates.
     *
     * @param platformName platform name to connect
     * @param userid      user identifier (not used in this implementation)
     * @param password    password (not used in this implementation)
     */
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
                    if(e instanceof SocketException) {
                        logger.info("Socket Closed");
                        return;
                    }
                    logger.error("Error in client thread: {}", e.getMessage(), e);
                    callback.onConnect(platformName, false);
                }
            }).start();

        } catch (Exception e) {
            logger.error("Error connecting to Telnet server: {}", e.getMessage(), e);
            callback.onConnect(platformName, false);
        }
    }

    /**
     * Disconnect from the telnet server and runs callback
     * @param platformName the platform name
     * @param userid       user identifier
     * @param password     user password
     */
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

    /**
     * sends a subscribe command for the given rateName to the telnet server
     * @param platformName the platform name
     * @param rateName     the name of the rate to subscribe to
     */
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
    /**
     * Sends an unsubscribe command for the given rateName to the Telnet server.
     *
     * @param platformName platform name
     * @param rateName    rate to unsubscribe from
     */
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

    /**
     * returns value of platform name
     * @return the string value of platform name
     */
    @Override
    public String getPlatformName() {
        return this.platformName;
    }
}
