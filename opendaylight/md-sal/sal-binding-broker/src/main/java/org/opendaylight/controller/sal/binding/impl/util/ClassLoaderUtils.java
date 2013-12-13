package org.opendaylight.controller.sal.binding.impl.util;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;

import static com.google.common.base.Preconditions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;

public final class ClassLoaderUtils {

    private ClassLoaderUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static <V> V withClassLoader(ClassLoader cls, Callable<V> function) throws Exception {
        return withClassLoaderAndLock(cls, Optional.<Lock> absent(), function);
    }

    public static <V> V withClassLoaderAndLock(ClassLoader cls, Lock lock, Callable<V> function) throws Exception {
        checkNotNull(lock, "Lock should not be null");
        return withClassLoaderAndLock(cls, Optional.of(lock), function);
    }

    public static <V> V withClassLoaderAndLock(ClassLoader cls, Optional<Lock> lock, Callable<V> function)
            throws Exception {
        checkNotNull(cls, "Classloader should not be null");
        checkNotNull(function, "Function should not be null");
        if (lock.isPresent()) {
            lock.get().lock();
        }
        ClassLoader oldCls = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cls);
            return function.call();
        } finally {
            Thread.currentThread().setContextClassLoader(oldCls);
            if (lock.isPresent()) {
                lock.get().unlock();
            }
        }
    }

    public static Object construct(Constructor<? extends Object> constructor, List<Object> objects)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Object[] initargs = objects.toArray(new Object[] {});
        return constructor.newInstance(initargs);
    }

    public static Class<?> loadClassWithTCCL(String name) throws ClassNotFoundException {
        if ("byte[]".equals(name)) {
            return byte[].class;
        } else if("char[]".equals(name)) {
            return char[].class;
        }
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            String[] components = name.split("\\.");
            String potentialOuter;
            int length = components.length;
            if (length > 2 && (potentialOuter = components[length - 2]) != null && Character.isUpperCase(potentialOuter.charAt(0))) {
                
                    String outerName = Joiner.on(".").join(Arrays.asList(components).subList(0, length - 1));
                    String innerName = outerName + "$" + components[length-1];
                    return Thread.currentThread().getContextClassLoader().loadClass(innerName);
            } else {
                throw e;
            }
        }
    }

    public static Class<?> tryToLoadClassWithTCCL(String fullyQualifiedName) {
        try {
            return loadClassWithTCCL(fullyQualifiedName);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}