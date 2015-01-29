/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.resolving;

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
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.util.NetconfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SimpleAttributeResolvingStrategy extends AbstractAttributeResolvingStrategy<Object, SimpleType<?>> {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleAttributeResolvingStrategy.class);

    SimpleAttributeResolvingStrategy(SimpleType<?> simpleType) {
        super(simpleType);
    }

    @Override
    public String toString() {
        return "ResolvedSimpleAttribute [" + getOpenType().getClassName() + "]";
    }

    @Override
    public Optional<Object> parseAttribute(String attrName, Object value) throws NetconfDocumentedException {
        if (value == null) {
            return Optional.absent();
        }

        Class<?> cls;
        try {
            cls = Class.forName(getOpenType().getClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to locate class for " + getOpenType().getClassName(), e);
        }

        NetconfUtil.checkType(value, String.class);

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

    static interface Resolver {
        Object resolveObject(Class<?> type, String attrName, String value) throws NetconfDocumentedException;
    }

    static class DefaultResolver implements Resolver {

        @Override
        public Object resolveObject(Class<?> type, String attrName, String value) throws NetconfDocumentedException {
            try {
                return parseObject(type, value);
            } catch (Exception e) {
                throw new NetconfDocumentedException("Unable to resolve attribute " + attrName + " from " + value,
                        NetconfDocumentedException.ErrorType.application,
                        NetconfDocumentedException.ErrorTag.operation_failed,
                        NetconfDocumentedException.ErrorSeverity.error);
            }
        }

        protected Object parseObject(Class<?> type, String value) throws NetconfDocumentedException {
            Method method = null;
            try {
                method = type.getMethod("valueOf", String.class);
                return method.invoke(null, value);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                LOG.trace("Error parsing object ",e);
                throw new NetconfDocumentedException("Error parsing object.",
                        NetconfDocumentedException.ErrorType.application,
                        NetconfDocumentedException.ErrorTag.operation_failed,
                        NetconfDocumentedException.ErrorSeverity.error);
            }
        }
    }

    static class StringResolver extends DefaultResolver {

        @Override
        protected Object parseObject(Class<?> type, String value) {
            return value;
        }
    }

    static class BigIntegerResolver extends DefaultResolver {

        @Override
        protected Object parseObject(Class<?> type, String value) {
            return new BigInteger(value);
        }
    }

    static class BigDecimalResolver extends DefaultResolver {

        @Override
        protected Object parseObject(Class<?> type, String value) {
            return new BigDecimal(value);
        }
    }

    static class CharResolver extends DefaultResolver {

        @Override
        protected Object parseObject(Class<?> type, String value)  {
            return value.charAt(0);
        }
    }

    static class DateResolver extends DefaultResolver {
        @Override
        protected Object parseObject(Class<?> type, String value) throws NetconfDocumentedException {
            try {
                return NetconfUtil.readDate(value);
            } catch (ParseException e) {
                LOG.trace("Unable parse value {} due to ",value, e);
                throw new NetconfDocumentedException("Unable to parse value "+value+" as date.",
                        NetconfDocumentedException.ErrorType.application,
                        NetconfDocumentedException.ErrorTag.operation_failed,
                        NetconfDocumentedException.ErrorSeverity.error);
            }
        }
    }

}
