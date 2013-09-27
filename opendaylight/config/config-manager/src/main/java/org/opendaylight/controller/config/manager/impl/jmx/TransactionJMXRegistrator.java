/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.jmx;

import java.io.Closeable;
import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;
import javax.management.QueryExp;

import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.manager.impl.jmx.InternalJMXRegistrator.InternalJMXRegistration;

/**
 * Contains constraints on passed {@link ObjectName} parameters. Only allow (un)
 * registration of ObjectNames that have expected transaction name.
 */
public class TransactionJMXRegistrator implements Closeable {
    private final InternalJMXRegistrator childJMXRegistrator;
    private final String transactionName;

    TransactionJMXRegistrator(InternalJMXRegistrator internalJMXRegistrator,
            String transactionName) {
        this.childJMXRegistrator = internalJMXRegistrator.createChild();
        this.transactionName = transactionName;
    }

    public static class TransactionJMXRegistration implements AutoCloseable {
        private final InternalJMXRegistration registration;

        TransactionJMXRegistration(InternalJMXRegistration registration) {
            this.registration = registration;
        }

        @Override
        public void close() {
            registration.close();
        }
    }

    public TransactionJMXRegistration registerMBean(Object object, ObjectName on)
            throws InstanceAlreadyExistsException {
        if (!transactionName.equals(ObjectNameUtil.getTransactionName(on)))
            throw new IllegalArgumentException(
                    "Transaction name mismatch between expected "
                            + transactionName + " " + "and " + on);
        ObjectNameUtil.checkType(on, ObjectNameUtil.TYPE_CONFIG_TRANSACTION);
        return new TransactionJMXRegistration(
                childJMXRegistrator.registerMBean(object, on));
    }

    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
        return childJMXRegistrator.queryNames(name, query);
    }

    public TransactionModuleJMXRegistrator createTransactionModuleJMXRegistrator() {
        return new TransactionModuleJMXRegistrator(childJMXRegistrator,
                transactionName);
    }

    @Override
    public void close() {
        childJMXRegistrator.close();
    }
}
