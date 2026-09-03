/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.akka.impl;

import com.google.common.annotations.Beta;
import java.util.ServiceLoader;
import org.kohsuke.MetaInfServices;
import org.opendaylight.controller.pekko.support.spi.PekkoAccessibleClasses;

/**
 * {@link DatastoreAccessibleClasses} for single-class loader environments, like {@link ServiceLoader}.
 *
 * @since 14.0.0
 */
@Beta
@MetaInfServices(PekkoAccessibleClasses.class)
public final class DefaultDatastoreAccessibleClasses extends DatastoreAccessibleClasses {
    /**
     * Default constructor for {@link ServiceLoader}.
     */
    public DefaultDatastoreAccessibleClasses() {
        super(DatastoreAccessibleClasses.class.getClassLoader());
    }
}
