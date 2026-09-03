/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.impl;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.kohsuke.MetaInfServices;
import org.opendaylight.controller.pekko.support.spi.ClassLoaderAccessibleClasses;
import org.opendaylight.controller.pekko.support.spi.PekkoAccessibleClasses;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * Classes accessible by default.
 */
@NonNullByDefault
@MetaInfServices(PekkoAccessibleClasses.class)
@Component(service = PekkoAccessibleClasses.class)
public final class SupportAccessibleClasses extends ClassLoaderAccessibleClasses {
    @Activate
    public SupportAccessibleClasses() {
        super(SupportAccessibleClasses.class.getClassLoader());
    }
}
