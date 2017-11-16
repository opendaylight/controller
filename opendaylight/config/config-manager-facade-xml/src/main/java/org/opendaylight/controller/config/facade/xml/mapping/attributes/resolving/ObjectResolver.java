/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.resolving;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Map.Entry;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.AttributeIfcSwitchStatement;
import org.opendaylight.controller.config.facade.xml.mapping.config.ServiceRegistryWrapper;
import org.opendaylight.controller.config.facade.xml.osgi.EnumResolver;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListDependenciesAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.TOAttribute;

public class ObjectResolver extends AttributeIfcSwitchStatement<AttributeResolvingStrategy<?, ? extends OpenType<?>>> {

    private final ServiceRegistryWrapper serviceTracker;
    private EnumResolver enumResolver;

    public ObjectResolver(final ServiceRegistryWrapper serviceTracker) {
        this.serviceTracker = serviceTracker;
    }

    @SuppressWarnings("checkstyle:hiddenField")
    public Map<String, AttributeResolvingStrategy<?, ? extends OpenType<?>>> prepareResolving(
            final Map<String, AttributeIfc> configDefinition, final EnumResolver enumResolver) {
        this.enumResolver = enumResolver;

        Map<String, AttributeResolvingStrategy<?, ? extends OpenType<?>>> strategies = Maps.newHashMap();

        for (Entry<String, AttributeIfc> attrEntry : configDefinition.entrySet()) {
            strategies.put(attrEntry.getKey(), prepareStrategy(attrEntry.getValue()));
        }

        return strategies;
    }

    private AttributeResolvingStrategy<?, ? extends OpenType<?>> prepareStrategy(final AttributeIfc attributeIfc) {
        return switchAttribute(attributeIfc);
    }

    private Map<String, String> createYangToJmxMapping(final TOAttribute attributeIfc) {
        Map<String, String> retVal = Maps.newHashMap();
        for (Entry<String, AttributeIfc> entry : attributeIfc.getYangPropertiesToTypesMap().entrySet()) {
            retVal.put(entry.getKey(), entry.getValue().getLowerCaseCammelCase());
        }
        return retVal;
    }

    @Override
    protected AttributeResolvingStrategy<?, ? extends OpenType<?>> caseJavaEnumAttribute(final OpenType<?> openType) {
        return new EnumAttributeResolvingStrategy((CompositeType) openType, enumResolver);
    }

    @Override
    protected AttributeResolvingStrategy<?, ? extends OpenType<?>> caseJavaSimpleAttribute(
            final SimpleType<?> openType) {
        return new SimpleAttributeResolvingStrategy(openType);
    }

    @Override
    protected AttributeResolvingStrategy<?, ? extends OpenType<?>> caseJavaArrayAttribute(final ArrayType<?> openType) {

        SimpleType<?> innerType = (SimpleType<?>) openType.getElementOpenType();
        AttributeResolvingStrategy<?, ? extends OpenType<?>> strat = new SimpleAttributeResolvingStrategy(innerType);
        return new ArrayAttributeResolvingStrategy(strat, openType);
    }

    @Override
    protected AttributeResolvingStrategy<?, ? extends OpenType<?>> caseJavaCompositeAttribute(
            final CompositeType openType) {
        Map<String, AttributeResolvingStrategy<?, ? extends OpenType<?>>> innerMap = Maps.newHashMap();
        Map<String, String> yangToJmxMapping = Maps.newHashMap();

        fillMappingForComposite(openType, innerMap, yangToJmxMapping);
        return new CompositeAttributeResolvingStrategy(innerMap, openType, yangToJmxMapping);
    }

    private void fillMappingForComposite(final CompositeType openType,
            final Map<String, AttributeResolvingStrategy<?, ? extends OpenType<?>>> innerMap,
            final Map<String, String> yangToJmxMapping) {
        for (String innerAttributeKey : openType.keySet()) {
            innerMap.put(innerAttributeKey, caseJavaAttribute(openType.getType(innerAttributeKey)));
            yangToJmxMapping.put(innerAttributeKey, innerAttributeKey);
        }
    }

    @Override
    protected AttributeResolvingStrategy<?, ? extends OpenType<?>> caseJavaUnionAttribute(final OpenType<?> openType) {

        Preconditions.checkState(openType instanceof CompositeType, "Unexpected open type, expected %s but was %s");
        CompositeType compositeType = (CompositeType) openType;

        Map<String, AttributeResolvingStrategy<?, ? extends OpenType<?>>> innerMap = Maps.newHashMap();
        Map<String, String> yangToJmxMapping = Maps.newHashMap();
        fillMappingForComposite(compositeType, innerMap, yangToJmxMapping);

        return new UnionCompositeAttributeResolvingStrategy(innerMap, compositeType, yangToJmxMapping);
    }

    @Override
    protected AttributeResolvingStrategy<?, ? extends OpenType<?>> caseDependencyAttribute(
            final SimpleType<?> openType) {
        return new ObjectNameAttributeResolvingStrategy(serviceTracker);
    }

    @Override
    protected AttributeResolvingStrategy<?, ? extends OpenType<?>> caseTOAttribute(final CompositeType openType) {
        Preconditions.checkState(getLastAttribute() instanceof TOAttribute);
        TOAttribute toAttribute = (TOAttribute) getLastAttribute();

        Map<String, AttributeResolvingStrategy<?, ? extends OpenType<?>>> innerMap = Maps.newHashMap();

        for (String innerName : openType.keySet()) {

            AttributeIfc innerAttributeIfc = toAttribute.getJmxPropertiesToTypesMap().get(innerName);
            innerMap.put(innerAttributeIfc.getAttributeYangName(), prepareStrategy(innerAttributeIfc));
        }
        return new CompositeAttributeResolvingStrategy(innerMap, openType, createYangToJmxMapping(toAttribute));
    }

    @Override
    protected AttributeResolvingStrategy<?, ? extends OpenType<?>> caseListAttribute(final ArrayType<?> openType) {
        Preconditions.checkState(getLastAttribute() instanceof ListAttribute);
        AttributeIfc innerAttribute = ((ListAttribute) getLastAttribute()).getInnerAttribute();
        return new ArrayAttributeResolvingStrategy(prepareStrategy(innerAttribute), openType);
    }

    @Override
    protected AttributeResolvingStrategy<?, ? extends OpenType<?>> caseListDependeciesAttribute(
            final ArrayType<?> openType) {
        Preconditions.checkState(getLastAttribute() instanceof ListDependenciesAttribute);
        return new ArrayAttributeResolvingStrategy(caseDependencyAttribute(SimpleType.OBJECTNAME), openType);
    }
}
