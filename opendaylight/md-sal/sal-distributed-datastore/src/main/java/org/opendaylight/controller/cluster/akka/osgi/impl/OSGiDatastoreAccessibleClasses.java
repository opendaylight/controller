/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.akka.osgi.impl;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.akka.impl.DatastoreAccessibleClasses;
import org.opendaylight.controller.pekko.support.spi.PekkoAccessibleClasses;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * OSGi component announcing {@link DatastoreAccessibleClasses}.
 */
@NonNullByDefault
@Component(immediate = true, service = PekkoAccessibleClasses.class)
public class OSGiDatastoreAccessibleClasses extends DatastoreAccessibleClasses {
    @Activate
    public OSGiDatastoreAccessibleClasses(final BundleContext bundleContext) {
        super(OSGiAccessibleClassLoader.of(bundleContext));
    }
}
