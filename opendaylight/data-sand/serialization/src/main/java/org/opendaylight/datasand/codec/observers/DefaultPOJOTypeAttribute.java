package org.opendaylight.datasand.codec.observers;

import org.opendaylight.datasand.codec.AttributeDescriptor;

public class DefaultPOJOTypeAttribute implements ITypeAttributeObserver{
    @Override
    public boolean isTypeAttribute(AttributeDescriptor ad) {
        if(ad.getReturnType().isPrimitive()) return false;
        if(ad.getReturnType().getName().startsWith("java.")) return false;
        return true;
    }
}
