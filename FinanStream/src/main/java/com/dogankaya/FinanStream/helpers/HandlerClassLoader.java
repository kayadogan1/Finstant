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

public class HandlerClassLoader {
    private static final Logger logger = LogManager.getLogger(HandlerClassLoader.class);
    private static ICoordinatorCallback coordinatorCallback;
    private static FinanStreamProperties finanStreamProperties;
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
