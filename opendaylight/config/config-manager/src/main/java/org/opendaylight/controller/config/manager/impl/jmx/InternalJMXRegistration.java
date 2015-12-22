/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.jmx;

import com.google.common.base.Preconditions;
import javax.management.ObjectName;

final class InternalJMXRegistration implements AutoCloseable {
    private final InternalJMXRegistrator internalJMXRegistrator;
    private final ObjectName on;

    InternalJMXRegistration(final InternalJMXRegistrator internalJMXRegistrator, final ObjectName on) {
        this.internalJMXRegistrator = Preconditions.checkNotNull(internalJMXRegistrator);
        this.on = on;
    }

    @Override
    public void close() {
        internalJMXRegistrator.unregisterMBean(on);
    }
}
