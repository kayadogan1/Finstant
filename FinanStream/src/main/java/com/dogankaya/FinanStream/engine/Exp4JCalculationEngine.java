package com.dogankaya.FinanStream.engine;

import com.dogankaya.FinanStream.abscraction.ICalculationEngine;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code Exp4JCalculationEngine} is an implementation of {@link ICalculationEngine} that uses the
 * exp4j library to compile and evaluate mathematical expressions dynamically.
 *
 * <p>This class caches compiled expressions for reuse and manages variables in a thread-safe manner
 * using a ConcurrentHashMap. Variables in expressions are identified via regex and registered
 * with the exp4j {@link ExpressionBuilder}.</p>
 *
 * <p>The engine supports setting variables, initializing with a set of variable bindings,
 * and evaluating expressions with optional per-evaluation variable overrides.</p>
 */
public class Exp4JCalculationEngine implements ICalculationEngine {
    private static final Logger logger = LoggerFactory.getLogger(Exp4JCalculationEngine.class);

    private final Map<String, Expression> compiledExpressions = new ConcurrentHashMap<>();
    private final Set<String> knownVariables = new HashSet<>();
    private Map<String, Object> currentExp4jContext;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    /**
     * Returns the name of this calculation engine.
     *
     * @return the string "exp4j"
     */
    @Override
    public String getName() {
        return "exp4j";
    }

    /**
     * Initializes the calculation engine with a set of initial variable bindings.
     *
     * @param initialBindings a map of variable names to their initial values
     */
    @Override
    public void initialize(Map<String, Object> initialBindings) {
        this.currentExp4jContext = new ConcurrentHashMap<>(initialBindings);
        this.knownVariables.addAll(initialBindings.keySet());
        logger.info("Exp4j engine initialized successfully with {} initial bindings.", initialBindings.size());
    }

    /**
     * Evaluates a mathematical expression string using the current context combined
     * with any additional variable bindings provided for this evaluation.
     *
     * <p>If the expression has not been compiled before, it is compiled and cached.
     * Variables are extracted via regex and registered with the expression builder.</p>
     *
     * <p>Variables present in the expression are assigned values from the context
     * if they are numbers; unsupported types or missing variables will generate warnings.</p>
     *
     * @param expressionString the expression to evaluate
     * @param currentBindings optional map of variable bindings specific to this evaluation, can be null
     * @return the evaluation result as a {@link BigDecimal}
     * @throws RuntimeException if the expression compilation or evaluation fails
     */
    @Override
    public BigDecimal evaluate(String expressionString, Map<String, Object> currentBindings) {
        if (currentBindings != null) {
            this.currentExp4jContext.putAll(currentBindings);
            this.knownVariables.addAll(currentBindings.keySet());
        }

        Expression compiledExp = compiledExpressions.computeIfAbsent(expressionString, key -> {
            try {
                Set<String> variablesInExpression = new HashSet<>();
                Matcher matcher = VARIABLE_PATTERN.matcher(key);
                while (matcher.find()) {
                    variablesInExpression.add(matcher.group());
                }
                Set<String> allPotentialVariables = new HashSet<>(knownVariables);
                allPotentialVariables.addAll(variablesInExpression);


                ExpressionBuilder builder = new ExpressionBuilder(key);
                builder.variables(allPotentialVariables);


                return builder.build();
            } catch (Exception e) {
                logger.error("Failed to compile Exp4j expression: {}", key, e);
                throw new RuntimeException("Failed to compile expression", e);
            }
        });

        for (String varName : compiledExp.getVariableNames()) {
            Object value = currentExp4jContext.get(varName);
            if (value instanceof Number) {
                compiledExp.setVariable(varName, ((Number) value).doubleValue());
            } else {
                logger.warn("Variable '{}' has unsupported type or is null in Exp4j context. Value: {}", varName, value);
            }
        }

        double result = compiledExp.evaluate();
        return BigDecimal.valueOf(result);
    }

    /**
     * Sets or updates the value of a variable in the current context.
     *
     * @param name  the variable name
     * @param value the variable value
     */
    @Override
    public void setVariable(String name, Object value) {
        if (currentExp4jContext != null) {
            currentExp4jContext.put(name, value);
            this.knownVariables.add(name);
        } else {
            logger.warn("Exp4j context not initialized. Cannot set variable: {}={}", name, value);
        }
    }
}