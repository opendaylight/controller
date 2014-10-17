package org.opendaylight.persisted.mdsal;

import java.lang.reflect.Method;

import org.opendaylight.persisted.codec.AttributeDescriptor;
import org.opendaylight.persisted.codec.observers.IMethodFilterObserver;

public class MDSALMethodFilter implements IMethodFilterObserver{

    @Override
    public boolean isValidModelMethod(Method m) {
        if (m.getName().equals("getImplementedInterface"))
            return false;
        return true;
    }

    @Override
    public boolean isValidAttribute(AttributeDescriptor ad) {
        if(ad.getReturnType().getName().equals("org.opendaylight.yangtools.yang.binding.Identifier"))
            return false;
         return true;
    }

}
