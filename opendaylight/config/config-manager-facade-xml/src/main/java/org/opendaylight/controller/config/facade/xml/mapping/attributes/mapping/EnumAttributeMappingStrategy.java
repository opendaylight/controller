/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.mapping;

import com.google.common.base.Optional;
import javax.management.openmbean.CompositeType;
import org.opendaylight.controller.config.facade.xml.osgi.EnumResolver;

public class EnumAttributeMappingStrategy extends AbstractAttributeMappingStrategy<String, CompositeType> {

    private final EnumResolver enumResolver;

    public EnumAttributeMappingStrategy(CompositeType openType, final EnumResolver enumResolver) {
        super(openType);
        this.enumResolver = enumResolver;
    }

    @Override
    public Optional<String> mapAttribute(Object value) {
        if (value == null){
            return Optional.absent();
        }

        String expectedClass = getOpenType().getTypeName();
        return Optional.of(enumResolver.toYang(expectedClass, value.toString()));
    }

}
