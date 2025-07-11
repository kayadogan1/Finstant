package platform1_telnet.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import enums.TickerType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import platform1_telnet.helpers.ConfigurationHelper;
import rate.RateDto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@code TelnetServerHandler} class runs on a separate thread for each connected Telnet client.
 * It manages client connections, processes incoming commands (subscribe, unsubscribe, exit),
 * and distributes market data to the relevant subscribers.
 */
public class TelnetServerHandler implements Runnable {
    private static final Logger logger = LogManager.getLogger(TelnetServerHandler.class);
    private static final TickerType[] supportedTickers = ConfigurationHelper.getSupportedTickers();
    private final Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private final Set<String> subscribedTickers = Collections.synchronizedSet(new HashSet<>());
    private static final ConcurrentHashMap<String, Set<TelnetServerHandler>> subscribers = new ConcurrentHashMap<>();

    private static final ObjectMapper objectMapper = new ObjectMapper();
    static {
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS").withZone(ZoneOffset.UTC);
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        objectMapper.registerModule(javaTimeModule);
    }

    /**
     * Constructs a new {@code TelnetServerHandler} for the given client socket.
     *
     * @param socket the connected client socket
     */
    public TelnetServerHandler(Socket socket) {
        this.clientSocket = socket;
    }

    /**
     * Runs the client handler thread, reading commands from the client socket,
     * processing subscription requests, unsubscriptions, and exit commands.
     */
    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String line;
            while ((line = in.readLine()) != null) {
                logger.info("Received from {}: {}", clientSocket.getInetAddress(), line);
                processCommand(line);
            }
        } catch (IOException e) {
            logger.warn("Client disconnected unexpectedly or error: {}", e.getMessage());
        } finally {
            cleanup();
        }
    }

    /**
     * Parses and processes a single command string received from the client.
     * Supported commands are "subscribe|ticker", "unsubscribe|ticker", and "exit".
     *
     * @param command the raw command string from the client
     */
    private void processCommand(String command) {
        command = command.trim();
        if (command.startsWith("subscribe|")) {
            String ticker = command.substring("subscribe|".length());
            subscribe(ticker);
        } else if (command.startsWith("unsubscribe|")) {
            String ticker = command.substring("unsubscribe|".length());
            unsubscribe(ticker);
        } else if (command.equals("exit")) {
            sendResponse("Good-bye!");
            try {
                clientSocket.close();
            } catch (IOException e) {
                logger.error("Error closing client socket", e);
            }
        } else {
            sendResponse("ERROR|Invalid request format");
        }
    }

    /**
     * Subscribes the client to market data updates for the given ticker.
     * Validates ticker support and notifies client of subscription status.
     *
     * @param tickerString the ticker name string
     */
    private void subscribe(String tickerString) {
        TickerType tickerEnum = TickerType.fromString(tickerString);

        if (tickerEnum == null) {
            sendResponse("ERROR|Rate data not found for " + tickerString);
            return;
        }

        if (!Arrays.asList(supportedTickers).contains(tickerEnum)) {
            sendResponse("ERROR|Rate not supported for " + tickerString);
            return;
        }

        String actualTickerValue = tickerEnum.getValue();

        if (subscribedTickers.add(actualTickerValue)) {
            subscribers.computeIfAbsent(actualTickerValue, k -> Collections.synchronizedSet(new HashSet<>())).add(this);
            sendResponse("Subscribed to " + actualTickerValue);
            logger.info("Client {} subscribed to {}", clientSocket.getInetAddress(), actualTickerValue);
        } else {
            sendResponse("Already subscribed to " + actualTickerValue);
        }
    }

    /**
     * Unsubscribes the client from market data updates for the given ticker.
     * Notifies client of unsubscription status.
     *
     * @param tickerString the ticker name string
     */
    private void unsubscribe(String tickerString) {
        TickerType tickerEnum = TickerType.fromString(tickerString);

        if (tickerEnum == null) {
            sendResponse("ERROR|Invalid ticker for unsubscribe: " + tickerString);
            return;
        }

        String actualTickerValue = tickerEnum.getValue();

        if (subscribedTickers.remove(actualTickerValue)) {
            Set<TelnetServerHandler> tickerSubscribers = subscribers.get(actualTickerValue);
            if (tickerSubscribers != null) {
                tickerSubscribers.remove(this);
                if (tickerSubscribers.isEmpty()) {
                    subscribers.remove(actualTickerValue);
                }
            }
            sendResponse("Unsubscribed from " + actualTickerValue);
            logger.info("Client {} unsubscribed from {}", clientSocket.getInetAddress(), actualTickerValue);
        } else {
            sendResponse("Not subscribed to " + actualTickerValue);
        }
    }

    /**
     * Sends a message back to the connected client.
     *
     * @param message the message string to send
     */
    public void sendResponse(String message) {
        if (out != null && !clientSocket.isClosed()) {
            out.println(message);
            logger.info("Sent to {}: {}", clientSocket.getInetAddress(), message);
        }
    }

    /**
     * Distributes a {@link RateDto} market data update to all clients subscribed to its ticker.
     * Serializes the data to JSON before sending.
     *
     * @param data the market data update to distribute
     */
    public static void distributeMarketData(RateDto data) {
        String ticker = data.getRateName();

        Set<TelnetServerHandler> tickerSubscribers = subscribers.get(ticker);
        if (tickerSubscribers != null) {
            try {
                String marketDataJson = objectMapper.writeValueAsString(data);
                for (TelnetServerHandler handler : tickerSubscribers) {
                    handler.sendResponse(marketDataJson);
                }
            } catch (JsonProcessingException e) {
                logger.error("Error serializing market data: {}", e.getMessage());
            }
        }
    }

    /**
     * Cleans up resources when the client disconnects.
     * Removes this handler from all subscription lists and closes streams and sockets.
     */
    private void cleanup() {
        logger.info("Client disconnected: {}", clientSocket.getInetAddress());
        for (String ticker : new HashSet<>(subscribedTickers)) {
            Set<TelnetServerHandler> tickerSubscribers = subscribers.get(ticker);
            if (tickerSubscribers != null) {
                tickerSubscribers.remove(this);
                if (tickerSubscribers.isEmpty()) {
                    subscribers.remove(ticker);
                }
            }
        }
        subscribedTickers.clear();

        try {
            if (in != null) in.close();
            if (out != null) out.close();
            clientSocket.close();
        } catch (IOException e) {
            logger.error("Error closing client resources", e);
        }
    }
}
