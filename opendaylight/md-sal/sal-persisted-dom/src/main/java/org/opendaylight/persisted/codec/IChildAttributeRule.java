package org.opendaylight.persisted.codec;

public interface IChildAttributeRule {
    public boolean isChildAttribute(AttributeDescriptor ad);
    public boolean isChildAttribute(TypeDescriptor td);
}
