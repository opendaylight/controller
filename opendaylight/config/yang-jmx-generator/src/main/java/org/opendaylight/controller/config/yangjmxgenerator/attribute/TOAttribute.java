/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.attribute;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;

import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.TypeProviderWrapper;
import org.opendaylight.yangtools.yang.model.api.AugmentationTarget;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class TOAttribute extends AbstractAttribute {

    private final String nullableDescription, nullableDefault;
    private final Map<String, AttributeIfc> yangNameToAttributeMap;
    private final Map<String, String> attributeNameMap;

    private static final Set<Class<? extends DataSchemaNode>> ALLOWED_CHILDREN = Sets
            .newHashSet();
    static {
        ALLOWED_CHILDREN.add(LeafListSchemaNode.class);
        ALLOWED_CHILDREN.add(ListSchemaNode.class);
        ALLOWED_CHILDREN.add(LeafSchemaNode.class);
        ALLOWED_CHILDREN.add(ContainerSchemaNode.class);
    }

    public static <T extends DataNodeContainer & AugmentationTarget & DataSchemaNode> TOAttribute create(
            T containerSchemaNode, TypeProviderWrapper typeProviderWrapper) {
        // Transfer Object: get the leaves
        Map<String, AttributeIfc> map = new HashMap<>();
        Map<String, String> attributeNameMap = new HashMap<>();
        for (DataSchemaNode dataSchemaNode : containerSchemaNode
                .getChildNodes()) {
            try {
                String yangName = dataSchemaNode.getQName().getLocalName();
                map.put(yangName,
                        createInnerAttribute(dataSchemaNode,
                                typeProviderWrapper));
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Unable to create TO");
            }
        }
        return new TOAttribute(containerSchemaNode, map, attributeNameMap,
                containerSchemaNode.getDescription());
    }

    private static AttributeIfc createInnerAttribute(
            DataSchemaNode dataSchemaNode,
            TypeProviderWrapper typeProviderWrapper) {
        Class<? extends DataSchemaNode> type = isAllowedType(dataSchemaNode);

        if (type.equals(LeafSchemaNode.class))
            return new JavaAttribute((LeafSchemaNode) dataSchemaNode,
                    typeProviderWrapper);
        else if (type.equals(ListSchemaNode.class))
            return ListAttribute.create((ListSchemaNode) dataSchemaNode,
                    typeProviderWrapper);
        else if (type.equals(LeafListSchemaNode.class))
            return ListAttribute.create((LeafListSchemaNode) dataSchemaNode,
                    typeProviderWrapper);
        else if (type.equals(ContainerSchemaNode.class))
            return TOAttribute.create((ContainerSchemaNode) dataSchemaNode,
                    typeProviderWrapper);

        throw new IllegalStateException("This should never happen");
    }

    private static Class<? extends DataSchemaNode> isAllowedType(
            DataSchemaNode dataSchemaNode) {
        for (Class<? extends DataSchemaNode> allowedType : ALLOWED_CHILDREN) {
            if (allowedType.isAssignableFrom(dataSchemaNode.getClass()) == true)
                return allowedType;
        }
        throw new IllegalArgumentException("Illegal child node for TO: "
                + dataSchemaNode.getClass() + " allowed node types: "
                + ALLOWED_CHILDREN);
    }

    private TOAttribute(DataSchemaNode attrNode,
            Map<String, AttributeIfc> transferObject,
            Map<String, String> attributeNameMap, String nullableDescription) {
        super(attrNode);
        yangNameToAttributeMap = transferObject;
        this.attributeNameMap = attributeNameMap;
        this.nullableDescription = nullableDescription;
        nullableDefault = null;
    }

    public Map<String, String> getAttributeNameMap() {
        return attributeNameMap;
    }

    public Map<String, AttributeIfc> getCapitalizedPropertiesToTypesMap() {
        Map<String, AttributeIfc> capitalizedPropertiesToTypesMap = Maps
                .newHashMap();
        for (Entry<String, AttributeIfc> entry : yangNameToAttributeMap
                .entrySet()) {

            capitalizedPropertiesToTypesMap.put(
                    ModuleMXBeanEntry.convertToJavaName(entry.getKey(), true),
                    entry.getValue());
        }
        return capitalizedPropertiesToTypesMap;
    }

    public Map<String, AttributeIfc> getJmxPropertiesToTypesMap() {
        Map<String, AttributeIfc> jmxPropertiesToTypesMap = Maps.newHashMap();
        for (Entry<String, AttributeIfc> entry : yangNameToAttributeMap
                .entrySet()) {

            jmxPropertiesToTypesMap.put(
                    ModuleMXBeanEntry.convertToJavaName(entry.getKey(), false),
                    entry.getValue());
        }
        return jmxPropertiesToTypesMap;
    }

    public Map<String, AttributeIfc> getYangPropertiesToTypesMap() {
        return yangNameToAttributeMap;
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

        TOAttribute that = (TOAttribute) o;

        if (nullableDefault != null ? !nullableDefault
                .equals(that.nullableDefault) : that.nullableDefault != null)
            return false;
        if (nullableDescription != null ? !nullableDescription
                .equals(that.nullableDescription)
                : that.nullableDescription != null)
            return false;
        if (yangNameToAttributeMap != null ? !yangNameToAttributeMap
                .equals(that.yangNameToAttributeMap)
                : that.yangNameToAttributeMap != null)
            return false;

        return true;
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
        result = 31
                * result
                + (yangNameToAttributeMap != null ? yangNameToAttributeMap
                        .hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TOAttribute{" + getAttributeYangName() + "," + "to="
                + yangNameToAttributeMap + '}';
    }

    @Override
    public OpenType<?> getOpenType() {
        String description = getNullableDescription() == null ? getAttributeYangName()
                : getNullableDescription();
        final String[] itemNames = new String[yangNameToAttributeMap.keySet()
                .size()];
        String[] itemDescriptions = itemNames;
        FunctionImpl functionImpl = new FunctionImpl(itemNames);
        Map<String, AttributeIfc> jmxPropertiesToTypesMap = getJmxPropertiesToTypesMap();
        OpenType<?>[] itemTypes = Collections2.transform(
                jmxPropertiesToTypesMap.entrySet(), functionImpl).toArray(
                new OpenType<?>[] {});
        try {
            // TODO add package name to create fully qualified name for this
            // type
            CompositeType compositeType = new CompositeType(
                    getUpperCaseCammelCase(), description, itemNames,
                    itemDescriptions, itemTypes);
            return compositeType;
        } catch (OpenDataException e) {
            throw new RuntimeException("Unable to create CompositeType for "
                    + this, e);
        }
    }

    private static final class FunctionImpl implements
            Function<Entry<String, AttributeIfc>, OpenType<?>> {
        private final String[] itemNames;
        int i = 0;

        private FunctionImpl(String[] itemNames) {
            this.itemNames = itemNames;
        }

        @Override
        public OpenType<?> apply(Entry<String, AttributeIfc> input) {
            AttributeIfc innerType = input.getValue();
            itemNames[i++] = input.getKey();
            return innerType.getOpenType();
        }
    }
}
