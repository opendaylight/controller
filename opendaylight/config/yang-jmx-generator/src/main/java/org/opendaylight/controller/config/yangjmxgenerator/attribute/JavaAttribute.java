/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.attribute;

import org.opendaylight.controller.config.yangjmxgenerator.TypeProviderWrapper;
import org.opendaylight.yangtools.sal.binding.model.api.Type;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

public class JavaAttribute extends AbstractAttribute implements TypedAttribute {

    private final Type type;
    private final String nullableDescription, nullableDefault;

    public JavaAttribute(LeafSchemaNode leaf,
            TypeProviderWrapper typeProviderWrapper) {
        super(leaf);
        this.type = typeProviderWrapper.getType(leaf);
        this.nullableDefault = leaf.getDefault();
        this.nullableDescription = leaf.getDescription();
    }

    public JavaAttribute(LeafListSchemaNode leaf,
            TypeProviderWrapper typeProviderWrapper) {
        super(leaf);
        this.type = typeProviderWrapper.getType(leaf);
        this.nullableDefault = null;
        this.nullableDescription = leaf.getDescription();
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public String getNullableDescription() {
        return nullableDescription;
    }

    @Override
    public String getNullableDefault() {
        return nullableDefault;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        JavaAttribute that = (JavaAttribute) o;

        if (nullableDefault != null ? !nullableDefault
                .equals(that.nullableDefault) : that.nullableDefault != null)
            return false;
        if (nullableDescription != null ? !nullableDescription
                .equals(that.nullableDescription)
                : that.nullableDescription != null)
            return false;
        if (type != null ? !type.equals(that.type) : that.type != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31
                * result
                + (nullableDescription != null ? nullableDescription.hashCode()
                        : 0);
        result = 31 * result
                + (nullableDefault != null ? nullableDefault.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "JavaAttribute{" + getAttributeYangName() + "," + "type=" + type
                + '}';
    }

    @Override
    public OpenType<?> getOpenType() {
        // If is array => arrayType
        if (isArray(getType())) {
            String innerTypeFullyQName = getInnerType(getType());
            SimpleType<?> innerSimpleType = SimpleTypeResolver
                    .getSimpleType(innerTypeFullyQName);
            try {
                ArrayType<Object> arrayType = isPrimitive(innerTypeFullyQName) ? new ArrayType<>(
                        innerSimpleType, true) : new ArrayType<>(1,
                        innerSimpleType);
                return arrayType;
            } catch (OpenDataException e) {
                throw new RuntimeException("Unable to create "
                        + ArrayType.class + " with inner element of type "
                        + innerSimpleType, e);
            }
        }
        // else simple type
        SimpleType<?> simpleType = SimpleTypeResolver.getSimpleType(getType());
        return simpleType;
    }

    // TODO verify
    private boolean isPrimitive(String innerTypeFullyQName) {
        if (innerTypeFullyQName.contains("."))
            return false;

        return true;
    }

    private static String getInnerType(Type type) {
        String fullyQualifiedName = type.getFullyQualifiedName();
        return fullyQualifiedName.substring(0, fullyQualifiedName.length() - 2);
    }

    private static boolean isArray(Type type) {
        return type.getName().endsWith("[]");
    }

}
