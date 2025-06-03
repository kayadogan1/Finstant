package finstant.platform1_telnet.handlers;

import enums.TickerType;
import finstant.platform1_telnet.helpers.ConfigurationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class TelnetServerHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(TelnetServerHandler.class);
    private static final TickerType[] supportedTickers = ConfigurationHelper.getSupportedTickers();
    private final Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private final Set<String> subscribedTickers = Collections.synchronizedSet(new HashSet<>());
    private static final ConcurrentHashMap<String, Set<TelnetServerHandler>> subscribers = new ConcurrentHashMap<>();

    public TelnetServerHandler(Socket socket) {
        this.clientSocket = socket;
    }

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

    public void sendResponse(String message) {
        if (out != null && !clientSocket.isClosed()) {
            out.println(message);
        }
    }

    public static void distributeMarketData(String marketData) {
        String[] parts = marketData.split("\\|");
        if (parts.length > 0) {
            String ticker = parts[0];
            Set<TelnetServerHandler> tickerSubscribers = subscribers.get(ticker);
            if (tickerSubscribers != null) {
                for (TelnetServerHandler handler : tickerSubscribers) {
                    handler.sendResponse(marketData);
                }
            }
        }
    }

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