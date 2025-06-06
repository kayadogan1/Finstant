package com.dogankaya.FinanStream;

import com.dogankaya.FinanStream.abscraction.ICoordinatorActions;
import com.dogankaya.FinanStream.abscraction.IPlatformHandler;
import com.dogankaya.FinanStream.helpers.FinanStreamProperties;
import com.dogankaya.FinanStream.helpers.HandlerClassLoader;
import com.dogankaya.FinanStream.services.CalculatorService;
import enums.PlatformName;
import enums.TickerType;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import rate.RateDto;
import rate.RateStatus;
import com.dogankaya.FinanStream.abscraction.ICoordinatorCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

/**
 * The {@code Coordinator} class is the central component that manages financial data streaming.
 * <p>
 * It implements both {@link ICoordinatorCallback} and {@link ICoordinatorActions} interfaces,
 * coordinating connections with platforms and handling rate updates.
 * <p>
 * This class also uses Redis (via {@link RedisTemplate}) to store and manage rate data.
 * Platform handlers are dynamically loaded using {@link HandlerClassLoader}.
 * </p>
 *
 */
@SpringBootApplication
public class Coordinator implements ICoordinatorCallback, ICoordinatorActions {
	private static final Logger logger = LogManager.getLogger(Coordinator.class);
	private final List<IPlatformHandler> platformHandlers;
	private final HashOperations<String, String, RateDto> hashOperations;
	private final CalculatorService calculatorService;

	public static void main(String[] args) {
		SpringApplication.run(Coordinator.class, args);
	}
	/**
	 * Constructs a new {@code Coordinator} instance.
	 *
	 * @param finanStreamProperties Properties for configuring the financial stream.
	 * @param redisTemplate         Redis template for caching .
	 */
	Coordinator(FinanStreamProperties finanStreamProperties, RedisTemplate<String, Object> redisTemplate, CalculatorService calculatorService) {
		platformHandlers = HandlerClassLoader.getHandlerInstances(finanStreamProperties.getHandlerClassNames(), this, finanStreamProperties);
		this.hashOperations = redisTemplate.opsForHash();
        this.calculatorService = calculatorService;
    }

	/**
	 * Called when a platform connection is established or fails.
	 *
	 * @param platformName The name of the platform.
	 * @param status       The connection status.
	 */
	@Override
	public void onConnect(String platformName, Boolean status) {
		if (status) {
			logger.info("Connected to {}", platformName);
		} else {
			logger.error("Cannot connect to {}", platformName);
		}
	}

	/**
	 * Called when a platform disconnection occurs
	 * @param platformName name of the platform
	 * @param status       true if disconnection succeeded, false otherwise
	 */
	@Override
	public void onDisConnect(String platformName, Boolean status) {
		if (status) {
			logger.info("Disconnected from {} successfully", platformName);
		} else {
			logger.error("Cannot disconnect from {}", platformName);
		}
	}

	/**
	 * Called when a new rate available from a platform
	 * @param platformName name of the platform
	 * @param rateName    the name of the rate
	 * @param rateDto     rate details
	 */
	@Override
	public void onRateAvailable(String platformName, String rateName, RateDto rateDto) {
		logger.info("{} is available from {}", rateName, platformName);
		if (rateDto != null) {
			logger.info("Rate: {}", rateDto);
		}
	}
	/**
	 * Called when the status of a rate changes.
	 *
	 * @param platformName The name of the platform.
	 * @param rateName     The name of the rate.
	 * @param rateStatus   The updated status.
	 */
	@Override
	public void onRateStatus(String platformName, String rateName, RateStatus rateStatus) {
		logger.info("Status for {} from {} is {}", rateName, platformName, rateStatus);
	}
	/**
	 * Called when a rate is updated.
	 * The updated rate is stored in Redis.
	 *
	 * @param platformName The name of the platform.
	 * @param rateName     The name of the rate.
	 * @param rateDto      The updated rate data.
	 */
	@Override
	public void onRateUpdate(String platformName, String rateName, RateDto rateDto) {
		logger.info("{} from {} updated to {}", rateName, platformName, rateDto);
		hashOperations.put(TickerType.getHashNameFromPlatformName(platformName),
				rateName,
				rateDto);
		calculatorService.calculateAffectedRates(rateDto);
	}
	/**
	 * Subscribes to a specific ticker type on the platform.
	 *
	 * @param tickerType The ticker type to subscribe to.
	 */
	@Override
	public void subscribe(TickerType tickerType) {
		IPlatformHandler platformHandler = getPlatformHandler(tickerType.getPlatformName());
		if (platformHandler != null) {
			platformHandler.subscribe(tickerType.getPlatformName(), tickerType.getValue());
		}
	}
	/**
	 * Connects to the specified platform.
	 *
	 * @param platformName The platform to connect to.
	 */
	@Override
	public void connect(PlatformName platformName) {
		IPlatformHandler platformHandler = getPlatformHandler(platformName.getName());
		if (platformHandler != null) {
			platformHandler.connect(platformName.getName(), "", "");
		}
	}
	/**
	 * Unsubscribes from a specific ticker type and removes it from Redis.
	 *
	 * @param tickerType The ticker type to unsubscribe from.
	 */
	@Override
	public void unsubscribe(TickerType tickerType) {
		IPlatformHandler platformHandler = getPlatformHandler(tickerType.getPlatformName());
		if (platformHandler != null) {
			platformHandler.unSubscribe(tickerType.getPlatformName(), tickerType.getValue());
		}
	}

	/**
	 * disconnect from a specific platform and removes its tickers  from Redis
	 * @param platformName the platform to disconnect from
	 */
	@Override
	public void disconnect(PlatformName platformName) {
		IPlatformHandler platformHandler = getPlatformHandler(platformName.getName());
		if (platformHandler != null) {
			platformHandler.disConnect(platformName.getName(),"" ,"");
		}
	}
	/**
	 * Retrieves the appropriate platform handler for the given platform name.
	 *
	 * @param platformName The name of the platform.
	 * @return The platform handler instance, or {@code null} if not found.
	 */
	private IPlatformHandler getPlatformHandler(String platformName) {
		IPlatformHandler platformHandler = platformHandlers.stream()
				.filter(p -> p.getPlatformName().equals(platformName))
				.findFirst()
				.orElse(null);

		if(platformHandler == null) {
			logger.error("Platform {} not found", platformName);
		}

		return platformHandler;
	}
}
