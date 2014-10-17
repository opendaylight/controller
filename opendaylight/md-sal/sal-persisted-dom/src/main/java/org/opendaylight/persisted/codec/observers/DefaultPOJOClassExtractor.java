package org.opendaylight.persisted.codec.observers;

import org.opendaylight.persisted.codec.TypeDescriptor;

public class DefaultPOJOClassExtractor implements IClassExtractorObserver{

    @Override
    public Class<?> getObjectClass(Object obj) {
        return obj.getClass();
    }

    @Override
    public Class<?> getBuilderClass(TypeDescriptor td) {
        return td.getTypeClass();
    }

    @Override
    public String getBuilderMethod(TypeDescriptor td) {
        return null;
    }

}
