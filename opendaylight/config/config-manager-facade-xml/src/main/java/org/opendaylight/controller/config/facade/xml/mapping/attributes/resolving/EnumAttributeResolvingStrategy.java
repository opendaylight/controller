/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.resolving;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.Map;
import javax.management.openmbean.CompositeType;
import org.opendaylight.controller.config.facade.xml.osgi.EnumResolver;
import org.opendaylight.controller.config.facade.xml.util.Util;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class EnumAttributeResolvingStrategy extends AbstractAttributeResolvingStrategy<Object, CompositeType> {

    private static final Logger LOG = LoggerFactory.getLogger(EnumAttributeResolvingStrategy.class);
    private final EnumResolver enumResolver;

    EnumAttributeResolvingStrategy(CompositeType simpleType, final EnumResolver enumResolver) {
        super(simpleType);
        this.enumResolver = enumResolver;
    }

    @Override
    public String toString() {
        return "ResolvedEnumAttribute [" + getOpenType().getClassName() + "]";
    }

    @Override
    public Optional<Object> parseAttribute(String attrName, Object value) throws DocumentedException {
        if (value == null) {
            return Optional.absent();
        }

        Util.checkType(value, Map.class);
        Map<?, ?> valueMap = (Map<?, ?>) value;
        Preconditions.checkArgument(valueMap.size() == 1, "Unexpected value size " + value + " should be just 1 foe enum");
        final Object innerValue = valueMap.values().iterator().next();
        Util.checkType(innerValue, String.class);

        final String className = getOpenType().getTypeName();
        final Object parsedValue = enumResolver.fromYang(className, ((String) innerValue));

        LOG.debug("Attribute {} : {} parsed to enum type {} with value {}", attrName, innerValue, getOpenType(), parsedValue);
        return Optional.of(parsedValue);
    }

}
