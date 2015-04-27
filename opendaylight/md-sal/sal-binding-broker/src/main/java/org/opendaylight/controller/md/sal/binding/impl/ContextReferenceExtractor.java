/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
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
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.annotations.RoutingContext;


abstract class ContextReferenceExtractor {

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

    abstract @Nullable InstanceIdentifier<?> extract(DataObject obj);

    private static ContextReferenceExtractor create(final Class<?> key) {
        final Method contextGetter = getContextGetter(key);
        if(contextGetter == null) {
            return NULL_EXTRACTOR;
        }
        final Class<?> returnType = contextGetter.getReturnType();
        if(InstanceIdentifier.class.isAssignableFrom(returnType)) {
            return new DirectGetterRouteContextExtractor(contextGetter);
        }
        final Method getValueMethod = getGetValueMethod(returnType);
        final Class<?> valueType = getValueMethod.getReturnType();
        if(InstanceIdentifier.class.isAssignableFrom(valueType)) {
            return new GetValueRouteContextExtractor(contextGetter,getValueMethod);
        }

        return NULL_EXTRACTOR;
    }

    private static Method getGetValueMethod(final Class<?> returnType) {
        try {
            return returnType.getMethod(GET_VALUE_NAME);
        } catch (final NoSuchMethodException e) {
            throw new IllegalArgumentException("Not supported type of Binding DTO.",e);
        }
    }

    private static Method getContextGetter(final Class<?> key) {
        for(final Method method : key.getMethods()) {
            if(method.getAnnotation(RoutingContext.class) != null) {
                return method;
            }
        }
        return null;
    }



}
