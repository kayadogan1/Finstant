package com.dogankaya.FinanStream.helpers;

import com.dogankaya.FinanStream.abscraction.ICoordinatorCallback;
import com.dogankaya.FinanStream.abscraction.IPlatformHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
/**
 * The {@code HandlerClassLoader} class is responsible for dynamically loading and instantiating platform handlers
 * based on class names provided in configuration.
 * <p>
 * It uses reflection to create instances of these handler classes, automatically resolving and injecting
 * dependencies if needed.
 * <p>
 * This class also ensures that dependencies like {@link ICoordinatorCallback} and {@link FinanStreamProperties}
 * are injected into handler constructors as needed.
 */
public class HandlerClassLoader {
    private static final Logger logger = LogManager.getLogger(HandlerClassLoader.class);
    private static ICoordinatorCallback coordinatorCallback;
    private static FinanStreamProperties finanStreamProperties;
    /**
     * Loads and returns a list of {@code Class} objects for the given list of handler class names.
     *
     * @param handlerClassNames the fully qualified class names of the handler classes.
     * @return a list of {@code Class} objects corresponding to the provided class names.
     */
    public static List<Class<?>> getHandlerClasses(List<String> handlerClassNames) {
        List<Class<?>> handlerClasses = new ArrayList<>();
        for (String className : handlerClassNames) {
            try {
                Class<?> clazz = Class.forName(className);
                handlerClasses.add(clazz);
            } catch (ClassNotFoundException e) {
                logger.error(e.getMessage());
            }
        }
        return handlerClasses;
    }
    /**
     * Creates and returns a list of {@link IPlatformHandler} instances for the given handler class names.
     * <p>
     * The dependencies {@code ICoordinatorCallback} and {@code FinanStreamProperties} are automatically injected
     * into handler constructors if required.
     *
     * @param handlerClassNames the list of handler class names.
     * @param callback          the coordinator callback to inject.
     * @param properties        the platform properties to inject.
     * @return a list of instantiated {@link IPlatformHandler} objects.
     */
    public static List<IPlatformHandler> getHandlerInstances(List<String> handlerClassNames, ICoordinatorCallback callback, FinanStreamProperties properties){
        List<Class<?>> handlerClasses = getHandlerClasses(handlerClassNames);
        coordinatorCallback = callback;
        finanStreamProperties = properties;
        List<IPlatformHandler> instances = new ArrayList<>();

        for (Class<?> handlerClass : handlerClasses) {
            try{
                IPlatformHandler instance = (IPlatformHandler)createInstanceRecursively(handlerClass);
                instances.add(instance);
            } catch (RuntimeException | NoSuchMethodException | InvocationTargetException | InstantiationException |
                     IllegalAccessException e) {
                logger.error(e.getMessage());
            }
        }

        return instances;
    }
    /**
     * Recursively creates an instance of the given class, automatically instantiating and injecting
     * constructor parameters as needed.
     * <p>
     * If the class is an interface or abstract, this method throws an {@link InstantiationException}.
     * <p>
     * Supports automatic injection for {@link ICoordinatorCallback} and {@link FinanStreamProperties}.
     *
     * @param clazz the class to instantiate.
     * @return the created instance.
     * @throws InstantiationException    if the class cannot be instantiated.
     * @throws IllegalAccessException    if constructor access is denied.
     * @throws InvocationTargetException if an exception occurs during constructor invocation.
     * @throws NoSuchMethodException     if no suitable constructor is found.
     */
    private static Object createInstanceRecursively(Class<?> clazz)
            throws InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException {

        if (clazz.isInterface()) {
            if (clazz == ICoordinatorCallback.class) {
                return coordinatorCallback;
            }
            throw new InstantiationException("Cannot instantiate interface: " + clazz.getName());
        }

        if(clazz == FinanStreamProperties.class){
            return finanStreamProperties;
        }

        if (Modifier.isAbstract(clazz.getModifiers())) {
            throw new InstantiationException("Cannot instantiate abstract class: " + clazz.getName());
        }

        try {
            Constructor<?> defaultConstructor = clazz.getDeclaredConstructor();
            defaultConstructor.setAccessible(true);
            return defaultConstructor.newInstance();
        } catch (NoSuchMethodException e) {
            Constructor<?>[] constructors = clazz.getDeclaredConstructors();

            if (constructors.length == 0) {
                throw new NoSuchMethodException("No constructors found for class: " + clazz.getName());
            }

            Constructor<?> selectedConstructor = constructors[0];
            for (Constructor<?> c : constructors) {
                if (c.getParameterCount() < selectedConstructor.getParameterCount()) {
                    selectedConstructor = c;
                }
            }

            List<Object> parameterInstances = new ArrayList<>();
            for (Class<?> paramType : selectedConstructor.getParameterTypes()) {
                Object paramInstance = createInstanceRecursively(paramType);
                parameterInstances.add(paramInstance);
            }

            selectedConstructor.setAccessible(true);
            return selectedConstructor.newInstance(parameterInstances.toArray());
        }
    }
}
