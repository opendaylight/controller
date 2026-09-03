/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.akka.impl;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.pekko.support.spi.ClassLoaderAccessibleClasses;
import org.opendaylight.controller.pekko.support.spi.PekkoAccessibleClasses;

/**
 * {@link PekkoAccessibleClasses} required for the distributed datastore to work.
 */
@NonNullByDefault
public abstract class DatastoreAccessibleClasses extends ClassLoaderAccessibleClasses {
    /**
     * Default constructor.
     *
     * @param classLoader backing class loader
     */
    protected DatastoreAccessibleClasses(final ClassLoader classLoader) {
        super(classLoader);
    }
}
