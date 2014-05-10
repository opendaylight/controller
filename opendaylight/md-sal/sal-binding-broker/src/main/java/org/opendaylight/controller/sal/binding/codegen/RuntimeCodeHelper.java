/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.codegen;

import java.lang.reflect.Field;
import java.util.Map;

import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;

public final class RuntimeCodeHelper {
    private RuntimeCodeHelper() {
        throw new UnsupportedOperationException("Utility class should never be instantiated");
    }

    private static Field getField(final Class<?> cls, final String name) {
        try {
            return cls.getField(name);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException(
                    String.format("Class %s is missing field %s", cls, name), e);
        } catch (SecurityException e) {
            throw new IllegalStateException(String.format("Failed to examine class %s", cls), e);
        }
    }

    private static Field getDelegateField(final Class<?> cls) {
        return getField(cls, RuntimeCodeSpecification.DELEGATE_FIELD);
    }

    private static Object getFieldValue(final Field field, final Object obj) {
        try {
            return field.get(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(String.format("Failed to get field %s of object %s", field, obj), e);
        }
    }

    private static void setFieldValue(final Field field, final Object obj, final Object value) {
        try {
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(String.format("Failed to set field %s to %s", field, value), e);
        }
    }

    /**
     * Helper method to return delegate from ManagedDirectedProxy with use of reflection.
     *
     * Note: This method uses reflection, but access to delegate field should be
     * avoided and called only if necessary.
     */
    @SuppressWarnings("unchecked")
    public static <T extends RpcService> T getDelegate(final RpcService proxy) {
        return (T)getFieldValue(getDelegateField(proxy.getClass()), proxy);
    }

    /**
     * Helper method to set delegate to ManagedDirectedProxy with use of reflection.
     *
     * Note: This method uses reflection, but setting delegate field should not occur too much
     * to introduce any significant performance hits.
     */
    public static void setDelegate(final Object proxy, final Object delegate) {
        final Field field = getDelegateField(proxy.getClass());

        if (delegate != null) {
            final Class<?> ft = field.getType();
            if (!ft.isAssignableFrom(delegate.getClass())) {
                throw new IllegalArgumentException(
                        String.format("Field %s type %s is not compatible with delegate type %s",
                                field, ft, delegate.getClass()));
            }
        }

        setFieldValue(field, proxy, delegate);
    }

    @SuppressWarnings("unchecked")
    public static Map<InstanceIdentifier<? extends Object>,? extends RpcService> getRoutingTable(final RpcService target, final Class<? extends BaseIdentity> tableClass) {
        final Field field = getField(target.getClass(), RuntimeCodeSpecification.getRoutingTableField(tableClass));
        return (Map<InstanceIdentifier<? extends Object>,? extends RpcService>) getFieldValue(field, target);
    }

    public static void setRoutingTable(final RpcService target, final Class<? extends BaseIdentity> tableClass, final Map<InstanceIdentifier<? extends Object>,? extends RpcService> routingTable) {
        final Field field = getField(target.getClass(), RuntimeCodeSpecification.getRoutingTableField(tableClass));
        setFieldValue(field, target, routingTable);
    }
}
