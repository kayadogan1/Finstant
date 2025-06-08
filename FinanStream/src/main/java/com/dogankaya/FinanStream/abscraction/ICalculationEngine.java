package com.dogankaya.FinanStream.abscraction;

import java.math.BigDecimal;
import java.util.Map;

public interface ICalculationEngine {
    String getName();
    void initialize(Map<String, Object> initialBindings);
    BigDecimal evaluate(String expression, Map<String, Object> currentBindings) throws Exception;
    void setVariable(String name, Object value);
}