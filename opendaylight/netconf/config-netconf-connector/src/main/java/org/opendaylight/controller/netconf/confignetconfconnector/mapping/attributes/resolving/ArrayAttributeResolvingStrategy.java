/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.resolving;

import com.google.common.base.Optional;
import java.lang.reflect.Array;
import java.util.List;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.util.NetconfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ArrayAttributeResolvingStrategy extends AbstractAttributeResolvingStrategy<Object, ArrayType<?>> {

    private final AttributeResolvingStrategy<?, ? extends OpenType<?>> innerTypeResolvingStrategy;

    private static final Logger LOG = LoggerFactory.getLogger(ArrayAttributeResolvingStrategy.class);

    public ArrayAttributeResolvingStrategy(AttributeResolvingStrategy<?, ? extends OpenType<?>> innerTypeResolved,
            ArrayType<?> openType) {
        super(openType);
        this.innerTypeResolvingStrategy = innerTypeResolved;
    }

    @Override
    public Optional<Object> parseAttribute(String attrName, Object value) throws NetconfDocumentedException {
        if (value == null) {
            return Optional.absent();
        }

        NetconfUtil.checkType(value, List.class);
        List<?> valueList = (List<?>) value;

        Class<?> innerTypeClass = null;

        if (innerTypeResolvingStrategy.getOpenType() instanceof CompositeType) {
            innerTypeClass = CompositeDataSupport.class;
        } else {
            try {
                innerTypeClass = Class.forName(getOpenType().getElementOpenType().getClassName());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Unable to locate class for "
                        + getOpenType().getElementOpenType().getClassName(), e);
            }
        }

        Object parsedArray = null;

        if (getOpenType().isPrimitiveArray()) {
            Class<?> primitiveType = getPrimitiveType(innerTypeClass);
            parsedArray = Array.newInstance(primitiveType, valueList.size());
        } else {
            parsedArray = Array.newInstance(innerTypeClass, valueList.size());
        }

        int i = 0;
        for (Object innerValue : valueList) {
            Optional<?> parsedElement = innerTypeResolvingStrategy.parseAttribute(attrName + "_" + i, innerValue);
            if (!parsedElement.isPresent()){
                continue;
            }
            Array.set(parsedArray, i, parsedElement.get());
            i++;
        }

        LOG.debug("Attribute {} : {} parsed to type {} as {}", attrName, value, getOpenType(),
                toStringArray(parsedArray));

        return Optional.of(parsedArray);
    }

    private static String toStringArray(Object array) {
        StringBuilder build = new StringBuilder(array.toString());
        build.append(" [");
        for (int i = 0; i < Array.getLength(array); i++) {
            build.append(Array.get(array, i).toString());
            build.append(",");
        }
        build.append("]]");
        return build.toString();
    }

    private static Class<?> getPrimitiveType(Class<?> innerTypeClass) {
        try {
            return (Class<?>) innerTypeClass.getField("TYPE").get(null);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to determine primitive type to " + innerTypeClass);
        }
    }

    @Override
    public String toString() {
        return "ResolvedArrayTypeAttributeType [innerTypeResolved=" + innerTypeResolvingStrategy + "]";
    }
}
