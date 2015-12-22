/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.jmx;

import com.google.common.base.Preconditions;
import java.io.Closeable;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;

/**
 * This subclass is used for registering readable module into JMX, it is also
 * used as underlying provider in {@link RuntimeBeanRegistratorImpl}. Closing
 * the instance thus unregisters all JMX beans related to the module excluding
 * currently open transactions.
 */
@ThreadSafe
public class ModuleJMXRegistrator implements Closeable {
    private final InternalJMXRegistrator childJMXRegistrator;

    ModuleJMXRegistrator(final InternalJMXRegistrator internalJMXRegistrator) {
        this.childJMXRegistrator = Preconditions.checkNotNull(internalJMXRegistrator);
    }

    static class ModuleJMXRegistration implements AutoCloseable {
        private final InternalJMXRegistration internalJMXRegistration;

        ModuleJMXRegistration(final InternalJMXRegistration registration) {
            this.internalJMXRegistration = registration;
        }

        @Override
        public void close() {
            internalJMXRegistration.close();
        }
    }

    public ModuleJMXRegistration registerMBean(final Object object, final ObjectName on)
            throws InstanceAlreadyExistsException {
        ObjectNameUtil.checkType(on, ObjectNameUtil.TYPE_MODULE);
        if (ObjectNameUtil.getTransactionName(on) != null) {
            throw new IllegalArgumentException(
                    "Transaction name not expected in " + on);
        }
        return new ModuleJMXRegistration(childJMXRegistrator.registerMBean(
                object, on));
    }

    @Override
    public void close() {
        childJMXRegistrator.close();
    }

}
