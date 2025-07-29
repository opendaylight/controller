/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Proxy;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * An {@link ObjectInputStream} which is bounded in its loading to a {@link RestrictedClassLoader}.
 */
final class RestrictedObjectInputStream extends ObjectInputStream {
    private final RestrictedClassLoader classLoader;

    @NonNullByDefault
    RestrictedObjectInputStream(final InputStream in, final RestrictedClassLoader classLoader) throws IOException {
        super(requireNonNull(in));
        this.classLoader = requireNonNull(classLoader);
    }

    @Override
    protected Class<?> resolveClass(final ObjectStreamClass objectStreamClass) throws ClassNotFoundException {
        return classLoader.loadClass(objectStreamClass.getName());
    }

    @Override
    protected Class<?> resolveProxyClass(final String[] interfaces) throws ClassNotFoundException {
        final var length = interfaces.length;
        final var classes = new Class<?>[length];
        for (int i = 0; i < length; i++) {
            classes[i] = Class.forName(interfaces[i], false, classLoader);
        }

        try {
            return Proxy.getProxyClass(classLoader, classes);
        } catch (IllegalArgumentException e) {
            throw new ClassNotFoundException(null, e);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("classLoader", classLoader).toString();
    }
}
