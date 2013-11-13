package org.opendaylight.controller.sal.binding.impl.util;



import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class ClassLoaderUtils {
    
    public static <V> V withClassLoader(ClassLoader cls,Callable<V> function) throws Exception {
        checkNotNull(cls);
        checkNotNull(function);
        ClassLoader oldCls = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cls);
            V result = function.call();
            Thread.currentThread().setContextClassLoader(oldCls);
            return result;
        } catch (Exception e) {
            Thread.currentThread().setContextClassLoader(oldCls);
            throw new Exception(e);
        }
    }

    public static Object construct(Constructor<? extends Object> constructor, ArrayList<Object> objects) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    Object[] initargs = objects.toArray(new Object[]{});
    return constructor.newInstance(initargs);
    }
    
    
    public static Class<?> loadClassWithTCCL(String name) throws ClassNotFoundException {
        return Thread.currentThread().getContextClassLoader().loadClass(name);
    }
}