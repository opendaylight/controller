/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.resolving;

import com.google.common.base.Optional;
import javax.management.openmbean.SimpleType;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.EnumResolver;
import org.opendaylight.controller.netconf.confignetconfconnector.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class EnumAttributeResolvingStrategy extends AbstractAttributeResolvingStrategy<Object, SimpleType<?>> {

    private static final Logger LOG = LoggerFactory.getLogger(EnumAttributeResolvingStrategy.class);
    private final EnumResolver enumResolver;

    EnumAttributeResolvingStrategy(SimpleType<?> simpleType, final EnumResolver enumResolver) {
        super(simpleType);
        this.enumResolver = enumResolver;
    }

    @Override
    public String toString() {
        return "ResolvedEnumAttribute [" + getOpenType().getClassName() + "]";
    }

    @Override
    public Optional<Object> parseAttribute(String attrName, Object value) throws NetconfDocumentedException {
        if (value == null) {
            return Optional.absent();
        }

        Util.checkType(value, String.class);

        final String className = getOpenType().getClassName();
        final Object parsedValue = enumResolver.fromYang(className, ((String) value));

        LOG.debug("Attribute {} : {} parsed to enum type {} with value {}", attrName, value, getOpenType(), parsedValue);
        return Optional.of(parsedValue);
    }

}
