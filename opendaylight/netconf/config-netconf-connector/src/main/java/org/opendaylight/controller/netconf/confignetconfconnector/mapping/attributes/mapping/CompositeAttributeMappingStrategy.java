/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.mapping;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Set;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import org.opendaylight.controller.netconf.util.NetconfUtil;

public class CompositeAttributeMappingStrategy extends
        AbstractAttributeMappingStrategy<Map<String, Object>, CompositeType> {

    private final Map<String, AttributeMappingStrategy<?, ? extends OpenType<?>>> innerStrategies;
    private final Map<String, String> jmxToJavaNameMapping;

    public CompositeAttributeMappingStrategy(CompositeType compositeType,
            Map<String, AttributeMappingStrategy<?, ? extends OpenType<?>>> innerStrategies,
            Map<String, String> jmxToJavaNameMapping) {
        super(compositeType);
        this.innerStrategies = innerStrategies;
        this.jmxToJavaNameMapping = jmxToJavaNameMapping;
    }

    @Override
    public Optional<Map<String, Object>> mapAttribute(Object value) {
        if (value == null){
            return Optional.absent();
        }

        NetconfUtil.checkType(value, CompositeDataSupport.class);

        CompositeDataSupport compositeData = (CompositeDataSupport) value;
        CompositeType currentType = compositeData.getCompositeType();
        CompositeType expectedType = getOpenType();

        Set<String> expectedCompositeTypeKeys = expectedType.keySet();
        Set<String> currentCompositeTypeKeys = currentType.keySet();
        Preconditions.checkArgument(expectedCompositeTypeKeys.equals(currentCompositeTypeKeys),
                "Composite type mismatch, expected composite type with attributes " + expectedCompositeTypeKeys
                        + " but was " + currentCompositeTypeKeys);

        Map<String, Object> retVal = Maps.newHashMap();

        for (String jmxName : jmxToJavaNameMapping.keySet()) {
            Optional<?> mapped = mapInnerAttribute(compositeData, jmxName, expectedType.getDescription(jmxName));
            if(mapped.isPresent()){
                retVal.put(jmxToJavaNameMapping.get(jmxName), mapped.get());
            }
        }

        return Optional.of(retVal);
    }

    protected Optional<?> mapInnerAttribute(CompositeDataSupport compositeData, String jmxName, String description) {
        Object innerValue = compositeData.get(jmxName);

        AttributeMappingStrategy<?, ? extends OpenType<?>> attributeMappingStrategy = innerStrategies
                .get(jmxName);
        return attributeMappingStrategy.mapAttribute(innerValue);
    }

}
