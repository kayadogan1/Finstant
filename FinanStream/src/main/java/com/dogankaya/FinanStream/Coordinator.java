package com.dogankaya.FinanStream;

import rate.Rate;
import rate.RateDto;
import rate.RateStatus;
import com.dogankaya.FinanStream.abscraction.ICoordinatorCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Coordinator implements ICoordinatorCallback {
	private static final Logger logger = LogManager.getLogger(Coordinator.class);

	public static void main(String[] args) {
		SpringApplication.run(Coordinator.class, args);
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
}
