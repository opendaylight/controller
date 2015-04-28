/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.lang.reflect.Method;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.annotations.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


abstract class ContextReferenceExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(ContextReferenceExtractor.class);
    private static final ContextReferenceExtractor NULL_EXTRACTOR = new ContextReferenceExtractor() {

        @Override
        InstanceIdentifier<?> extract(final DataObject obj) {
            return null;
        }
    };


    private static final LoadingCache<Class<?>, ContextReferenceExtractor> EXTRACTORS = CacheBuilder.newBuilder()
            .weakKeys().build(new CacheLoader<Class<?>, ContextReferenceExtractor>() {

                @Override
                public ContextReferenceExtractor load(final Class<?> key) throws Exception {
                    return create(key);
                }
            });


    private static final String GET_VALUE_NAME = "getValue";

    static ContextReferenceExtractor from(final Class<?> obj) {
        return EXTRACTORS.getUnchecked(obj);
    }

    /**
     * Extract context-reference (Instance Identifier) from
     * Binding DataObject.
     *
     * @param obj DataObject from which context reference
     * should be extracted.
     *
     * @return Instance Identifier representing context reference
     * or null, if data object does not contain context reference.
     */
    abstract @Nullable InstanceIdentifier<?> extract(DataObject obj);

    @Nonnull
    private static ContextReferenceExtractor create(final Class<?> key) {
        final Method contextGetter = getContextGetter(key);
        if (contextGetter == null) {
            return NULL_EXTRACTOR;
        }
        final Class<?> returnType = contextGetter.getReturnType();
        try {
            if (InstanceIdentifier.class.isAssignableFrom(returnType)) {
                return DirectGetterRouteContextExtractor.create(contextGetter);
            }
            final Method getValueMethod = findGetValueMethod(returnType,InstanceIdentifier.class);
            if (getValueMethod != null) {
                return GetValueRouteContextExtractor.create(contextGetter, getValueMethod);
            } else {
                LOG.warn("Class {} can not be used to determine context, falling back to NULL_EXTRACTOR.",returnType);
            }
        } catch (final IllegalAccessException e) {
            LOG.warn("Class {} does not conform to Binding Specification v1. Falling back to NULL_EXTRACTOR", e);
        }
        return NULL_EXTRACTOR;
    }

    @Nullable
    private static Method findGetValueMethod(final Class<?> type, final Class<?> returnType) {
        try {
            final Method method = type.getMethod(GET_VALUE_NAME);
            if(returnType.equals(method.getReturnType())) {
                return method;
            }
        } catch (final NoSuchMethodException e) {
            LOG.warn("Value class {} does not comform to Binding Specification v1.", type, e);
        }
        return null;
    }

    private static Method getContextGetter(final Class<?> key) {
        for (final Method method : key.getMethods()) {
            if (method.getAnnotation(RoutingContext.class) != null) {
                return method;
            }
        }
        return null;
    }



}
