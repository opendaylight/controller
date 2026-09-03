/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.spi;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A set of {@link PekkoAccessibleClasses} backed by a {@link ClassLoader}.
 *
 * @param classLoader backing class loader
 * @since 14.0.0
 */
@NonNullByDefault
record ClassLoaderAccessibleClasses(ClassLoader classLoader) implements PekkoAccessibleClasses {
    ClassLoaderAccessibleClasses {
        requireNonNull(classLoader);
    }

    @Override
    public ClassLoader asClassLoader() {
        return classLoader;
    }
}
