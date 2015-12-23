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
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;

final class InternalJMXRegistration extends AbstractObjectRegistration<ObjectName> {
    private final InternalJMXRegistrator internalJMXRegistrator;

    InternalJMXRegistration(final InternalJMXRegistrator internalJMXRegistrator, final ObjectName on) {
        super(on);
        this.internalJMXRegistrator = Preconditions.checkNotNull(internalJMXRegistrator);
    }

    @Override
    protected void removeRegistration() {
        internalJMXRegistrator.unregisterMBean(getInstance());
    }
}
