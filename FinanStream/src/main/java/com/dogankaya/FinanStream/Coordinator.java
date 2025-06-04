package com.dogankaya.FinanStream;

import com.dogankaya.FinanStream.abscraction.ICoordinatorActions;
import com.dogankaya.FinanStream.abscraction.IPlatformHandler;
import com.dogankaya.FinanStream.helpers.FinanStreamProperties;
import com.dogankaya.FinanStream.helpers.HandlerClassLoader;
import enums.PlatformName;
import enums.TickerType;
import rate.Rate;
import rate.RateDto;
import rate.RateStatus;
import com.dogankaya.FinanStream.abscraction.ICoordinatorCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

@SpringBootApplication
public class Coordinator implements ICoordinatorCallback, ICoordinatorActions {
	private static final Logger logger = LogManager.getLogger(Coordinator.class);
	private final List<IPlatformHandler> platformHandlers;

	public static void main(String[] args) {
		SpringApplication.run(Coordinator.class, args);
	}

	Coordinator(FinanStreamProperties finanStreamProperties) {
		platformHandlers = HandlerClassLoader.getHandlerInstances(finanStreamProperties.getHandlerClassNames(), this, finanStreamProperties);
	}

	@Override
	public void onConnect(String platformName, Boolean status) {
		if (status) {
			logger.info("Connected to {}", platformName);
		} else {
			logger.error("Cannot connect to {}", platformName);
		}
	}

	@Override
	public void onDisConnect(String platformName, Boolean status) {
		if (status) {
			logger.info("Disconnected from {} successfully", platformName);
		} else {
			logger.error("Cannot disconnect from {}", platformName);
		}
	}

	@Override
	public void onRateAvailable(String platformName, String rateName, Rate rate) {
		logger.info("{} is available from {}", rateName, platformName);
		if (rate != null) {
			logger.info("Rate: {}", rate);
		}
	}

	@Override
	public void onRateStatus(String platformName, String rateName, RateStatus rateStatus) {
		logger.info("Status for {} from {} is {}", rateName, platformName, rateStatus);
	}

	@Override
	public void onRateUpdate(String platformName, String rateName, RateDto rateDto) {
		logger.info("{} from {} updated to {}", rateName, platformName, rateDto);
	}

	@Override
	public void subscribe(TickerType tickerType) {
		IPlatformHandler platformHandler = getPlatformHandler(tickerType.getPlatformName());
		if (platformHandler != null) {
			platformHandler.subscribe(tickerType.getPlatformName(), tickerType.getValue());
		}
	}

	@Override
	public void connect(PlatformName platformName) {
		IPlatformHandler platformHandler = getPlatformHandler(platformName.getName());
		if (platformHandler != null) {
			platformHandler.connect(platformName.getName(), "", "");
		}
	}

	@Override
	public void unsubscribe(TickerType tickerType) {
		IPlatformHandler platformHandler = getPlatformHandler(tickerType.getPlatformName());
		if (platformHandler != null) {
			platformHandler.unSubscribe(tickerType.getPlatformName(), tickerType.getValue());
		}
	}

	@Override
	public void disconnect(PlatformName platformName) {
		IPlatformHandler platformHandler = getPlatformHandler(platformName.getName());
		if (platformHandler != null) {
			platformHandler.disConnect(platformName.getName(),"" ,"");
		}
	}

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
