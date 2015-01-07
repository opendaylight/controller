package org.opendaylight.datasand.codec.observers;

import java.lang.reflect.Method;

import org.opendaylight.datasand.codec.AttributeDescriptor;

public interface IMethodFilterObserver {
    public boolean isValidModelMethod(Method m);
    public boolean isValidAttribute(AttributeDescriptor ad);
}
