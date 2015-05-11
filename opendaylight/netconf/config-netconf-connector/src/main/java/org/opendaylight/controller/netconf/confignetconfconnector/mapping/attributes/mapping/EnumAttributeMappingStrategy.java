/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.mapping;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import javax.management.openmbean.SimpleType;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.EnumResolver;

public class EnumAttributeMappingStrategy extends AbstractAttributeMappingStrategy<String, SimpleType<?>> {

    private final EnumResolver enumResolver;

    public EnumAttributeMappingStrategy(SimpleType<?> openType, final EnumResolver enumResolver) {
        super(openType);
        this.enumResolver = enumResolver;
    }

    @Override
    public Optional<String> mapAttribute(Object value) {
        if (value == null){
            return Optional.absent();
        }

        String expectedClass = getOpenType().getClassName();
        String realClass = value.getClass().getName();
        Preconditions.checkArgument(realClass.equals(expectedClass), "Type mismatch, expected " + expectedClass
                + " but was " + realClass);

        final String className = getOpenType().getClassName();
        return Optional.of(enumResolver.toYang(className, value.toString()));
    }

}
