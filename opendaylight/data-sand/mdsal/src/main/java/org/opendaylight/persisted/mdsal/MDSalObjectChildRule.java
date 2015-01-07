package org.opendaylight.persisted.mdsal;

import org.opendaylight.datasand.codec.AttributeDescriptor;
import org.opendaylight.datasand.codec.TypeDescriptor;
import org.opendaylight.datasand.codec.observers.IChildAttributeObserver;
import org.opendaylight.yangtools.yang.binding.DataObject;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
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
