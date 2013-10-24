/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.jmx;

import org.opendaylight.controller.config.api.ModuleIdentifier;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.QueryExp;
import java.util.Set;

public class BaseJMXRegistrator implements AutoCloseable {

    private final InternalJMXRegistrator internalJMXRegistrator;

    public BaseJMXRegistrator(MBeanServer configMBeanServer) {
        internalJMXRegistrator = new InternalJMXRegistrator(configMBeanServer);
    }

    public BaseJMXRegistrator(InternalJMXRegistrator internalJMXRegistrator) {
        this.internalJMXRegistrator = internalJMXRegistrator;
    }

    public TransactionJMXRegistrator createTransactionJMXRegistrator(
            String transactionName) {
        return new TransactionJMXRegistrator(
                internalJMXRegistrator.createChild(), transactionName);
    }

    public ModuleJMXRegistrator createModuleJMXRegistrator() {
        return new ModuleJMXRegistrator(internalJMXRegistrator.createChild());
    }

    public RootRuntimeBeanRegistratorImpl createRuntimeBeanRegistrator(
            ModuleIdentifier moduleIdentifier) {
        return new RootRuntimeBeanRegistratorImpl(internalJMXRegistrator.createChild(),
                moduleIdentifier);
    }

    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
        return internalJMXRegistrator.queryNames(name, query);
    }

    public Set<ObjectName> getRegisteredObjectNames() {
        return internalJMXRegistrator.getRegisteredObjectNames();
    }

    @Override
    public void close() {
        internalJMXRegistrator.close();
    }
}
