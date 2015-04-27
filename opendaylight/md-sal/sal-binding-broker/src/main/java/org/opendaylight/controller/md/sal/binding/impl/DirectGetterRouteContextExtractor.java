/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.base.Throwables;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class DirectGetterRouteContextExtractor extends ContextReferenceExtractor {

    private final MethodHandle handle;

    DirectGetterRouteContextExtractor(final Method contextGetter) {
        try {
            handle = MethodHandles.publicLookup().unreflect(contextGetter).asType(MethodType.methodType(InstanceIdentifier.class,DataObject.class));
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException("Can not access public getter.",e);
        }
    }

    @Override
    InstanceIdentifier<?> extract(final DataObject obj) {
        try {
            return (InstanceIdentifier<?>) handle.invoke(obj);
        } catch (final Throwable e) {
            throw Throwables.propagate(e);
        }
    }

}
