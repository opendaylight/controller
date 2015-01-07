package org.opendaylight.datasand.codec.observers;

import org.opendaylight.datasand.codec.TypeDescriptor;

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
