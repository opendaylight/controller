package org.opendaylight.controller.sal.binding.impl.util;



import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;

import static com.google.common.base.Preconditions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import com.google.common.base.Optional;

public class ClassLoaderUtils {
    
    public static <V> V withClassLoader(ClassLoader cls,Callable<V> function) throws Exception {
        return withClassLoaderAndLock(cls, Optional.<Lock>absent(), function);
    }
    
    public static <V> V withClassLoaderAndLock(ClassLoader cls,Lock lock,Callable<V> function) throws Exception {
        checkNotNull(lock,"Lock should not be null");
        return withClassLoaderAndLock(cls, Optional.of(lock), function);
    }
    
    public static <V> V withClassLoaderAndLock(ClassLoader cls,Optional<Lock> lock,Callable<V> function) throws Exception {
        checkNotNull(cls, "Classloader should not be null");
        checkNotNull(function,"Function should not be null");
        if(lock.isPresent()) {
            lock.get().lock();
        }
        ClassLoader oldCls = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cls);
            V result = function.call();
            return result;
        }  finally {
            Thread.currentThread().setContextClassLoader(oldCls);
            if(lock.isPresent()) {
                lock.get().unlock();
            }
        }
    }

    public static Object construct(Constructor<? extends Object> constructor, ArrayList<Object> objects) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    Object[] initargs = objects.toArray(new Object[]{});
    return constructor.newInstance(initargs);
    }
    
    
    public static Class<?> loadClassWithTCCL(String name) throws ClassNotFoundException {
        if("byte[]".equals(name)) {
            return byte[].class;
        }
        
        return Thread.currentThread().getContextClassLoader().loadClass(name);
    }
}