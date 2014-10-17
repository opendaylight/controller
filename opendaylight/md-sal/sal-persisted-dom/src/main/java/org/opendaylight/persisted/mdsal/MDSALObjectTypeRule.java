package org.opendaylight.persisted.mdsal;

import org.opendaylight.persisted.codec.AttributeDescriptor;
import org.opendaylight.persisted.codec.observers.ITypeAttributeObserver;

public class MDSALObjectTypeRule implements ITypeAttributeObserver{

    @Override
    public boolean isTypeAttribute(AttributeDescriptor ad) {
        return (ad.getReturnType().getName().indexOf(".rev")!=-1);
    }
}
