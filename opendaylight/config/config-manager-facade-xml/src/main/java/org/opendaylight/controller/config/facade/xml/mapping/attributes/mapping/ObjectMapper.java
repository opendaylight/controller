/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.mapping;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Map.Entry;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.AttributeIfcSwitchStatement;
import org.opendaylight.controller.config.facade.xml.osgi.EnumResolver;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.DependencyAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListDependenciesAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.TOAttribute;

public class ObjectMapper extends AttributeIfcSwitchStatement<AttributeMappingStrategy<?, ? extends OpenType<?>>> {

    private EnumResolver enumResolver;

    @SuppressWarnings("checkstyle:hiddenField")
    public Map<String, AttributeMappingStrategy<?, ? extends OpenType<?>>> prepareMapping(
            final Map<String, AttributeIfc> configDefinition, final EnumResolver enumResolver) {
        this.enumResolver = Preconditions.checkNotNull(enumResolver);
        Map<String, AttributeMappingStrategy<?, ? extends OpenType<?>>> strategies = Maps.newHashMap();

        for (Entry<String, AttributeIfc> attrEntry : configDefinition.entrySet()) {
            strategies.put(attrEntry.getKey(), prepareStrategy(attrEntry.getValue()));
        }
        return strategies;
    }

    public AttributeMappingStrategy<?, ? extends OpenType<?>> prepareStrategy(final AttributeIfc attributeIfc) {

        if (attributeIfc instanceof DependencyAttribute) {
            namespaceOfDepAttr = ((DependencyAttribute) attributeIfc).getDependency().getSie().getQName().getNamespace()
                    .toString();
        } else if (attributeIfc instanceof ListDependenciesAttribute) {
            namespaceOfDepAttr = ((ListDependenciesAttribute) attributeIfc).getDependency().getSie().getQName()
                    .getNamespace().toString();
        }
        return switchAttribute(attributeIfc);
    }

    private Map<String, String> createJmxToYangMapping(final TOAttribute attributeIfc) {
        Map<String, String> retVal = Maps.newHashMap();
        for (Entry<String, AttributeIfc> entry : attributeIfc.getJmxPropertiesToTypesMap().entrySet()) {
            retVal.put(entry.getKey(), entry.getValue().getAttributeYangName());
        }
        return retVal;
    }

    @Override
    protected AttributeMappingStrategy<?, ? extends OpenType<?>> caseJavaSimpleAttribute(final SimpleType<?> openType) {
        return new SimpleAttributeMappingStrategy(openType);
    }

    @Override
    protected AttributeMappingStrategy<?, ? extends OpenType<?>> caseJavaEnumAttribute(final OpenType<?> openType) {
        return new EnumAttributeMappingStrategy((CompositeType) openType, enumResolver);
    }

    @Override
    protected AttributeMappingStrategy<?, ? extends OpenType<?>> caseJavaArrayAttribute(final ArrayType<?> openType) {

        AttributeMappingStrategy<?, ? extends OpenType<?>> innerStrategy = new SimpleAttributeMappingStrategy(
                (SimpleType<?>) openType.getElementOpenType());
        return new ArrayAttributeMappingStrategy(openType, innerStrategy);
    }

    @Override
    protected AttributeMappingStrategy<?, ? extends OpenType<?>> caseJavaCompositeAttribute(
            final CompositeType openType) {
        Map<String, AttributeMappingStrategy<?, ? extends OpenType<?>>> innerStrategies = Maps.newHashMap();

        Map<String, String> attributeMapping = Maps.newHashMap();

        for (String innerAttributeKey : openType.keySet()) {

            innerStrategies.put(innerAttributeKey, caseJavaAttribute(openType.getType(innerAttributeKey)));
            attributeMapping.put(innerAttributeKey, innerAttributeKey);
        }
        return new CompositeAttributeMappingStrategy(openType, innerStrategies, attributeMapping);
    }

    @Override
    protected AttributeMappingStrategy<?, ? extends OpenType<?>> caseJavaUnionAttribute(final OpenType<?> openType) {
        Map<String, AttributeMappingStrategy<?, ? extends OpenType<?>>> innerStrategies = Maps.newHashMap();

        Map<String, String> attributeMapping = Maps.newHashMap();

        CompositeType compositeType = (CompositeType) openType;
        for (String innerAttributeKey : compositeType.keySet()) {

            innerStrategies.put(innerAttributeKey, caseJavaAttribute(compositeType.getType(innerAttributeKey)));
            attributeMapping.put(innerAttributeKey, innerAttributeKey);
        }
        return new UnionCompositeAttributeMappingStrategy(compositeType, innerStrategies, attributeMapping);
    }

    private String namespaceOfDepAttr;

    @Override
    protected AttributeMappingStrategy<?, ? extends OpenType<?>> caseDependencyAttribute(final SimpleType<?> openType) {
        return new ObjectNameAttributeMappingStrategy(openType, namespaceOfDepAttr);
    }

    @Override
    protected AttributeMappingStrategy<?, ? extends OpenType<?>> caseTOAttribute(final CompositeType openType) {
        Map<String, AttributeMappingStrategy<?, ? extends OpenType<?>>> innerStrategies = Maps.newHashMap();

        Preconditions.checkState(getLastAttribute() instanceof TOAttribute);
        TOAttribute lastTO = (TOAttribute) getLastAttribute();

        for (Entry<String, AttributeIfc> innerAttrEntry : ((TOAttribute) getLastAttribute())
                .getJmxPropertiesToTypesMap().entrySet()) {
            innerStrategies.put(innerAttrEntry.getKey(), prepareStrategy(innerAttrEntry.getValue()));
        }

        return new CompositeAttributeMappingStrategy(openType, innerStrategies, createJmxToYangMapping(lastTO));
    }

    @Override
    protected AttributeMappingStrategy<?, ? extends OpenType<?>> caseListAttribute(final ArrayType<?> openType) {
        Preconditions.checkState(getLastAttribute() instanceof ListAttribute);
        return new ArrayAttributeMappingStrategy(openType,
                prepareStrategy(((ListAttribute) getLastAttribute()).getInnerAttribute()));
    }

    @Override
    protected AttributeMappingStrategy<?, ? extends OpenType<?>> caseListDependeciesAttribute(
            final ArrayType<?> openType) {
        Preconditions.checkState(getLastAttribute() instanceof ListDependenciesAttribute);
        return new ArrayAttributeMappingStrategy(openType, caseDependencyAttribute(SimpleType.OBJECTNAME));
    }
}
