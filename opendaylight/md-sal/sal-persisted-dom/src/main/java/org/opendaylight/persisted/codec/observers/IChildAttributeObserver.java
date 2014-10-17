package org.opendaylight.persisted.codec.observers;

import org.opendaylight.persisted.codec.AttributeDescriptor;
import org.opendaylight.persisted.codec.TypeDescriptor;

public interface IChildAttributeObserver {
    public boolean isChildAttribute(AttributeDescriptor ad);
    public boolean isChildAttribute(TypeDescriptor td);
    public boolean supportAugmentation(AttributeDescriptor ad);
    public boolean supportAugmentation(TypeDescriptor td);
}
