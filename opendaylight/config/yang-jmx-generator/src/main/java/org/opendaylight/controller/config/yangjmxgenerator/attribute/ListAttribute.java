/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.attribute;

import java.util.List;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import org.opendaylight.controller.config.yangjmxgenerator.TypeProviderWrapper;
import org.opendaylight.mdsal.binding.model.api.Type;
import org.opendaylight.mdsal.binding.model.util.Types;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

public class ListAttribute extends AbstractAttribute implements TypedAttribute {

    private final String nullableDescription, nullableDefault;
    private final TypedAttribute innerAttribute;

    public static ListAttribute create(final ListSchemaNode node,
            final TypeProviderWrapper typeProvider, final String packageName) {

        final TOAttribute innerAttribute = TOAttribute.create(node, typeProvider, packageName);

        return new ListAttribute(node, innerAttribute, node.getDescription());
    }

    public static ListAttribute create(final LeafListSchemaNode node,
            final TypeProviderWrapper typeProvider) {

        final JavaAttribute innerAttribute = new JavaAttribute(node, typeProvider);

        return new ListAttribute(node, innerAttribute, node.getDescription());
    }

    ListAttribute(final DataSchemaNode attrNode, final TypedAttribute innerAttribute,
            final String description) {
        super(attrNode);
        this.nullableDescription = description;
        this.innerAttribute = innerAttribute;
        this.nullableDefault = null;
    }

    @Override
    public String getNullableDescription() {
        return this.nullableDescription;
    }

    @Override
    public String getNullableDefault() {
        return this.nullableDefault;
    }

    public AttributeIfc getInnerAttribute() {
        return this.innerAttribute;
    }

    @Override
    public String toString() {
        return "ListAttribute{" + getAttributeYangName() + "," + "to="
                + this.innerAttribute + '}';
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = (31
                * result)
                + (this.nullableDescription != null ? this.nullableDescription.hashCode()
                        : 0);
        result = (31 * result)
                + (this.nullableDefault != null ? this.nullableDefault.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        final ListAttribute that = (ListAttribute) o;

        if (this.nullableDefault != null ? !this.nullableDefault
                .equals(that.nullableDefault) : that.nullableDefault != null) {
            return false;
        }
        if (this.nullableDescription != null ? !this.nullableDescription
                .equals(that.nullableDescription)
                : that.nullableDescription != null) {
            return false;
        }

        return true;
    }


    @Override
    public Type getType() {
        return Types.parameterizedTypeFor(Types.typeForClass(List.class), this.innerAttribute.getType());
    }

    @Override
    public ArrayType<?> getOpenType() {
        final OpenType<?> innerOpenType = this.innerAttribute.getOpenType();
        return constructArrayType(innerOpenType);
    }

    static ArrayType<?> constructArrayType(final OpenType<?> innerOpenType){
        try {
            return new ArrayType<>(1, innerOpenType);
        } catch (final OpenDataException e) {
            throw new RuntimeException("Unable to create " + ArrayType.class
                    + " with inner element of type " + innerOpenType, e);
        }
    }

}
