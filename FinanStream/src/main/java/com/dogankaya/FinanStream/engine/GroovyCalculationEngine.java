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

public class GroovyCalculationEngine implements ICalculationEngine {
    private static final Logger logger = LoggerFactory.getLogger(GroovyCalculationEngine.class);

    private GroovyShell shell;
    private Binding binding;
    private final boolean staticCompile;
    private final String scriptBaseClass;

    public GroovyCalculationEngine() {
        this.staticCompile = false;
        this.scriptBaseClass = null;
    }

    @Override
    public String getName() {
        return "groovy";
    }

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

    @Override
    public void setVariable(String name, Object value) {
        if (binding != null) {
            binding.setVariable(name, value);
        } else {
            logger.warn("Binding not initialized for GroovyCalculationEngine. Cannot set variable: {}={}", name, value);
        }
    }
}