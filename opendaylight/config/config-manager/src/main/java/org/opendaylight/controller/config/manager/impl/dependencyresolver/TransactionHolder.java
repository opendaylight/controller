/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.dependencyresolver;

import org.opendaylight.controller.config.api.JmxAttribute;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.manager.impl.CommitInfo;
import org.opendaylight.controller.config.manager.impl.ModuleInternalTransactionalInfo;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;

import javax.management.InstanceAlreadyExistsException;
import java.util.Map;

interface TransactionHolder {
    CommitInfo toCommitInfo();

    Module findModule(ModuleIdentifier moduleIdentifier,
            JmxAttribute jmxAttributeForReporting);

    ModuleFactory findModuleFactory(ModuleIdentifier moduleIdentifier,
            JmxAttribute jmxAttributeForReporting);

    Map<ModuleIdentifier, Module> getAllModules();

    void put(ModuleInternalTransactionalInfo moduleInternalTransactionalInfo);

    ModuleInternalTransactionalInfo destroyModule(
            ModuleIdentifier moduleIdentifier);

    void assertNotExists(ModuleIdentifier moduleIdentifier)
            throws InstanceAlreadyExistsException;

}
