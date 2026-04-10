/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import static java.util.Objects.requireNonNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.contract.Naming;
import org.opendaylight.yangtools.yang.common.QName;
import org.osgi.service.blueprint.container.ComponentDefinitionException;

/**
 * Helper to tie together a {@link DataObject} class and its corresponding {@link QName}.
 */
record BindingClass<T extends DataObject>(@NonNull Class<T> clazz, @NonNull QName qname) {
    BindingClass {
        requireNonNull(clazz);
        requireNonNull(qname);
    }

    static <T extends DataObject> @NonNull BindingClass<T> of(final @NonNull String logName,
            final @NonNull Class<T> clazz) {
        final MethodHandle getter;
        try {
            getter = MethodHandles.publicLookup().findStaticGetter(clazz, Naming.QNAME_STATIC_FIELD_NAME, QName.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ComponentDefinitionException("%s: Cannot find field %s.%s".formatted(
                logName, clazz.getCanonicalName(), Naming.QNAME_STATIC_FIELD_NAME), e);
        }

        final var qname = readQName(getter);
        if (qname == null) {
            throw new ComponentDefinitionException("%s: field %s.%s is null: what is going on?!".formatted(
                logName, Naming.QNAME_STATIC_FIELD_NAME, clazz.getCanonicalName()));
        }
        return new BindingClass<>(clazz, qname);
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    private static QName readQName(final MethodHandle getter) {
        try {
            return (QName) getter.invokeExact();
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to read " + Naming.QNAME_STATIC_FIELD_NAME, e);
        }
    }
}
