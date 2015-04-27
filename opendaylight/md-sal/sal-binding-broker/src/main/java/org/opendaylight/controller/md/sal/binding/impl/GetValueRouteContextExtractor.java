/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.base.Throwables;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class GetValueRouteContextExtractor extends ContextReferenceExtractor {

    private final MethodHandle contextHandle;
    private final MethodHandle valueHandle;

    GetValueRouteContextExtractor(final Method contextGetter, final Method getValueMethod) {
        try {
            contextHandle =
                    MethodHandles.publicLookup().unreflect(contextGetter)
                            .asType(MethodType.methodType(Object.class, DataObject.class));
            valueHandle =
                    MethodHandles.publicLookup().unreflect(getValueMethod)
                            .asType(MethodType.methodType(InstanceIdentifier.class, Object.class));
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException("Can not access public getter.", e);
        }
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
