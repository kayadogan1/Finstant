package com.dogankaya.FinanStream.services;

import com.dogankaya.FinanStream.helpers.FinanStreamProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import rate.RateDto;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@EnableScheduling
public class CalculatorService {
    private final Logger logger = LogManager.getLogger();
    private final HashOperations<String, String, RateDto> hashOperations;

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final String ratesConfigFilePath;

    private final Map<String, String> formulas = new HashMap<>();
    private final Map<String, List<String>> dependsOn = new HashMap<>();

    private GroovyShell shell;

    public CalculatorService(RedisTemplate<String, Object> redisTemplate,
                             ObjectMapper objectMapper,
                             ResourceLoader resourceLoader,
                             FinanStreamProperties finanStreamProperties) {
        this.hashOperations = redisTemplate.opsForHash();
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
        this.resourceLoader = resourceLoader;
        this.ratesConfigFilePath = finanStreamProperties.getRatesConfigPath();
    }

    @PostConstruct
    public void init() throws Exception {
        logger.info("Initializing CalculatorService");
        loadFormulasFromConfig();

    }

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

    public Map<String, RateDto> loadRawRatesFromRedis() {
        try {
            return hashOperations.entries("raw_rates");
        } catch (Exception e) {
            throw new RuntimeException("Cannot read raw_rates from redis", e);
        }
    }

    public Map<String, RateDto> loadCalculatedRatesFromRedis() {
        try {
            return hashOperations.entries("calculated_rates");
        } catch (Exception e) {
            throw new RuntimeException("Cannot read calculated_rates from redis", e);
        }
    }


    public void resolve(String key, Map<String, RateDto> raw, Map<String, RateDto> calculated, Binding binding) {
        String baseKey = key;
        if(key.endsWith("_ask") || key.endsWith("_bid")){
            baseKey = key.substring(0, key.indexOf("_"));
        }

        List<String> dependencies = dependsOn.get(baseKey);
        if(dependencies == null){
            logger.warn("No dependencies found for key:  {}",baseKey);
            return;
        }
        for(String dependency : dependencies){
            if(raw.containsKey(dependency)){
                RateDto dto = objectMapper.convertValue(raw.get(dependency), RateDto.class);
                binding.setVariable(dependency + "_ask", dto.getAsk());
                binding.setVariable(dependency + "_bid", dto.getBid());
                continue;
            }
            if(calculated.containsKey(dependency)){
                RateDto dto = objectMapper.convertValue(calculated.get(dependency), RateDto.class);
                binding.setVariable(dependency, dto.getAsk());
                binding.setVariable(dependency + "_ask", dto.getAsk());
                binding.setVariable(dependency + "_bid", dto.getBid());
                continue;
            }
            if(formulas.containsKey(dependency)){
                resolve(dependency, raw, calculated, binding);
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
                result = (BigDecimal) shell.evaluate(formula);
            }
            if(formula_ask != null){
                ask = (BigDecimal) shell.evaluate(formula_ask);
            }
            if (formulas_bid != null) {
                bid = (BigDecimal) shell.evaluate(formulas_bid);
            }
        }catch (Exception e){
            logger.error("Ticker {} cannot calculated cause: {}", key, e.getMessage());
            return;
        }

        RateDto dto = calculated.get(key);
        if(dto == null){
            dto = new RateDto();
            calculated.put(baseKey, dto);
            dto.setRateName(baseKey);
            dto.setRateUpdateTime(LocalDateTime.now());
        }
        if(bid != null){
            dto.setBid(bid);
            binding.setVariable(key, bid);
        }
        if(ask != null){
            dto.setAsk(ask);
            binding.setVariable(key, ask);
        }
        if(result != null){
            dto.setAsk(result);
            dto.setBid(result);
            binding.setVariable(key, result);
        }
        calculated.put(key, dto);
        logger.info("Key: {}.bid, Calculated: {}", key, dto.getBid());
        logger.info("Key: {}.ask, Calculated: {}", key, dto.getAsk());
        hashOperations.put("calculated_rates", baseKey, dto);
    }

    public void calculateAffectedRates(RateDto rateDto) {
        Map<String, RateDto> raw = loadRawRatesFromRedis();
        Map<String, RateDto> calculated = loadCalculatedRatesFromRedis();
        Binding binding = new Binding();
        shell = new GroovyShell(binding);
        for(String key : dependsOn.keySet()){
            if (isAffectedRate(key, rateDto, calculated)) {
                resolve(key, raw, calculated, binding);
            }
        }
    }

    private boolean isAffectedRate(String key, RateDto rateDto, Map<String, RateDto> calculated) {
        if(key.equals(rateDto.getRateName())){
            calculated.remove(key);
            return true;
        }
        List<String> dependencies = dependsOn.get(key);
        if(dependencies == null)
            return false;
        for(String dependency : dependencies){
            if(dependency.equals(rateDto.getRateName())){
                calculated.remove(key);
                return true;
            }else{
                List<String> childDependencies = dependsOn.get(dependency);
                if(childDependencies == null)
                    continue;
                for(String childDep : childDependencies){
                    if(isAffectedRate(childDep, rateDto, calculated)){
                        calculated.remove(key);
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
