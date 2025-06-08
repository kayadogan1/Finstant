package com.dogankaya.FinanStream.abscraction;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Interface representing a generic calculation engine capable of evaluating
 * expressions and managing variable bindings.
 *
 * <p>This interface is designed to be implemented by various types of calculation
 * engines which support expression evaluation with a given set of variables.</p>
 */
public interface ICalculationEngine {

    /**
     * Returns the name of the calculation engine.
     *
     * @return a {@link String} representing the name of the engine.
     */
    String getName();

    /**
     * Initializes the engine with a set of initial variable bindings.
     *
     * @param initialBindings a {@link Map} containing variable names and their initial values.
     */
    void initialize(Map<String, Object> initialBindings);

    /**
     * Evaluates the given expression using the provided variable bindings.
     *
     * @param expression the expression to evaluate as a {@link String}.
     * @param currentBindings a {@link Map} containing the current variable bindings.
     * @return the result of the evaluation as a {@link BigDecimal}.
     * @throws Exception if the expression is invalid or an error occurs during evaluation.
     */
    BigDecimal evaluate(String expression, Map<String, Object> currentBindings) throws Exception;

    /**
     * Sets or updates the value of a specific variable in the engine's context.
     *
     * @param name the name of the variable.
     * @param value the value to assign to the variable.
     */
    void setVariable(String name, Object value);
}
