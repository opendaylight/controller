package org.opendaylight.datasand.store.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class JDBCProxy implements InvocationHandler {

    private Object myObject = null;
    private Class<?> myObjectClass = null;

    public JDBCProxy(Object obj) {
        this.myObject = obj;
        this.myObjectClass = this.myObject.getClass();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        System.err.println("Class " + this.myObjectClass.getSimpleName()
                + " Method " + method.getName());
        return method.invoke(this.myObject, args);
    }

}
