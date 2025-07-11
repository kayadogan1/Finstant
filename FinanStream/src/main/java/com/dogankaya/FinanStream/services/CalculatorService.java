package com.dogankaya.FinanStream.services;

import com.dogankaya.FinanStream.abscraction.ICalculationEngine;
import com.dogankaya.FinanStream.engine.Exp4JCalculationEngine;
import com.dogankaya.FinanStream.engine.GroovyCalculationEngine;
import com.dogankaya.FinanStream.helpers.FinanStreamProperties;
import com.dogankaya.FinanStream.kafka.KafkaProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import rate.RateDto;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for calculating financial rates based on formulas and dependencies.
 *
 * <p>This service loads formulas, initializes the calculation engine, reads raw and calculated rates
 * from Redis, calculates dependent rates, and publishes updated rates to Kafka.</p>
 */
@Service
public class CalculatorService {
    private final Logger logger = LogManager.getLogger();
    private final HashOperations<String, String, RateDto> hashOperations;
    private final KafkaProducer kafkaProducer;

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final String ratesConfigFilePath;

    @Value("${finanstream.engine.type:groovy}")
    private String engineType;
    private ICalculationEngine calculationEngine;

    private final Map<String, String> formulas = new HashMap<>();
    private final Map<String, List<String>> dependsOn = new HashMap<>();

    /**
     * Constructor for CalculatorService.
     *
     * @param redisTemplate         RedisTemplate used to access Redis storage.
     * @param kafkaProducer         KafkaProducer used to send calculated rates.
     * @param objectMapper          ObjectMapper for JSON serialization/deserialization.
     * @param resourceLoader        ResourceLoader for loading configuration files.
     * @param finanStreamProperties Properties containing configuration such as rates config path.
     */
    public CalculatorService(RedisTemplate<String, Object> redisTemplate, KafkaProducer kafkaProducer,
                             ObjectMapper objectMapper,
                             ResourceLoader resourceLoader,
                             FinanStreamProperties finanStreamProperties) {
        this.hashOperations = redisTemplate.opsForHash();
        this.kafkaProducer = kafkaProducer;
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
        this.resourceLoader = resourceLoader;
        this.ratesConfigFilePath = finanStreamProperties.getRatesConfigPath();
    }

    /**
     * Initializes the service after construction.
     *
     * <p>Loads formulas from configuration and initializes the calculation engine based on the configured type.</p>
     *
     * @throws Exception if loading formulas or initializing the engine fails.
     */
    @PostConstruct
    public void init() throws Exception {
        logger.info("Initializing CalculatorService");
        loadFormulasFromConfig();
        initializeCalculationEngine();
    }

    /**
     * Loads rate formulas and their dependencies from the configuration file.
     *
     * @throws Exception if there is an error reading the configuration resource.
     */
    private void loadFormulasFromConfig() throws Exception {
        Resource resource = resourceLoader.getResource(ratesConfigFilePath);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            List<String> lines = reader.lines().toList();

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                if (line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    if (key.endsWith(".depends.on")) {
                        String formulaKey = key.substring(0, key.indexOf(".depends.on"));
                        List<String> dependencies = Arrays.stream(value.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .collect(Collectors.toList());
                        dependsOn.put(formulaKey, dependencies);
                    } else {
                        key = key.replace(".", "_");
                        value = value.replace(".", "_");
                        formulas.put(key, value);
                    }
                }
            }
        }
    }

    /**
     * Initializes the calculation engine based on the configured engine type.
     *
     * <p>Supports "groovy" and "exp4j" engine types. Throws exception for unsupported types.</p>
     */
    private void initializeCalculationEngine() {
        switch (engineType) {
            case "groovy":
                this.calculationEngine = new GroovyCalculationEngine();
                break;
            case "exp4j":
                this.calculationEngine = new Exp4JCalculationEngine();
                break;
            default:
                throw new IllegalArgumentException("Unsupported calculation engine type: " + engineType);
        }
        logger.info("Using calculation engine: {}", this.calculationEngine.getName());
    }

    /**
     * Loads raw rate data from Redis.
     *
     * @return map of rate names to their {@link RateDto} raw values.
     * @throws RuntimeException if reading from Redis fails.
     */
    private Map<String, RateDto> loadRawRatesFromRedis() {
        try {
            return hashOperations.entries("raw_rates");
        } catch (Exception e) {
            throw new RuntimeException("Cannot read raw_rates from redis", e);
        }
    }

    /**
     * Loads calculated rate data from Redis.
     *
     * @return map of rate names to their {@link RateDto} calculated values.
     * @throws RuntimeException if reading from Redis fails.
     */
    private Map<String, RateDto> loadCalculatedRatesFromRedis() {
        try {
            return hashOperations.entries("calculated_rates");
        } catch (Exception e) {
            throw new RuntimeException("Cannot read calculated_rates from redis", e);
        }
    }

    /**
     * Adds rate ask and bid values to the current bindings map for formula evaluation.
     *
     * @param currentBindings the current bindings map where variables are stored.
     * @param key             the rate name key.
     * @param dto             the {@link RateDto} containing ask and bid values.
     */
    private void addBinding(Map<String, Object> currentBindings, String key, RateDto dto){
        currentBindings.put(key, dto.getAsk());
        currentBindings.put(key + "_ask", dto.getAsk());
        currentBindings.put(key + "_bid", dto.getBid());
    }

    /**
     * Recursively resolves and calculates a rate formula and its dependencies.
     *
     * <p>Updates the calculated rates map and sends updates to Kafka and Redis.</p>
     *
     * @param key        the formula key to resolve.
     * @param raw        the raw rates map.
     * @param calculated the calculated rates map to update.
     */
    private void resolve(String key, Map<String, RateDto> raw, Map<String, RateDto> calculated) {
        String baseKey = key;
        if(key.endsWith("_ask") || key.endsWith("_bid")){
            baseKey = key.substring(0, key.indexOf("_"));
        }

        List<String> dependencies = dependsOn.get(baseKey);
        if(dependencies == null){
            logger.warn("No dependencies found for key:  {}",baseKey);
            return;
        }

        Map<String, Object> currentBindings = new HashMap<>();

        for(String dependency : dependencies){
            if(raw.containsKey(dependency)){
                RateDto dto = objectMapper.convertValue(raw.get(dependency), RateDto.class);
                addBinding(currentBindings, dependency, dto);
                continue;
            }
            if(calculated.containsKey(dependency)){
                RateDto dto = objectMapper.convertValue(calculated.get(dependency), RateDto.class);
                addBinding(currentBindings, dependency, dto);
                continue;
            }
            if(formulas.containsKey(dependency)){
                resolve(dependency, raw, calculated);
            } else {
                logger.warn("Dependency '{}' for formula '{}' is not a raw rate, calculated rate, or a defined formula.", dependency, key);
            }
        }
        String formula = formulas.get(key);
        String formula_ask = formulas.get(key + "_ask");
        String formulas_bid = formulas.get(key + "_bid");
        BigDecimal result = null;
        BigDecimal ask = null;
        BigDecimal bid = null;

        try{
            if(formula != null){
                result = calculationEngine.evaluate(formula, currentBindings);
            }
            if(formula_ask != null){
                ask = calculationEngine.evaluate(formula_ask, currentBindings);
            }
            if (formulas_bid != null) {
                bid = calculationEngine.evaluate(formulas_bid, currentBindings);
            }
        }catch (Exception e){
            logger.warn("Ticker {} cannot be calculated cause: {}", key, e.getMessage());
            return;
        }

        RateDto dto = calculated.get(baseKey);
        if(dto == null){
            dto = new RateDto();
            dto.setRateName(baseKey);
            dto.setRateUpdateTime(LocalDateTime.now());
        }
        if(bid != null){
            dto.setBid(bid);
            calculationEngine.setVariable(key, bid);
        }
        if(ask != null){
            dto.setAsk(ask);
            calculationEngine.setVariable(key, ask);
        }
        if(result != null){
            dto.setAsk(result);
            dto.setBid(result);
            calculationEngine.setVariable(key, result);
        }
        calculated.put(baseKey, dto);
        kafkaProducer.sendRate("rate-topic", dto);
        logger.info("Key: {}.bid, Calculated: {}", baseKey, dto.getBid());
        logger.info("Key: {}.ask, Calculated: {}", baseKey, dto.getAsk());
        hashOperations.put("calculated_rates", baseKey, dto);
    }

    /**
     * Calculates all rates affected by a given updated {@link RateDto}.
     *
     * <p>Loads raw and calculated rates from Redis, determines which formulas depend on the updated rate,
     * evaluates these formulas, and updates the calculated rates.</p>
     *
     * @param rateDto the updated raw rate that may affect calculated rates.
     */
    public void calculateAffectedRates(RateDto rateDto) {
        Map<String, RateDto> raw = loadRawRatesFromRedis();
        Map<String, RateDto> calculated = loadCalculatedRatesFromRedis();
        List<String> toCalculateList = new ArrayList<>();

        Map<String, Object> initialBindings = new HashMap<>();
        for(Map.Entry<String, RateDto> r: raw.entrySet()){
            RateDto dto = objectMapper.convertValue(r.getValue(), RateDto.class);
            initialBindings.put(dto.getRateName() + "_ask", dto.getAsk());
            initialBindings.put(dto.getRateName() + "_bid", dto.getBid());
        }
        for(Map.Entry<String, RateDto> r: calculated.entrySet()){
            RateDto dto = objectMapper.convertValue(r.getValue(), RateDto.class);
            if(Objects.equals(dto.getAsk(), dto.getBid())){
                initialBindings.put(dto.getRateName(), dto.getAsk());
                continue;
            }
            initialBindings.put(dto.getRateName() + "_ask", dto.getAsk());
            initialBindings.put(dto.getRateName() + "_bid", dto.getBid());

        }

        for(String key : dependsOn.keySet()){
            if (isAffectedRate(key, rateDto, calculated, initialBindings)) {
                toCalculateList.add(key);
            }
        }

        this.calculationEngine.initialize(initialBindings);

        toCalculateList.forEach(key -> resolve(key, raw, calculated));
    }

    /**
     * Determines whether the given rate key is affected by the specified RateDto,
     * either directly or indirectly through dependencies.
     * <p>
     * This method recursively checks if the provided key or any of its dependencies
     * match the rate name from the given RateDto. If a match is found, the related
     * entries are removed from the provided calculated rates and initial bindings maps.
     * </p>
     *
     * @param key             the rate key to check for dependency on the given RateDto
     * @param rateDto         the RateDto object containing the rate name to check against dependencies
     * @param calculated      the map of currently calculated rates that may be modified
     * @param initialBindings the map of initial variable bindings that may be modified
     * @return true if the key is affected by the rateDto (directly or indirectly), otherwise false
     */
    private boolean isAffectedRate(String key, RateDto rateDto, Map<String, RateDto> calculated, Map<String, Object> initialBindings) {
        if(key.equals(rateDto.getRateName())){
            calculated.remove(key);
            initialBindings.remove(key);
            return true;
        }
        List<String> dependencies = dependsOn.get(key);
        if(dependencies == null)
            return false;
        for(String dependency : dependencies){
            if(dependency.equals(rateDto.getRateName())){
                calculated.remove(key);
                initialBindings.remove(key);
                initialBindings.remove(key + "_ask");
                initialBindings.remove(key + "_bid");
                return true;
            }else{
                List<String> childDependencies = dependsOn.get(dependency);
                if(childDependencies == null)
                    continue;
                for(String childDep : childDependencies){
                    if(isAffectedRate(childDep, rateDto, calculated, initialBindings)){
                        calculated.remove(key);
                        calculated.remove(dependency);
                        initialBindings.remove(key);
                        initialBindings.remove(key + "_ask");
                        initialBindings.remove(key + "_bid");
                        initialBindings.remove(dependency);
                        initialBindings.remove(dependency + "_ask");
                        initialBindings.remove(dependency + "_bid");
                        return true;
                    }
                }
            }
        }
        return false;
    }
}