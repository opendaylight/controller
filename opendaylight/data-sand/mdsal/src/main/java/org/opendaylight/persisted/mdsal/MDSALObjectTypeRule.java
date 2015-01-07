package org.opendaylight.persisted.mdsal;

import org.opendaylight.datasand.codec.AttributeDescriptor;
import org.opendaylight.datasand.codec.observers.ITypeAttributeObserver;

public class MDSALObjectTypeRule implements ITypeAttributeObserver{

    @Override
    public boolean isTypeAttribute(AttributeDescriptor ad) {
        return (ad.getReturnType().getName().indexOf(".rev")!=-1);
    }
}
