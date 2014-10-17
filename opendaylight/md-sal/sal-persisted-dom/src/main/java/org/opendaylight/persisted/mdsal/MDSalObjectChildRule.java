package org.opendaylight.persisted.mdsal;

import org.opendaylight.persisted.codec.AttributeDescriptor;
import org.opendaylight.persisted.codec.TypeDescriptor;
import org.opendaylight.persisted.codec.observers.IChildAttributeObserver;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class MDSalObjectChildRule implements IChildAttributeObserver{

    @Override
    public boolean isChildAttribute(AttributeDescriptor ad) {
        return DataObject.class.isAssignableFrom(ad.getReturnType());
    }

    @Override
    public boolean isChildAttribute(TypeDescriptor td) {
        return DataObject.class.isAssignableFrom(td.getTypeClass());
    }

    @Override
    public boolean supportAugmentation(AttributeDescriptor ad) {
        return DataObject.class.isAssignableFrom(ad.getReturnType());
    }

    @Override
    public boolean supportAugmentation(TypeDescriptor td) {
        return DataObject.class.isAssignableFrom(td.getTypeClass());
    }
}
