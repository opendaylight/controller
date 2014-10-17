package org.opendaylight.persisted.mdsal;

import org.opendaylight.persisted.codec.AttributeDescriptor;
import org.opendaylight.persisted.codec.IChildAttributeRule;
import org.opendaylight.persisted.codec.TypeDescriptor;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class MDSalObjectChildRule implements IChildAttributeRule{

    @Override
    public boolean isChildAttribute(AttributeDescriptor ad) {
        return DataObject.class.isAssignableFrom(ad.getReturnType());
    }

    @Override
    public boolean isChildAttribute(TypeDescriptor td) {
        return DataObject.class.isAssignableFrom(td.getTypeClass());
    }
}
