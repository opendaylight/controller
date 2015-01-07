package org.opendaylight.datasand.codec.observers;

import org.opendaylight.datasand.codec.TypeDescriptor;

public interface IClassExtractorObserver {
    public Class<?> getObjectClass(Object obj);
    public Class<?> getBuilderClass(TypeDescriptor td);
    public String getBuilderMethod(TypeDescriptor td);
}
