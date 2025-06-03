package com.dogankaya.FinanStream;

import rate.Rate;
import rate.RateDto;
import rate.RateStatus;
import com.dogankaya.FinanStream.abscraction.ICoordinatorCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Coordinator implements ICoordinatorCallback {
	private final static Logger logger = LoggerFactory.getLogger(Coordinator.class);

	public static void main(String[] args) {
		SpringApplication.run(Coordinator.class, args);
	}

	@Override
	public void onConnect(String platformName, Boolean status) {
		if(status){
			logger.info("Connected to " + platformName);
			return;
		}
		logger.error("Cannot Connected to " + platformName);
	}

	@Override
	public void onDisConnect(String platformName, Boolean status) {
		if(status){
			logger.info("Disconnected from" + platformName + " successfully");
			return;
		}
		logger.error("Cannot Disconnect from " + platformName);
	}

	@Override
	public void onRateAvailable(String platformName, String rateName, Rate rate) {
		logger.info(rateName + " is available from " + platformName);
		logger.info(rate.toString());
	}

	@Override
	public void onRateStatus(String platformName, String rateName, RateStatus rateStatus) {
		logger.info("Status for " + rateName + " from " + platformName + " is " + rateStatus.toString());
	}

	@Override
	public void onRateUpdate(String platformName, String rateName, RateDto rateDto) {
		logger.info(rateName + " from " + platformName + " updated to " + rateDto);
	}
}
