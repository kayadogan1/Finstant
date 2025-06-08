package com.dogankaya.FinanStream.engine;

import com.dogankaya.FinanStream.abscraction.ICalculationEngine;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import groovy.transform.CompileStatic;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Implementation of {@link ICalculationEngine} that evaluates mathematical expressions using the Groovy language.
 *
 * <p>This engine uses a {@link GroovyShell} to evaluate expressions dynamically, with variables managed through
 * a {@link Binding}. It supports optional static compilation via the {@code @CompileStatic} annotation
 * to improve performance and type safety.</p>
 *
 * <p>The engine automatically imports {@code java.lang.Math} static methods and {@code java.math.BigDecimal}
 * for convenience in expression evaluation.</p>
 *
 * <p>Evaluation results are returned as {@link BigDecimal}. If the expression result is not numeric,
 * an {@link IllegalArgumentException} is thrown.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 *     GroovyCalculationEngine engine = new GroovyCalculationEngine();
 *     Map&lt;String, Object&gt; initialVars = Map.of("x", 5, "y", 10);
 *     engine.initialize(initialVars);
 *     BigDecimal result = engine.evaluate("x * y + Math.sin(x)", null);
 * </pre>
 */
public class GroovyCalculationEngine implements ICalculationEngine {
    private static final Logger logger = LoggerFactory.getLogger(GroovyCalculationEngine.class);

    private GroovyShell shell;
    private Binding binding;
    private final boolean staticCompile;
    private final String scriptBaseClass;

    /**
     * Default constructor which disables static compilation and does not set a custom script base class.
     */
    public GroovyCalculationEngine() {
        this.staticCompile = false;
        this.scriptBaseClass = null;
    }

    /**
     * Returns the name identifier of this calculation engine implementation.
     *
     * @return a {@link String} representing the engine's name; here it is "groovy".
     */
    @Override
    public String getName() {
        return "groovy";
    }

    /**
     * Initializes the calculation engine with a set of initial variable bindings.
     *
     * <p>This method sets up the Groovy {@link Binding} with the provided variables,
     * configures the Groovy shell including optional static compilation and
     * script base class customization, and adds convenient imports such as {@code java.lang.Math}
     * and {@code java.math.BigDecimal}.</p>
     *
     * @param initialBindings a {@link Map} containing initial variable names and their values
     */
    @Override
    public void initialize(Map<String, Object> initialBindings) {
        this.binding = new Binding();
        initialBindings.forEach(this.binding::setVariable);

        CompilerConfiguration config = new CompilerConfiguration();

        if (scriptBaseClass != null && !scriptBaseClass.isEmpty()) {
            config.setScriptBaseClass(scriptBaseClass);
            logger.info("Groovy engine using custom script base class: {}", scriptBaseClass);
        }

        if (staticCompile) {
            config.addCompilationCustomizers(new ASTTransformationCustomizer(CompileStatic.class));
            logger.info("Groovy engine will use @CompileStatic transformation.");
        }

        ImportCustomizer importCustomizer = new ImportCustomizer();
        importCustomizer.addStaticStars("java.lang.Math");
        importCustomizer.addImports("java.math.BigDecimal");
        config.addCompilationCustomizers(importCustomizer);

        this.shell = new GroovyShell(this.getClass().getClassLoader(), binding, config);
    }

    /**
     * Evaluates the given Groovy expression string using the current variable bindings.
     *
     * <p>If {@code currentBindings} is provided, the variables in it are updated
     * in the engine's binding before evaluation.</p>
     *
     * <p>The result is expected to be a numeric type and is converted to {@link BigDecimal}.
     * If the result is not numeric, this method throws an {@link IllegalArgumentException}.</p>
     *
     * @param expression the Groovy expression to evaluate as a {@link String}
     * @param currentBindings a {@link Map} of variables to update in the engine before evaluation; may be {@code null}
     * @return the evaluated result as a {@link BigDecimal}
     * @throws IllegalArgumentException if the evaluation result is not a number
     */
    @Override
    public BigDecimal evaluate(String expression, Map<String, Object> currentBindings) {
        if (currentBindings != null) {
            currentBindings.forEach(this.binding::setVariable);
        }
        Object result = shell.evaluate(expression);
        if (result instanceof BigDecimal) {
            return (BigDecimal) result;
        } else if (result instanceof Number) {
            return BigDecimal.valueOf(((Number) result).doubleValue());
        }
        throw new IllegalArgumentException("Groovy expression did not return a valid number: " + result);
    }

    /**
     * Sets or updates a variable in the Groovy shell's binding.
     *
     * <p>If the binding is not initialized, this method logs a warning and does nothing.</p>
     *
     * @param name the variable name as a {@link String}
     * @param value the variable value as an {@link Object}
     */
    @Override
    public void setVariable(String name, Object value) {
        if (binding != null) {
            binding.setVariable(name, value);
        } else {
            logger.warn("Binding not initialized for GroovyCalculationEngine. Cannot set variable: {}={}", name, value);
        }
    }
}