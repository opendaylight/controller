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
import org.opendaylight.controller.netconf.confignetconfconnector.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.openmbean.SimpleType;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Date;
import java.util.Map;

final class SimpleAttributeResolvingStrategy extends AbstractAttributeResolvingStrategy<Object, SimpleType<?>> {

    private static final Logger logger = LoggerFactory.getLogger(SimpleAttributeResolvingStrategy.class);

    SimpleAttributeResolvingStrategy(SimpleType<?> simpleType) {
        super(simpleType);
    }

    @Override
    public String toString() {
        return "ResolvedSimpleAttribute [" + getOpenType().getClassName() + "]";
    }

    @Override
    public Optional<Object> parseAttribute(String attrName, Object value) {
        if (value == null) {
            return Optional.absent();
        }

        Class<?> cls;
        try {
            cls = Class.forName(getOpenType().getClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to locate class for " + getOpenType().getClassName(), e);
        }

        Util.checkType(value, String.class);

        Resolver prefferedPlugin = resolverPlugins.get(cls.getCanonicalName());
        prefferedPlugin = prefferedPlugin == null ? resolverPlugins.get(DEFAULT_RESOLVERS) : prefferedPlugin;

        Object parsedValue = prefferedPlugin.resolveObject(cls, attrName, (String) value);
        logger.debug("Attribute {} : {} parsed to type {} with value {}", attrName, value, getOpenType(), parsedValue);
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
    }

    static interface Resolver {
        Object resolveObject(Class<?> type, String attrName, String value);
    }

    static class DefaultResolver implements Resolver {

        @Override
        public Object resolveObject(Class<?> type, String attrName, String value) {
            try {
                Object parsedValue = parseObject(type, value);
                return parsedValue;
            } catch (Exception e) {
                throw new RuntimeException("Unable to resolve attribute " + attrName + " from " + value, e);
            }
        }

        protected Object parseObject(Class<?> type, String value) throws Exception {
            Method method = type.getMethod("valueOf", String.class);
            Object parsedValue = method.invoke(null, value);
            return parsedValue;
        }
    }

    static class StringResolver extends DefaultResolver {

        @Override
        protected Object parseObject(Class<?> type, String value) throws Exception {
            return value;
        }
    }

    static class BigIntegerResolver extends DefaultResolver {

        @Override
        protected Object parseObject(Class<?> type, String value) throws Exception {
            return new BigInteger(value);
        }
    }

    static class CharResolver extends DefaultResolver {

        @Override
        protected Object parseObject(Class<?> type, String value) throws Exception {
            return new Character(value.charAt(0));
        }
    }

    static class DateResolver extends DefaultResolver {

        @Override
        protected Object parseObject(Class<?> type, String value) throws Exception {
            return Util.readDate(value);
        }
    }

}
