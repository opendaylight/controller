/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.jmx;

import java.util.Set;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.QueryExp;
import org.opendaylight.controller.config.api.ModuleIdentifier;

public class BaseJMXRegistrator implements AutoCloseable, NestableJMXRegistrator {

    private final InternalJMXRegistrator internalJMXRegistrator;

    public BaseJMXRegistrator(final MBeanServer configMBeanServer) {
        internalJMXRegistrator = InternalJMXRegistrator.create(configMBeanServer);
    }

    public BaseJMXRegistrator(final InternalJMXRegistrator internalJMXRegistrator) {
        this.internalJMXRegistrator = internalJMXRegistrator;
    }

    public TransactionJMXRegistrator createTransactionJMXRegistrator(final String transactionName) {
        return new TransactionJMXRegistrator(internalJMXRegistrator.createChild(), transactionName);
    }

    public ModuleJMXRegistrator createModuleJMXRegistrator() {
        return new ModuleJMXRegistrator(internalJMXRegistrator.createChild());
    }

    public RootRuntimeBeanRegistratorImpl createRuntimeBeanRegistrator(final ModuleIdentifier moduleIdentifier) {
        return new RootRuntimeBeanRegistratorImpl(internalJMXRegistrator.createChild(), moduleIdentifier);
    }

    public Set<ObjectName> queryNames(final ObjectName name, final QueryExp query) {
        return internalJMXRegistrator.queryNames(name, query);
    }

    public Set<ObjectName> getRegisteredObjectNames() {
        return internalJMXRegistrator.getRegisteredObjectNames();
    }

    @Override
    public InternalJMXRegistrator createChild() {
        return internalJMXRegistrator.createChild();
    }

    @Override
    public void close() {
        internalJMXRegistrator.close();
    }
}
