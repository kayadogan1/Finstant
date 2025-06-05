package platform1_telnet;

import enums.TickerType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import platform1_telnet.handlers.TelnetServerHandler;
import platform1_telnet.helpers.ConfigurationHelper;
import platform1_telnet.services.FinancialDataGenerator;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The {@code Platform1_Telnet} class serves as the entry point for the Telnet server application.
 * It initializes the server, loads configuration properties, starts the financial data generation,
 * and manages incoming client connections. Each client connection is handled in a separate thread
 * from a fixed-size thread pool.
 */
public class Platform1_Telnet {
    private static final Logger logger = LogManager.getLogger(Platform1_Telnet.class);
    private static final int THREAD_POOL_SIZE = 10;
    /**
     * The main method to start the Telnet server.
     * It performs the following steps:
     * 1. Loads server port and supported tickers from configuration.
     * 2. Initializes a fixed thread pool for handling client connections.
     * 3. Starts the {@link FinancialDataGenerator} to produce market data.
     * 4. Enters a loop to accept incoming client connections, submitting each to the thread pool.
     * 5. Handles potential {@link IOException} during server operation and ensures proper cleanup.
     *
     * @param args Command line arguments (not used in this application).
     */
    public static void main(String[] args) {
        int port = ConfigurationHelper.getServerPort();
        TickerType[] supportedTickers = ConfigurationHelper.getSupportedTickers();
        ExecutorService clientThreadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        FinancialDataGenerator financialDataGenerator = new FinancialDataGenerator(supportedTickers);
        financialDataGenerator.startGenerating(TelnetServerHandler::distributeMarketData);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Telnet Server started on port {}", port);
            logger.info("Supported Tickers: {}", Arrays.toString(supportedTickers));

            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("New client connected: {}", clientSocket.getInetAddress());
                clientThreadPool.submit(new TelnetServerHandler(clientSocket));
            }
        } catch (IOException e) {
            logger.error("Server error: {}", e.getMessage(), e);
        } finally {
            financialDataGenerator.stopGenerating();
            clientThreadPool.shutdown();
            logger.info("Telnet Server stopped.");
        }
    }
}