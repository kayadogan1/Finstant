package platform1_telnet.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rate.RateDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import enums.TickerType;
import platform1_telnet.helpers.ConfigurationHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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
    /**
     * Constructs a new {@code TelnetServerHandler} instance.
     * This instance is used to communicate with a specific client socket.
     *
     * @param socket The {@link Socket} object used for communication with the client.
     */
    public TelnetServerHandler(Socket socket) {
        this.clientSocket = socket;
    }
    /**
     * The main execution method that manages client communication.
     * It reads commands from the client, processes them, and sends back responses
     * until the connection is closed or an error occurs.
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
     * Parses the incoming command string and initiates the appropriate action.
     * Supported commands include: "subscribe|ticker", "unsubscribe|ticker", and "exit".
     * Sends an error response for invalid command formats.
     *
     * @param command The command string received from the client.
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
     * Subscribes the current client to the specified ticker.
     * Sends an error response if the ticker is invalid or not supported.
     * Adds the subscribed ticker to the client's internal subscription list and the global subscriber map.
     *
     * @param tickerString The string representation of the ticker to subscribe to (e.g., "EURUSD").
     */
    private void subscribe(String tickerString) {
        TickerType tickerEnum = TickerType.fromString(tickerString);

        if (tickerEnum == null) {
            sendResponse("ERROR|Rate data not found for " + tickerString);
            return;
        }

        if(!Arrays.asList(supportedTickers).contains(tickerEnum)) {
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
     * Unsubscribes the current client from the specified ticker.
     * May send an error response if the ticker is invalid.
     * Removes the client from its internal subscription list and the global subscriber map.
     *
     * @param tickerString The string representation of the ticker to unsubscribe from.
     */
    private void unsubscribe(String tickerString) {
        TickerType tickerEnum = TickerType.fromString(tickerString);

        if (tickerEnum == null) {
            sendResponse("ERROR|Invalid ticker for unsubscribe: " + tickerString); // Geçersiz ticker durumunda hata verilebilir
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
     * Sends a message to the connected client.
     * The message is sent only if the output stream is not null and the client socket is still open.
     *
     * @param message The message string to be sent to the client.
     */
    public void sendResponse(String message) {
        if (out != null && !clientSocket.isClosed()) {
            out.println(message);
        }
    }
    /**
     * Distributes a given market data object (RateDto) to all relevant subscribers.
     * Converts the incoming {@link RateDto} object to a JSON string and sends it to all
     * {@code TelnetServerHandler} instances that are subscribed to that specific ticker.
     *
     * @param data The {@link RateDto} object containing the market data to be distributed.
     */
    public static void distributeMarketData(RateDto data) {
        String ticker = data.getRateName();

        Set<TelnetServerHandler> tickerSubscribers = subscribers.get(ticker);
        if (tickerSubscribers != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            try {
                String marketDataJson = objectMapper.writeValueAsString(data);
                for (TelnetServerHandler handler : tickerSubscribers) {
                    handler.sendResponse(marketDataJson);
                }
            } catch (JsonProcessingException e) {
                logger.error(e.getMessage());
            }
        }
    }
    /**
     * Performs cleanup operations when a client disconnects or an error occurs.
     * This includes removing the client from all subscribed ticker lists and closing
     * input/output streams and the client socket to release resources.
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
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            logger.error("Error closing client resources", e);
        }
    }
}