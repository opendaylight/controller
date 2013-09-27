/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.attribute;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;

import org.opendaylight.controller.config.yangjmxgenerator.TypeProviderWrapper;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

public class ListAttribute extends AbstractAttribute {

    private final String nullableDescription, nullableDefault;
    private final AttributeIfc innerAttribute;

    public static ListAttribute create(ListSchemaNode node,
            TypeProviderWrapper typeProvider) {

        AttributeIfc innerAttribute = TOAttribute.create(node, typeProvider);

        return new ListAttribute(node, innerAttribute, node.getDescription());
    }

    public static ListAttribute create(LeafListSchemaNode node,
            TypeProviderWrapper typeProvider) {

        AttributeIfc innerAttribute = new JavaAttribute(node, typeProvider);

        return new ListAttribute(node, innerAttribute, node.getDescription());
    }

    ListAttribute(DataSchemaNode attrNode, AttributeIfc innerAttribute,
            String description) {
        super(attrNode);
        this.nullableDescription = description;
        this.innerAttribute = innerAttribute;
        this.nullableDefault = null;
    }

    @Override
    public String getNullableDescription() {
        return nullableDescription;
    }

    @Override
    public String getNullableDefault() {
        return nullableDefault;
    }

    public AttributeIfc getInnerAttribute() {
        return innerAttribute;
    }

    @Override
    public String toString() {
        return "ListAttribute{" + getAttributeYangName() + "," + "to="
                + innerAttribute + '}';
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31
                * result
                + (nullableDescription != null ? nullableDescription.hashCode()
                        : 0);
        result = 31 * result
                + (nullableDefault != null ? nullableDefault.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        ListAttribute that = (ListAttribute) o;

        if (nullableDefault != null ? !nullableDefault
                .equals(that.nullableDefault) : that.nullableDefault != null)
            return false;
        if (nullableDescription != null ? !nullableDescription
                .equals(that.nullableDescription)
                : that.nullableDescription != null)
            return false;

        return true;
    }

    @Override
    public ArrayType<?> getOpenType() {
        OpenType<?> inerOpenType = innerAttribute.getOpenType();
        try {
            return new ArrayType<>(1, inerOpenType);
        } catch (OpenDataException e) {
            throw new RuntimeException("Unable to create " + ArrayType.class
                    + " with inner element of type " + inerOpenType, e);
        }
    }

}
