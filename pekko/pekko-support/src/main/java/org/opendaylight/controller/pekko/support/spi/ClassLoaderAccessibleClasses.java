/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A set of {@link PekkoAccessibleClasses} backed by a {@link ClassLoader}.
 *
 * @since 14.0.0
 */
@Beta
@NonNullByDefault
public class ClassLoaderAccessibleClasses implements PekkoAccessibleClasses {
    private final ClassLoader classLoader;

    /**
     * Default constructor.
     *
     * @param classLoader backing class loader
     */
    protected ClassLoaderAccessibleClasses(final ClassLoader classLoader) {
        this.classLoader = requireNonNull(classLoader);
    }

    /**
     * Static factory method for creating instances backed by the class loader of specified class.
     *
     * @param classLoader the {@link ClassLoader}
     * @return A {@link PekkoAccessibleClasses}
     */
    public static final PekkoAccessibleClasses of(final ClassLoader classLoader) {
        return new ClassLoaderAccessibleClasses(classLoader);
    }

    /**
     * Static factory method for creating instances backed by the class loader of specified class.
     *
     * @param accessibleClass the {@link Class}
     * @return A {@link PekkoAccessibleClasses}
     */
    public static final PekkoAccessibleClasses ofLoaderOf(final Class<?> accessibleClass) {
        return of(accessibleClass.getClassLoader());
    }

    @Override
    public final ClassLoader asClassLoader() {
        return classLoader;
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("classLoader", classLoader).toString();
    }
}
