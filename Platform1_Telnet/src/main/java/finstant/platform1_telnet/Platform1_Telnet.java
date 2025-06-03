package finstant.platform1_telnet;

import finstant.platform1_telnet.handlers.TelnetServerHandler;
import finstant.platform1_telnet.helpers.ConfigurationHelper;
import finstant.platform1_telnet.services.FinancialDataGenerator;
import enums.TickerType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Platform1_Telnet {
    private static final int THREAD_POOL_SIZE = 10;

    public static void main(String[] args) {
        int port = ConfigurationHelper.getServerPort();
        TickerType[] supportedTickers = ConfigurationHelper.getSupportedTickers();
        ExecutorService clientThreadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        FinancialDataGenerator financialDataGenerator = new FinancialDataGenerator(supportedTickers);
        financialDataGenerator.startGenerating(TelnetServerHandler::distributeMarketData);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.printf("Telnet Server started on port %d%n", port);
            System.out.printf("Supported Tickers: %s%n", Arrays.toString(supportedTickers));

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.printf("New client connected: %s%n", clientSocket.getInetAddress());
                clientThreadPool.submit(new TelnetServerHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.printf("Server error: %s%n", e.getMessage());
        } finally {
            financialDataGenerator.stopGenerating();
            clientThreadPool.shutdown();
            System.out.println("Telnet Server stopped.");
        }
    }
}