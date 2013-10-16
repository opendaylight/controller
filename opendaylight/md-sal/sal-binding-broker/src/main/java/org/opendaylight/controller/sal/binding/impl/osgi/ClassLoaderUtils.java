package org.opendaylight.controller.sal.binding.impl.osgi;



import java.util.concurrent.Callable;
import static com.google.common.base.Preconditions.*;

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
}