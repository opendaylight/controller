package org.opendaylight.datasand.codec.observers;

import java.lang.reflect.Method;

import org.opendaylight.datasand.codec.AttributeDescriptor;

public class DefaultMethodFilterObserver implements IMethodFilterObserver{

    @Override
    public boolean isValidModelMethod(Method m) {
        if(m.getName().equals("getClass"))
            return false;
        if(!m.getName().startsWith("get") && !m.getName().startsWith("is"))
            return false;
        if(m.getParameterTypes().length > 0)
            return false;
        return true;
    }

    @Override
    public boolean isValidAttribute(AttributeDescriptor ad) {
        return true;
    }
}
