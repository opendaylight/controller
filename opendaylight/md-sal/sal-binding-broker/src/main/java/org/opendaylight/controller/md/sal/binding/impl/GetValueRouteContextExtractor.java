/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.base.Throwables;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class GetValueRouteContextExtractor extends ContextReferenceExtractor {

    private final static Lookup PUBLIC_LOOKUP = MethodHandles.publicLookup();
    private final MethodHandle contextHandle;
    private final MethodHandle valueHandle;

    private GetValueRouteContextExtractor(final MethodHandle rawContextHandle, final MethodHandle rawValueHandle) {
        contextHandle = rawContextHandle.asType(MethodType.methodType(Object.class, DataObject.class));
        valueHandle = rawValueHandle.asType(MethodType.methodType(InstanceIdentifier.class, Object.class));
    }

    public static ContextReferenceExtractor create(final Method contextGetter, final Method getValueMethod)
            throws IllegalAccessException {
        final MethodHandle rawContextHandle = PUBLIC_LOOKUP.unreflect(contextGetter);
        final MethodHandle rawValueHandle = PUBLIC_LOOKUP.unreflect(getValueMethod);
        return new GetValueRouteContextExtractor(rawContextHandle, rawValueHandle);
    }

    @Override
    InstanceIdentifier<?> extract(final DataObject obj) {
        try {
            final Object ctx = contextHandle.invokeExact(obj);
            if (ctx != null) {
                return (InstanceIdentifier<?>) valueHandle.invokeExact(ctx);
            }
            return null;
        } catch (final Throwable e) {
            throw Throwables.propagate(e);
        }
    }


}
