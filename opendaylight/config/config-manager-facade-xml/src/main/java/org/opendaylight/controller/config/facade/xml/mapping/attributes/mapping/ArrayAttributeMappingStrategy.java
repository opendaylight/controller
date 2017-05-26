/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.mapping;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.lang.reflect.Array;
import java.util.List;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.OpenType;

public class ArrayAttributeMappingStrategy extends AbstractAttributeMappingStrategy<List<Object>, ArrayType<?>> {

    private final AttributeMappingStrategy<?, ? extends OpenType<?>> innerElementStrategy;

    public ArrayAttributeMappingStrategy(ArrayType<?> arrayType,
            AttributeMappingStrategy<?, ? extends OpenType<?>> innerElementStrategy) {
        super(arrayType);
        this.innerElementStrategy = innerElementStrategy;
    }

    @Override
    public Optional<List<Object>> mapAttribute(Object value) {
        if (value == null){
            return Optional.absent();
        }

        Preconditions.checkArgument(value.getClass().isArray(), "Value has to be instanceof Array ");

        List<Object> retVal = Lists.newArrayList();

        for (int i = 0; i < Array.getLength(value); i++) {
            Object innerValue = Array.get(value, i);
            Optional<?> mapAttribute = innerElementStrategy.mapAttribute(innerValue);

            if (mapAttribute.isPresent()){
                retVal.add(mapAttribute.get());
            }
        }

        return Optional.of(retVal);
    }

}
