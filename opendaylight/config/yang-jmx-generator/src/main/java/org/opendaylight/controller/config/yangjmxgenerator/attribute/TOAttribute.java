/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.attribute;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import org.opendaylight.controller.config.yangjmxgenerator.TypeProviderWrapper;
import org.opendaylight.mdsal.binding.model.api.Type;
import org.opendaylight.mdsal.binding.model.util.ReferencedTypeImpl;
import org.opendaylight.yangtools.yang.model.api.AugmentationTarget;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

public class TOAttribute extends AbstractAttribute implements TypedAttribute {

    private final String nullableDescription, nullableDefault;
    private final Map<String, AttributeIfc> yangNameToAttributeMap;
    private final Map<String, String> attributeNameMap;
    private final String packageName;

    private static final Set<Class<? extends DataSchemaNode>> ALLOWED_CHILDREN = Sets
            .newHashSet();
    static {
        ALLOWED_CHILDREN.add(LeafListSchemaNode.class);
        ALLOWED_CHILDREN.add(ListSchemaNode.class);
        ALLOWED_CHILDREN.add(LeafSchemaNode.class);
        ALLOWED_CHILDREN.add(ContainerSchemaNode.class);
    }

    public static <T extends DataNodeContainer & AugmentationTarget & DataSchemaNode> TOAttribute create(
            final T containerSchemaNode, final TypeProviderWrapper typeProviderWrapper, final String packageName) {
        // Transfer Object: get the leaves
        final Map<String, AttributeIfc> map = new HashMap<>();
        final Map<String, String> attributeNameMap = new HashMap<>();
        for (final DataSchemaNode dataSchemaNode : containerSchemaNode
                .getChildNodes()) {
            try {
                final String yangName = dataSchemaNode.getQName().getLocalName();
                map.put(yangName,
                        createInnerAttribute(dataSchemaNode,
                                typeProviderWrapper, packageName));
            } catch (final IllegalArgumentException e) {
                throw new IllegalStateException("Unable to create TO", e);
            }
        }
        return new TOAttribute(containerSchemaNode, map, attributeNameMap,
                containerSchemaNode.getDescription(), packageName);
    }

    private static AttributeIfc createInnerAttribute(
            final DataSchemaNode dataSchemaNode,
            final TypeProviderWrapper typeProviderWrapper, final String packageName) {
        final Class<? extends DataSchemaNode> type = isAllowedType(dataSchemaNode);

        if (type.equals(LeafSchemaNode.class)) {
            return new JavaAttribute((LeafSchemaNode) dataSchemaNode,
                    typeProviderWrapper);
        } else if (type.equals(ListSchemaNode.class)) {
            return ListAttribute.create((ListSchemaNode) dataSchemaNode,
                    typeProviderWrapper, packageName);
        } else if (type.equals(LeafListSchemaNode.class)) {
            return ListAttribute.create((LeafListSchemaNode) dataSchemaNode,
                    typeProviderWrapper);
        } else if (type.equals(ContainerSchemaNode.class)) {
            return TOAttribute.create((ContainerSchemaNode) dataSchemaNode,
                    typeProviderWrapper, packageName);
        }

        throw new IllegalStateException("This should never happen");
    }

    private static Class<? extends DataSchemaNode> isAllowedType(
            final DataSchemaNode dataSchemaNode) {
        for (final Class<? extends DataSchemaNode> allowedType : ALLOWED_CHILDREN) {
            if (allowedType.isAssignableFrom(dataSchemaNode.getClass()) == true) {
                return allowedType;
            }
        }
        throw new IllegalArgumentException("Illegal child node for TO: "
                + dataSchemaNode.getClass() + " allowed node types: "
                + ALLOWED_CHILDREN);
    }

    private TOAttribute(final DataSchemaNode attrNode,
            final Map<String, AttributeIfc> transferObject,
            final Map<String, String> attributeNameMap, final String nullableDescription, final String packageName) {
        super(attrNode);
        this.yangNameToAttributeMap = transferObject;
        this.attributeNameMap = attributeNameMap;
        this.nullableDescription = nullableDescription;
        this.nullableDefault = null;
        this.packageName = packageName;
    }

    public Map<String, String> getAttributeNameMap() {
        return this.attributeNameMap;
    }

    public Map<String, AttributeIfc> getCapitalizedPropertiesToTypesMap() {
        final Map<String, AttributeIfc> capitalizedPropertiesToTypesMap = Maps
                .newHashMap();
        for (final Entry<String, AttributeIfc> entry : this.yangNameToAttributeMap
                .entrySet()) {

            capitalizedPropertiesToTypesMap.put(
                    TypeProviderWrapper.convertToJavaName(entry.getKey(), true),
                    entry.getValue());
        }
        return capitalizedPropertiesToTypesMap;
    }

    public Map<String, AttributeIfc> getJmxPropertiesToTypesMap() {
        final Map<String, AttributeIfc> jmxPropertiesToTypesMap = Maps.newHashMap();
        for (final Entry<String, AttributeIfc> entry : this.yangNameToAttributeMap
                .entrySet()) {

            jmxPropertiesToTypesMap.put(
                    TypeProviderWrapper.convertToJavaName(entry.getKey(), false),
                    entry.getValue());
        }
        return jmxPropertiesToTypesMap;
    }

    public Map<String, AttributeIfc> getYangPropertiesToTypesMap() {
        return this.yangNameToAttributeMap;
    }

    @Override
    public String getNullableDescription() {
        return this.nullableDescription;
    }

    @Override
    public String getNullableDefault() {
        return this.nullableDefault;
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

        final TOAttribute that = (TOAttribute) o;

        if (this.nullableDefault != null ? !this.nullableDefault
                .equals(that.nullableDefault) : that.nullableDefault != null) {
            return false;
        }
        if (this.nullableDescription != null ? !this.nullableDescription
                .equals(that.nullableDescription)
                : that.nullableDescription != null) {
            return false;
        }
        if (this.yangNameToAttributeMap != null ? !this.yangNameToAttributeMap
                .equals(that.yangNameToAttributeMap)
                : that.yangNameToAttributeMap != null) {
            return false;
        }

        return true;
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
        result = (31
                * result)
                + (this.yangNameToAttributeMap != null ? this.yangNameToAttributeMap
                        .hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TOAttribute{" + getAttributeYangName() + "," + "to="
                + this.yangNameToAttributeMap + '}';
    }

    @Override
    public Type getType() {
        // TODO: ReferencedTypeImpl from Types
        return new ReferencedTypeImpl(this.packageName, getUpperCaseCammelCase());
    }

    @Override
    public CompositeType getOpenType() {
        final String description = getNullableDescription() == null ? getAttributeYangName() : getNullableDescription();

        final FunctionImpl functionImpl = new FunctionImpl();
        final Map<String, AttributeIfc> jmxPropertiesToTypesMap = getJmxPropertiesToTypesMap();
        final OpenType<?>[] itemTypes = Collections2.transform(
                jmxPropertiesToTypesMap.entrySet(), functionImpl).toArray(
                new OpenType<?>[] {});
        final String[] itemNames = functionImpl.getItemNames();
        try {
            // TODO add package name to create fully qualified name for this
            // type
            final CompositeType compositeType = new CompositeType(
                    getUpperCaseCammelCase(), description, itemNames,
                    itemNames, itemTypes);
            return compositeType;
        } catch (final OpenDataException e) {
            throw new RuntimeException("Unable to create CompositeType for "
                    + this, e);
        }
    }

    public String getPackageName() {
        return this.packageName;
    }

}

class FunctionImpl implements
        Function<Entry<String, AttributeIfc>, OpenType<?>> {
    private final List<String> itemNames = new ArrayList<>();

    @Override
    public OpenType<?> apply(final Entry<String, AttributeIfc> input) {
        final AttributeIfc innerType = input.getValue();
        this.itemNames.add(input.getKey());
        return innerType.getOpenType();
    }

    public String[] getItemNames(){
        return this.itemNames.toArray(new String[this.itemNames.size()]);
    }
}
