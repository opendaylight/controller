/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.resolving;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import javax.management.openmbean.SimpleType;
import org.opendaylight.controller.config.facade.xml.util.Util;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SimpleAttributeResolvingStrategy extends AbstractAttributeResolvingStrategy<Object, SimpleType<?>> {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleAttributeResolvingStrategy.class);

    SimpleAttributeResolvingStrategy(final SimpleType<?> simpleType) {
        super(simpleType);
    }

    @Override
    public String toString() {
        return "ResolvedSimpleAttribute [" + getOpenType().getClassName() + "]";
    }

    @Override
    public Optional<Object> parseAttribute(final String attrName, final Object value) throws DocumentedException {
        if (value == null) {
            return Optional.absent();
        }

        Class<?> cls;
        try {
            cls = Class.forName(getOpenType().getClassName());
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException("Unable to locate class for " + getOpenType().getClassName(), e);
        }

        Util.checkType(value, String.class);

        Resolver prefferedPlugin = resolverPlugins.get(cls.getCanonicalName());
        prefferedPlugin = prefferedPlugin == null ? resolverPlugins.get(DEFAULT_RESOLVERS) : prefferedPlugin;

        Object parsedValue = prefferedPlugin.resolveObject(cls, attrName, (String) value);
        LOG.debug("Attribute {} : {} parsed to type {} with value {}", attrName, value, getOpenType(), parsedValue);
        return Optional.of(parsedValue);
    }

    private static final String DEFAULT_RESOLVERS = "default";
    private static final Map<String, Resolver> resolverPlugins = Maps.newHashMap();

    static {
        resolverPlugins.put(DEFAULT_RESOLVERS, new DefaultResolver());
        resolverPlugins.put(String.class.getCanonicalName(), new StringResolver());
        resolverPlugins.put(Date.class.getCanonicalName(), new DateResolver());
        resolverPlugins.put(Character.class.getCanonicalName(), new CharResolver());
        resolverPlugins.put(BigInteger.class.getCanonicalName(), new BigIntegerResolver());
        resolverPlugins.put(BigDecimal.class.getCanonicalName(), new BigDecimalResolver());
    }

    interface Resolver {
        Object resolveObject(Class<?> type, String attrName, String value) throws DocumentedException;
    }

    static class DefaultResolver implements Resolver {

        @Override
        public Object resolveObject(final Class<?> type, final String attrName, final String value) throws DocumentedException {
            try {
                return parseObject(type, value);
            } catch (final Exception e) {
                throw new DocumentedException("Unable to resolve attribute " + attrName + " from " + value,
                        DocumentedException.ErrorType.APPLICATION,
                        DocumentedException.ErrorTag.OPERATION_FAILED,
                        DocumentedException.ErrorSeverity.ERROR);
            }
        }

        protected Object parseObject(final Class<?> type, final String value) throws DocumentedException {
            Method method = null;
            try {
                method = type.getMethod("valueOf", String.class);
                return method.invoke(null, value);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                LOG.trace("Error parsing object ",e);
                throw new DocumentedException("Error parsing object.",
                        DocumentedException.ErrorType.APPLICATION,
                        DocumentedException.ErrorTag.OPERATION_FAILED,
                        DocumentedException.ErrorSeverity.ERROR);
            }
        }
    }

    static class StringResolver extends DefaultResolver {

        @Override
        protected Object parseObject(final Class<?> type, final String value) {
            return value;
        }
    }

    static class BigIntegerResolver extends DefaultResolver {

        @Override
        protected Object parseObject(final Class<?> type, final String value) {
            return new BigInteger(value);
        }
    }

    static class BigDecimalResolver extends DefaultResolver {

        @Override
        protected Object parseObject(final Class<?> type, final String value) {
            return new BigDecimal(value);
        }
    }

    static class CharResolver extends DefaultResolver {

        @Override
        protected Object parseObject(final Class<?> type, final String value)  {
            return value.charAt(0);
        }
    }

    static class DateResolver extends DefaultResolver {
        @Override
        protected Object parseObject(final Class<?> type, final String value) throws DocumentedException {
            try {
                return Util.readDate(value);
            } catch (final ParseException e) {
                LOG.trace("Unable parse value {} due to ",value, e);
                throw new DocumentedException("Unable to parse value "+value+" as date.",
                        DocumentedException.ErrorType.APPLICATION,
                        DocumentedException.ErrorTag.OPERATION_FAILED,
                        DocumentedException.ErrorSeverity.ERROR);
            }
        }
    }

}
