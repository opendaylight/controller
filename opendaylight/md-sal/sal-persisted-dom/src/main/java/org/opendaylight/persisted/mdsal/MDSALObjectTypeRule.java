package org.opendaylight.persisted.mdsal;

import org.opendaylight.persisted.codec.AttributeDescriptor;
import org.opendaylight.persisted.codec.ITypeAttributeRule;

public class MDSALObjectTypeRule implements ITypeAttributeRule{

    @Override
    public boolean isTypeAttribute(AttributeDescriptor ad) {
        return (ad.getReturnType().getName().indexOf(".rev")!=-1);
    }
}
