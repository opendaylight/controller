/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.confignetconfconnector.osgi;

import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.ModuleIdentifier;

public interface YangStoreContext {

    /**
     * @deprecated Use {@link #getQNamesToIdentitiesToModuleMXBeanEntries()} instead. This method return only one
     * module representation even if multiple revisions are available.
     */
    @Deprecated
    Map<String/* Namespace from yang file */,
            Map<String /* Name of module entry from yang file */, ModuleMXBeanEntry>> getModuleMXBeanEntryMap();


    Map<QName, Map<String /* identity local name */, ModuleMXBeanEntry>> getQNamesToIdentitiesToModuleMXBeanEntries();

    /**
     * Get all modules discovered when this snapshot was created.
     * @return all modules discovered. If one module exists with two different revisions, return both.
     */
    Set<Module> getModules();

    String getModuleSource(ModuleIdentifier moduleIdentifier);

}
