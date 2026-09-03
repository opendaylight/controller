/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.impl;

import com.google.common.base.MoreObjects;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.pekko.support.spi.PekkoAccessibleClasses;

/**
 * A {@link ClassLoader} which delegates to a set of {@link PekkoAccessibleClass}.
 */
final class PekkoAccessibleClassLoader extends ClassLoader {
    @NonNullByDefault
    private final List<ClassLoader> loaders;

    @NonNullByDefault
    private PekkoAccessibleClassLoader(final List<ClassLoader> loaders) {
        super(UUID.randomUUID().toString(), PekkoAccessibleClassLoader.class.getClassLoader());
        this.loaders = List.copyOf(loaders);
    }

    @NonNullByDefault
    PekkoAccessibleClassLoader(final Stream<PekkoAccessibleClasses> accessibleClasses) {
        this(accessibleClasses.distinct().map(PekkoAccessibleClasses::asClassLoader).distinct().toList());
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        ClassNotFoundException cause = null;
        for (var loader : loaders) {
            try {
                return loader.loadClass(name);
            } catch (ClassNotFoundException e) {
                if (cause == null) {
                    cause = e;
                } else {
                    cause.addSuppressed(e);
                }
            }
        }
        throw cause != null ? cause : new ClassNotFoundException(name);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("uuid", getName()).add("loaders", loaders).toString();
    }

}
