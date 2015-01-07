package org.opendaylight.datasand.codec.observers;

import org.opendaylight.datasand.codec.AttributeDescriptor;
import org.opendaylight.datasand.codec.TypeDescriptor;

public interface IChildAttributeObserver {
    public boolean isChildAttribute(AttributeDescriptor ad);
    public boolean isChildAttribute(TypeDescriptor td);
    public boolean supportAugmentation(AttributeDescriptor ad);
    public boolean supportAugmentation(TypeDescriptor td);
}
