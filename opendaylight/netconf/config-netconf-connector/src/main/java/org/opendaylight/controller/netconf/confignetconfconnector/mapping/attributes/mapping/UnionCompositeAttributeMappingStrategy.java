/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.mapping;

import com.google.common.base.Optional;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.JavaAttribute;

import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import java.util.Map;

public class UnionCompositeAttributeMappingStrategy extends
        CompositeAttributeMappingStrategy {


    public UnionCompositeAttributeMappingStrategy(CompositeType compositeType, Map<String, AttributeMappingStrategy<?, ? extends OpenType<?>>> innerStrategies, Map<String, String> jmxToJavaNameMapping) {
        super(compositeType, innerStrategies, jmxToJavaNameMapping);
    }

    @Override
    protected Optional<?> mapInnerAttribute(CompositeDataSupport compositeData, String jmxName, String description) {
        if(description.equals(JavaAttribute.DESCRIPTION_OF_VALUE_ATTRIBUTE_FOR_UNION) == false)
            return Optional.absent();

        return super.mapInnerAttribute(compositeData, jmxName, description);
    }
}
