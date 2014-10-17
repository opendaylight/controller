package org.opendaylight.persisted.codec.observers;

import java.lang.reflect.Method;

import org.opendaylight.persisted.codec.AttributeDescriptor;

public interface IMethodFilterObserver {
    public boolean isValidModelMethod(Method m);
    public boolean isValidAttribute(AttributeDescriptor ad);
}
