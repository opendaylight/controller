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

final class DirectGetterRouteContextExtractor extends ContextReferenceExtractor {

    private final static Lookup PUBLIC_LOOKUP = MethodHandles.publicLookup();
    private final MethodHandle handle;

    private DirectGetterRouteContextExtractor(final MethodHandle rawHandle) {
        handle = rawHandle.asType(MethodType.methodType(InstanceIdentifier.class, DataObject.class));
    }

    static final ContextReferenceExtractor create(final Method getterMethod) throws IllegalAccessException {
        final MethodHandle getterHandle = PUBLIC_LOOKUP.unreflect(getterMethod);
        return new DirectGetterRouteContextExtractor(getterHandle);
    }

    @Override
    InstanceIdentifier<?> extract(final DataObject obj) {
        try {
            return (InstanceIdentifier<?>) handle.invokeExact(obj);
        } catch (final Throwable e) {
            throw Throwables.propagate(e);
        }
    }

}
