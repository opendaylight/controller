package org.opendaylight.persisted.codec.observers;

import org.opendaylight.persisted.codec.TypeDescriptor;

public interface IClassExtractorObserver {
    public Class<?> getObjectClass(Object obj);
    public Class<?> getBuilderClass(TypeDescriptor td);
    public String getBuilderMethod(TypeDescriptor td);
}
